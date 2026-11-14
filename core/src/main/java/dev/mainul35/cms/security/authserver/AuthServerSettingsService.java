package dev.mainul35.cms.security.authserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The VSD Auth Server settings in use right now. An administrator can change them in the admin console and they
 * apply to the next sign-in, with no restart. Until someone does, the application configuration
 * ({@code app.auth-server.*} and {@code spring.security.oauth2.client.*.vsd-auth.*}) is used.
 *
 * <p>The auth server does not have to publish an OpenID discovery document: the endpoints are used as given, or
 * derived from the issuer address with the usual paths ({@code /oauth2/authorize}, {@code /oauth2/token},
 * {@code /oauth2/jwks}, {@code /userinfo}).
 */
@Service
@Slf4j
public class AuthServerSettingsService {

    private static final Set<String> LOOPBACK = Set.of("localhost", "127.0.0.1", "[::1]", "::1");
    private static final long CACHE_MS = 15_000;
    private static final int MAX_BODY = 256 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** The settings as they are used. {@code clientSecret} is plain text and must never leave the server. */
    public record Effective(boolean enabled, String issuerUri, String authorizationUri, String tokenUri,
                            String jwkSetUri, String userInfoUri, String clientId, String clientSecret,
                            String scopes, boolean fromDatabase) {

        /** Complete enough to start a sign-in. */
        public boolean usable() {
            return enabled && notBlank(clientId) && notBlank(clientSecret) && notBlank(authorizationUri)
                    && notBlank(tokenUri) && notBlank(jwkSetUri);
        }
    }

    /** What an administrator submits. A blank {@code clientSecret} keeps the stored one. */
    public record Update(Boolean enabled, String issuerUri, String authorizationUri, String tokenUri, String jwkSetUri,
                         String userInfoUri, String clientId, String clientSecret, String scopes) {
    }

    /** One line of a connection test. */
    public record Check(String name, boolean ok, String message) {
    }

    private final AuthServerSettingsRepository repository;
    private final ObjectMapper mapper;
    private final Effective environment;
    private final byte[] key;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final AtomicLong version = new AtomicLong();

    private volatile Effective cached;
    private volatile long cachedAt;

    public AuthServerSettingsService(
            AuthServerSettingsRepository repository,
            ObjectMapper mapper,
            @Value("${app.auth-server.enabled:false}") boolean enabled,
            @Value("${app.auth-server.issuer-uri:}") String issuer,
            @Value("${app.auth-server.jwk-set-uri:}") String jwk,
            @Value("${spring.security.oauth2.client.provider.vsd-auth.authorization-uri:}") String authorization,
            @Value("${spring.security.oauth2.client.provider.vsd-auth.token-uri:}") String token,
            @Value("${spring.security.oauth2.client.provider.vsd-auth.user-info-uri:}") String userInfo,
            @Value("${spring.security.oauth2.client.registration.vsd-auth.client-id:}") String clientId,
            @Value("${spring.security.oauth2.client.registration.vsd-auth.client-secret:}") String clientSecret,
            @Value("${app.security.settings-key:}") String settingsKey,
            @Value("${app.jwt.secret:}") String jwtSecret) {
        this.repository = repository;
        this.mapper = mapper;
        this.environment = withStandardEndpoints(new Effective(enabled, blankToNull(issuer), blankToNull(authorization),
                blankToNull(token), blankToNull(jwk), blankToNull(userInfo), blankToNull(clientId),
                blankToNull(clientSecret), "openid profile email", false));
        String material = notBlank(settingsKey) ? settingsKey : jwtSecret;
        this.key = sha256("vsd-auth-server-settings|" + material);
    }

    // ---- reading ----------------------------------------------------------------------------

    /** The settings in use. Cached for a few seconds and dropped whenever they are changed. */
    public Effective effective() {
        long now = System.currentTimeMillis();
        Effective c = cached;
        if (c != null && now - cachedAt < CACHE_MS) {
            return c;
        }
        Effective fresh = repository.findById(AuthServerSettings.SINGLETON_ID).map(this::fromRow).orElse(environment);
        cached = fresh;
        cachedAt = now;
        return fresh;
    }

    /** Changes whenever the settings change, so caches built from them can be dropped. */
    public long version() {
        return version.get();
    }

    private Effective fromRow(AuthServerSettings r) {
        return new Effective(r.isEnabled(), r.getIssuerUri(), r.getAuthorizationUri(), r.getTokenUri(), r.getJwkSetUri(),
                r.getUserInfoUri(), r.getClientId(), decrypt(r.getClientSecretEnc()),
                notBlank(r.getScopes()) ? r.getScopes() : "openid profile email", true);
    }

