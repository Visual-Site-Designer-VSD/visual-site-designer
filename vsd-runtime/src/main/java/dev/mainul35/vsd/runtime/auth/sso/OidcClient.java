package dev.mainul35.vsd.runtime.auth.sso;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import dev.mainul35.vsd.runtime.util.Json;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Talks to the VSD auth server: builds the authorization URL, swaps the code for tokens and
 * verifies the token. Only RS256 is accepted, keys come from the server's JWKS, and issuer,
 * audience, expiry and nonce are all checked. Tokens never leave this class's callers on the server.
 */
@Component
public class OidcClient {

    private static final int MAX_RESPONSE_BYTES = 256 * 1024;
    private static final long JWKS_MAX_AGE_MS = 10 * 60_000L;
    private static final long JWKS_MIN_REFETCH_MS = 30_000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** The token to trust and whether it is an ID token (which carries the nonce). */
    public record Token(String value, boolean idToken) {
    }

    private final RuntimeProperties.Sso cfg;
    private final HttpClient http;
    private JWKSet jwks;
    private long jwksFetchedAt;

    public OidcClient(RuntimeProperties props) {
        this.cfg = props.getSso();
        this.http = HttpClient.newBuilder()
                .connectTimeout(cfg.getTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        if (cfg.isEnabled()) {
            validateConfig();
        }
    }

    public boolean enabled() {
        return cfg.isEnabled();
    }

    public boolean passwordLogin() {
        return !cfg.isEnabled() || cfg.isPasswordLogin();
    }

    public RuntimeProperties.Sso config() {
        return cfg;
    }

    // ---- authorization ----------------------------------------------------------------------

    public static String randomToken() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static String challenge(String verifier) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String authorizeUrl(String state, String nonce, String verifier) {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("response_type", "code");
        q.put("client_id", cfg.getClientId());
        q.put("redirect_uri", cfg.getRedirectUri());
        q.put("scope", cfg.getScopes());
        q.put("state", state);
        q.put("nonce", nonce);
        q.put("code_challenge", challenge(verifier));
        q.put("code_challenge_method", "S256");
        String base = cfg.getAuthorizationEndpoint();
        return base + (base.contains("?") ? "&" : "?") + form(q);
    }

    /** Where to send the visitor after sign-out so the auth server session ends too, or null. */
    public String endSessionUrl() {
        String base = cfg.getEndSessionEndpoint();
        if (!cfg.isEnabled() || base == null || base.isBlank()) {
            return null;
        }
        URI redirect = URI.create(cfg.getRedirectUri());
        String home = redirect.getScheme() + "://" + redirect.getRawAuthority() + "/";
        Map<String, String> q = new LinkedHashMap<>();
        q.put("client_id", cfg.getClientId());
        q.put("post_logout_redirect_uri", home);
        return base + (base.contains("?") ? "&" : "?") + form(q);
    }

    // ---- token exchange ---------------------------------------------------------------------

    public Token exchange(String code, String verifier) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("redirect_uri", cfg.getRedirectUri());
        body.put("client_id", cfg.getClientId());
        body.put("code_verifier", verifier);

        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(cfg.getTokenEndpoint()))
                .timeout(cfg.getTimeout())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json");
        if (cfg.getClientSecret() != null && !cfg.getClientSecret().isBlank()) {
            String basic = enc(cfg.getClientId()) + ":" + enc(cfg.getClientSecret());
            req.header("Authorization", "Basic "
                    + Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8)));
        }
        String text = send(req.POST(HttpRequest.BodyPublishers.ofString(form(body))).build(), "token endpoint");
        JsonNode json;
        try {
            json = Json.MAPPER.readTree(text);
        } catch (IOException e) {
            throw new SsoException("Token endpoint returned invalid JSON", e);
        }
        String id = json.path("id_token").asText("");
        if (!id.isBlank()) {
            return new Token(id, true);
        }
        String access = json.path("access_token").asText("");
        if (!access.isBlank()) {
            return new Token(access, false);
        }
        throw new SsoException("Token endpoint returned no token");
    }

    // ---- verification -----------------------------------------------------------------------

    public JWTClaimsSet verify(Token token, String expectedNonce) {
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(token.value());
        } catch (ParseException e) {
            throw new SsoException("Token is not a signed JWT", e);
        }
        if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())) {
            throw new SsoException("Unsupported token algorithm: " + jwt.getHeader().getAlgorithm());
        }
        String kid = jwt.getHeader().getKeyID();
        if (kid == null) {
            throw new SsoException("Token has no key id");
        }
        RSAKey key = findKey(kid);
        try {
            if (!jwt.verify(new RSASSAVerifier(key))) {
                throw new SsoException("Token signature is invalid");
            }
            JWTClaimsSet c = jwt.getJWTClaimsSet();
            check(c, token.idToken(), expectedNonce);
            return c;
        } catch (com.nimbusds.jose.JOSEException | ParseException e) {
            throw new SsoException("Token could not be verified", e);
        }
    }

    private void check(JWTClaimsSet c, boolean idToken, String expectedNonce) throws ParseException {
        if (!cfg.getIssuer().equals(c.getIssuer())) {
            throw new SsoException("Wrong issuer: " + c.getIssuer());
        }
        String audience = cfg.getAudience() == null || cfg.getAudience().isBlank() ? cfg.getClientId() : cfg.getAudience();
        List<String> aud = c.getAudience();
        if (aud == null || !aud.contains(audience)) {
            throw new SsoException("Token is not for this client");
        }
        long skew = cfg.getClockSkew().toMillis();
        long now = System.currentTimeMillis();
        Date exp = c.getExpirationTime();
        if (exp == null || exp.getTime() + skew < now) {
            throw new SsoException("Token expired");
        }
        Date nbf = c.getNotBeforeTime();
        if (nbf != null && nbf.getTime() - skew > now) {
            throw new SsoException("Token not valid yet");
        }
        Date iat = c.getIssueTime();
        if (iat != null && iat.getTime() - skew > now) {
            throw new SsoException("Token issued in the future");
        }
        if (c.getSubject() == null || c.getSubject().isBlank()) {
            throw new SsoException("Token has no subject");
        }
        if (idToken) {
            String nonce = c.getStringClaim("nonce");
            if (nonce == null || !MessageDigest.isEqual(nonce.getBytes(StandardCharsets.UTF_8),
                    expectedNonce.getBytes(StandardCharsets.UTF_8))) {
                throw new SsoException("Nonce mismatch");
            }
        }
    }

    private synchronized RSAKey findKey(String kid) {
        long now = System.currentTimeMillis();
        boolean stale = jwks == null || now - jwksFetchedAt > JWKS_MAX_AGE_MS;
        if (stale) {
            loadJwks(now);
        }
        JWK k = jwks.getKeyByKeyId(kid);
        if (k == null && now - jwksFetchedAt > JWKS_MIN_REFETCH_MS) {
            loadJwks(now); // the server may have rotated its keys
            k = jwks.getKeyByKeyId(kid);
        }
        if (!(k instanceof RSAKey rsa)) {
            throw new SsoException("No RSA key for kid " + kid);
        }
        return rsa;
    }

    private void loadJwks(long now) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(cfg.getJwksUri()))
                .timeout(cfg.getTimeout()).header("Accept", "application/json").GET().build();
        try {
            jwks = JWKSet.parse(send(req, "JWKS endpoint"));
            jwksFetchedAt = now;
        } catch (ParseException e) {
            throw new SsoException("JWKS endpoint returned invalid keys", e);
        }
    }

    // ---- plumbing ---------------------------------------------------------------------------

    private String send(HttpRequest request, String what) {
        try {
            HttpResponse<InputStream> res = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = res.body()) {
                byte[] bytes = in.readNBytes(MAX_RESPONSE_BYTES + 1);
                if (res.statusCode() != 200) {
                    throw new SsoException(what + " answered " + res.statusCode());
                }
                if (bytes.length > MAX_RESPONSE_BYTES) {
                    throw new SsoException(what + " response is too large");
                }
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new SsoException("Could not reach the " + what, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SsoException("Interrupted while calling the " + what, e);
        }
    }

    private static String form(Map<String, String> values) {
        StringBuilder sb = new StringBuilder();
        values.forEach((k, v) -> {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(enc(k)).append('=').append(enc(v));
        });
        return sb.toString();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private void validateConfig() {
        require("issuer", cfg.getIssuer());
        require("authorization-endpoint", cfg.getAuthorizationEndpoint());
        require("token-endpoint", cfg.getTokenEndpoint());
        require("jwks-uri", cfg.getJwksUri());
        require("client-id", cfg.getClientId());
        require("redirect-uri", cfg.getRedirectUri());
        for (String url : new String[]{cfg.getAuthorizationEndpoint(), cfg.getTokenEndpoint(), cfg.getJwksUri(),
                cfg.getRedirectUri(), cfg.getEndSessionEndpoint()}) {
            if (url == null || url.isBlank()) {
                continue;
            }
            URI u;
            try {
                u = URI.create(url);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("vsd.runtime.sso: invalid URL " + url);
            }
            boolean https = "https".equalsIgnoreCase(u.getScheme());
            boolean http = "http".equalsIgnoreCase(u.getScheme());
            if (u.getHost() == null || !(https || (http && cfg.isAllowInsecureHttp()))) {
                throw new IllegalStateException("vsd.runtime.sso: " + url
                        + " must be an https URL (set allow-insecure-http=true for local development only)");
            }
        }
        if (!cfg.getRedirectUri().endsWith("/_vsd/auth/callback")) {
            throw new IllegalStateException("vsd.runtime.sso.redirect-uri must end with /_vsd/auth/callback");
        }
    }

    private static void require(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("vsd.runtime.sso." + name + " is required when SSO is enabled");
        }
    }
}