    // ---- changing ---------------------------------------------------------------------------

    /**
     * Validates and stores new settings.
     *
     * @throws IllegalArgumentException with a message for the administrator if they cannot be used
     */
    public Effective update(Update in, Long adminId) {
        Effective current = effective();
        boolean enabled = Boolean.TRUE.equals(in.enabled());
        Effective merged = withStandardEndpoints(new Effective(enabled, url("Issuer address", in.issuerUri()),
                url("Authorization endpoint", in.authorizationUri()), url("Token endpoint", in.tokenUri()),
                url("Signing keys (JWKS) address", in.jwkSetUri()), url("User info endpoint", in.userInfoUri()),
                text("Client ID", in.clientId(), 200),
                notBlank(in.clientSecret()) ? in.clientSecret().trim() : current.clientSecret(),
                notBlank(in.scopes()) ? text("Scopes", in.scopes(), 300) : "openid profile email", true));
        if (merged.clientSecret() != null && merged.clientSecret().length() > 500) {
            throw new IllegalArgumentException("The client secret is too long.");
        }
        if (enabled) {
            require(merged.clientId(), "Client ID");
            require(merged.clientSecret(), "Client secret");
            require(merged.authorizationUri(), "Authorization endpoint (or the issuer address, to fill it in)");
            require(merged.tokenUri(), "Token endpoint (or the issuer address, to fill it in)");
            require(merged.jwkSetUri(), "Signing keys (JWKS) address (or the issuer address, to fill it in)");
        }
        AuthServerSettings row = repository.findById(AuthServerSettings.SINGLETON_ID).orElseGet(AuthServerSettings::new);
        row.setEnabled(merged.enabled());
        row.setIssuerUri(merged.issuerUri());
        row.setAuthorizationUri(merged.authorizationUri());
        row.setTokenUri(merged.tokenUri());
        row.setJwkSetUri(merged.jwkSetUri());
        row.setUserInfoUri(merged.userInfoUri());
        row.setClientId(merged.clientId());
        row.setClientSecretEnc(merged.clientSecret() == null ? null : encrypt(merged.clientSecret()));
        row.setScopes(merged.scopes());
        row.setUpdatedBy(adminId);
        row.setUpdatedAt(Instant.now());
        repository.save(row);
        changed();
        log.info("VSD Auth Server settings saved by user {} (enabled={}, issuer={}, client={})", adminId, merged.enabled(),
                merged.issuerUri(), merged.clientId());
        return merged;
    }

    /** Forgets the stored settings so the application configuration applies again. */
    public Effective resetToEnvironment(Long adminId) {
        repository.deleteById(AuthServerSettings.SINGLETON_ID);
        changed();
        log.info("VSD Auth Server settings reset to the application configuration by user {}", adminId);
        return effective();
    }

    private void changed() {
        version.incrementAndGet();
        cached = null;
    }

    // ---- testing ----------------------------------------------------------------------------

    /** Looks at the endpoints without signing anybody in: are they there, and do the keys look right? */
    public List<Check> test(Update draft) {
        Effective e = withStandardEndpoints(new Effective(true, url("Issuer address", draft.issuerUri()),
                url("Authorization endpoint", draft.authorizationUri()), url("Token endpoint", draft.tokenUri()),
                url("Signing keys (JWKS) address", draft.jwkSetUri()), url("User info endpoint", draft.userInfoUri()),
                null, null, null, false));
        List<Check> out = new ArrayList<>();
        out.add(reach("Authorization endpoint", e.authorizationUri(), "GET"));
        out.add(reach("Token endpoint", e.tokenUri(), "POST"));
        out.add(keys(e.jwkSetUri()));
        if (notBlank(e.userInfoUri())) {
            out.add(reach("User info endpoint", e.userInfoUri(), "GET"));
        }
        return out;
    }

    private Check reach(String name, String url, String method) {
        if (!notBlank(url)) {
            return new Check(name, false, "Not set.");
        }
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5));
            HttpRequest req = method.equals("POST") ? b.POST(HttpRequest.BodyPublishers.noBody()).build() : b.GET().build();
            HttpResponse<Void> res = http.send(req, HttpResponse.BodyHandlers.discarding());
            // An answer shows the endpoint is there: 400/401 is normal without credentials. 404 means it is not,
            // and a server error means something is wrong on the auth server.
            if (res.statusCode() == 404) {
                return new Check(name, false, "Not found (404): check the address.");
            }
            return res.statusCode() < 500
                    ? new Check(name, true, "Answered (" + res.statusCode() + ").")
                    : new Check(name, false, "The server answered " + res.statusCode() + ".");
        } catch (IOException e) {
            return new Check(name, false, "Could not connect: " + shortMessage(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Check(name, false, "Interrupted.");
        }
    }

    private Check keys(String url) {
        String name = "Signing keys (JWKS)";
        if (!notBlank(url)) {
            return new Check(name, false, "Not set.");
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<InputStream> res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = res.body()) {
                byte[] body = in.readNBytes(MAX_BODY + 1);
                if (res.statusCode() != 200) {
                    return new Check(name, false, "The server answered " + res.statusCode() + ".");
                }
                if (body.length > MAX_BODY) {
                    return new Check(name, false, "The answer is too large to be a key set.");
                }
                JsonNode json = mapper.readTree(new String(body, StandardCharsets.UTF_8));
                int count = json.path("keys").isArray() ? json.path("keys").size() : 0;
                return count > 0 ? new Check(name, true, count + " key(s) found.")
                        : new Check(name, false, "The address answered, but it holds no keys.");
            }
        } catch (IOException e) {
            return new Check(name, false, "Could not read the keys: " + shortMessage(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Check(name, false, "Interrupted.");
        }
    }

    // ---- helpers ----------------------------------------------------------------------------

    /** Fills endpoints that were left out from the issuer address, using the usual paths. */
    static Effective withStandardEndpoints(Effective e) {
        String issuer = e.issuerUri() == null ? null : e.issuerUri().replaceAll("/+$", "");
        if (issuer == null) {
            return e;
        }
        return new Effective(e.enabled(), e.issuerUri(),
                orElse(e.authorizationUri(), issuer + "/oauth2/authorize"),
                orElse(e.tokenUri(), issuer + "/oauth2/token"),
                orElse(e.jwkSetUri(), issuer + "/oauth2/jwks"),
                orElse(e.userInfoUri(), issuer + "/userinfo"),
                e.clientId(), e.clientSecret(), e.scopes(), e.fromDatabase());
    }

    /** Only https, except for this machine (local development). */
    static String url(String what, String value) {
        if (!notBlank(value)) {
            return null;
        }
        String v = value.trim();
        if (v.length() > 500) {
            throw new IllegalArgumentException(what + " is too long.");
        }
        URI u;
        try {
            u = URI.create(v);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(what + " is not a valid address.");
        }
        String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.ROOT);
        if (u.getHost() == null || !(scheme.equals("https") || scheme.equals("http"))) {
            throw new IllegalArgumentException(what + " must be a full http(s) address.");
        }
        if (u.getUserInfo() != null) {
            throw new IllegalArgumentException(what + " must not contain a user name or password.");
        }
        if (scheme.equals("http") && !LOOPBACK.contains(u.getHost().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(what + " must use https (plain http is only allowed for localhost).");
        }
        return v;
    }

    private static String text(String what, String value, int max) {
        if (!notBlank(value)) {
            return null;
        }
        String v = value.trim();
        if (v.length() > max || v.chars().anyMatch(c -> c < 0x20)) {
            throw new IllegalArgumentException(what + " is not valid.");
        }
        return v;
    }

    private static void require(String value, String what) {
        if (!notBlank(value)) {
            throw new IllegalArgumentException(what + " is required to turn sign-in on.");
        }
    }

    private static String orElse(String value, String fallback) {
        return notBlank(value) ? value : fallback;
    }

    private static String shortMessage(Exception e) {
        String m = e.getMessage();
        return m == null || m.isBlank() ? e.getClass().getSimpleName() : (m.length() > 120 ? m.substring(0, 120) : m);
    }

    static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    static String blankToNull(String s) {
        return notBlank(s) ? s.trim() : null;
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- the client secret at rest ----------------------------------------------------------

    String encrypt(String plain) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] all = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, all, 0, iv.length);
            System.arraycopy(ct, 0, all, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(all);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not protect the client secret", e);
        }
    }

    /** Null if there is none, or if it cannot be read (the key changed): the administrator enters it again. */
    String decrypt(String stored) {
        if (!notBlank(stored)) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, all, 0, 12));
            return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("The stored VSD Auth Server client secret cannot be read (was the application secret changed?). "
                    + "Enter it again in the admin console.");
            return null;
        }
    }

    /** For tests and the admin view: is a row stored? */
    public Optional<AuthServerSettings> stored() {
        return repository.findById(AuthServerSettings.SINGLETON_ID);
    }
}
