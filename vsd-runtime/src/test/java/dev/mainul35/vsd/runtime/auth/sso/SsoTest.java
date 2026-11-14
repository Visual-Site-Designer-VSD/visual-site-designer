package dev.mainul35.vsd.runtime.auth.sso;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import dev.mainul35.cms.sdk.runtime.RuntimeUser;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import dev.mainul35.vsd.runtime.auth.AuthService;
import dev.mainul35.vsd.runtime.session.InMemorySessionStore;
import dev.mainul35.vsd.runtime.session.Session;
import dev.mainul35.vsd.runtime.session.SessionService;
import dev.mainul35.vsd.runtime.shared.SharedStateService;
import dev.mainul35.vsd.runtime.shared.SharedTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sign-in through the auth server, against a fake issuer that serves real RS256 tokens. */
class SsoTest {

    private static final String ISSUER = "https://auth.example";
    private static final String CLIENT = "site-client";

    private HttpServer issuer;
    private RSAKey key;
    private RSAKey otherKey;
    private final AtomicReference<String> idToken = new AtomicReference<>();
    private final AtomicReference<Map<String, String>> tokenForm = new AtomicReference<>();
    private final AtomicReference<String> tokenAuth = new AtomicReference<>();
    private RuntimeProperties props;
    private OidcClient oidc;
    private SsoService sso;
    private SessionService sessions;
    private InMemorySessionStore store;
    private SharedStateService shared;

    @BeforeEach
    void setUp() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("k1").generate();
        otherKey = new RSAKeyGenerator(2048).keyID("k1").generate(); // same kid, different key
        issuer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        issuer.createContext("/jwks", ex -> {
            byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        issuer.createContext("/token", ex -> {
            tokenAuth.set(ex.getRequestHeaders().getFirst("Authorization"));
            tokenForm.set(parseForm(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            String t = idToken.get();
            byte[] body = (t == null ? "{}" : "{\"id_token\":\"" + t + "\"}").getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        issuer.start();
        String base = "http://127.0.0.1:" + issuer.getAddress().getPort();

        props = new RuntimeProperties();
        RuntimeProperties.Sso s = props.getSso();
        s.setEnabled(true);
        s.setIssuer(ISSUER);
        s.setAuthorizationEndpoint(ISSUER + "/oauth2/authorize");
        s.setTokenEndpoint(base + "/token");
        s.setJwksUri(base + "/jwks");
        s.setEndSessionEndpoint(ISSUER + "/logout");
        s.setClientId(CLIENT);
        s.setClientSecret("s3cret");
        s.setRedirectUri("https://site.example/_vsd/auth/callback");
        s.setAllowInsecureHttp(true);
        build();
    }

    private void build() {
        oidc = new OidcClient(props);
        store = new InMemorySessionStore();
        shared = SharedTestSupport.service(props);
        sessions = new SessionService(store, props, shared);
        AuthService auth = new AuthService((u, p) -> Optional.empty(), sessions, oidc, shared);
        sso = new SsoService(oidc, auth);
    }

    @AfterEach
    void tearDown() {
        issuer.stop(0);
    }

    // ---- helpers ----------------------------------------------------------------------------

    private JWTClaimsSet.Builder claims(String nonce) {
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER).subject("ada@example.com").audience(CLIENT)
                .issueTime(new Date()).expirationTime(new Date(System.currentTimeMillis() + 300_000))
                .claim("nonce", nonce)
                .claim("user_id", 42)
                .claim("display_name", "Ada Lovelace")
                .claim("email", "ada@example.com")
                .claim("roles", List.of("ROLE_EDITOR", "ROLE_VIEWER"))
                .claim("avatar_url", "https://img.example/ada.png");
    }

    private static String sign(RSAKey signer, JWTClaimsSet c) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signer.getKeyID()).build(), c);
        jwt.sign(new RSASSASigner(signer));
        return jwt.serialize();
    }

    private static Map<String, String> parseForm(String raw) {
        Map<String, String> m = new HashMap<>();
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            if (i > 0) {
                m.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
            }
        }
        return m;
    }

    private record Started(Session session, Map<String, String> query, SsoService.Outcome outcome) {
    }

    private Started start(String returnTo) {
        Session s = store.create();
        SsoService.Outcome o = sso.start(s, returnTo, "1.2.3.4");
        assertTrue(o.ok(), o.message());
        return new Started(s, parseForm(URI.create(o.redirectTo()).getRawQuery()), o);
    }

    private SsoService.Outcome callback(Session s, String code, String state) {
        return sso.callback(s, code, state, null, "1.2.3.4", new MockHttpServletRequest(), new MockHttpServletResponse());
    }

    // ---- the whole flow ---------------------------------------------------------------------

    @Test
    void signsTheVisitorInAndReturnsThemToTheirPage() throws Exception {
        Started st = start("/about?tab=2");
        assertTrue(st.outcome.redirectTo().startsWith(ISSUER + "/oauth2/authorize?"));
        assertEquals("code", st.query.get("response_type"));
        assertEquals("S256", st.query.get("code_challenge_method"));
        assertEquals(CLIENT, st.query.get("client_id"));

        idToken.set(sign(key, claims(st.query.get("nonce")).build()));
        String oldId = st.session.id();
        SsoService.Outcome done = callback(st.session, "the-code", st.query.get("state"));

        assertTrue(done.ok(), done.message());
        assertEquals("/about?tab=2", done.redirectTo());
        // PKCE: the verifier the runtime sent hashes to the challenge it announced
        Map<String, String> form = tokenForm.get();
        assertEquals("the-code", form.get("code"));
        assertEquals(st.query.get("code_challenge"), OidcClient.challenge(form.get("code_verifier")));
        assertEquals("https://site.example/_vsd/auth/callback", form.get("redirect_uri"));
        assertTrue(tokenAuth.get().startsWith("Basic "));

        // identity is in session state, the session id was rotated
        assertNotEquals(oldId, st.session.id());
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) st.session.get(AuthService.USER_KEY).orElseThrow();
        assertEquals("42", user.get("id"));
        assertEquals("Ada Lovelace", user.get("displayName"));
        assertEquals(List.of("EDITOR", "VIEWER"), user.get("roles"));
        assertEquals("sso", user.get("provider"));
        assertEquals("https://img.example/ada.png", user.get("avatarUrl"));
        assertFalse(user.containsKey("password"));
    }

    @Test
    void aStateCanBeUsedOnlyOnce() throws Exception {
        Started st = start("/");
        idToken.set(sign(key, claims(st.query.get("nonce")).build()));
        assertTrue(callback(st.session, "c", st.query.get("state")).ok());
        assertFalse(callback(st.session, "c", st.query.get("state")).ok());
    }

    @Test
    void aCallbackFromAnotherBrowserIsRefusedAndBurnsTheState() throws Exception {
        Started st = start("/");
        idToken.set(sign(key, claims(st.query.get("nonce")).build()));
        Session attacker = store.create();
        SsoService.Outcome o = callback(attacker, "c", st.query.get("state"));
        assertFalse(o.ok());
        assertNull(attacker.get(AuthService.USER_KEY).orElse(null));
        assertFalse(callback(st.session, "c", st.query.get("state")).ok(), "state was consumed");
        assertFalse(callback(null, "c", "unknown").ok());
    }

    @Test
    void anErrorFromTheAuthServerSignsNobodyIn() {
        Started st = start("/");
        SsoService.Outcome o = sso.callback(st.session, null, st.query.get("state"), "access_denied", "1.2.3.4",
                new MockHttpServletRequest(), new MockHttpServletResponse());
        assertEquals(403, o.status());
        assertNull(st.session.get(AuthService.USER_KEY).orElse(null));
    }

    @Test
    void requiredRolesKeepOtherAccountsOut() throws Exception {
        props.getSso().setRequiredRoles(List.of("ROLE_ADMIN"));
        build();
        Started st = start("/");
        idToken.set(sign(key, claims(st.query.get("nonce")).build()));
        SsoService.Outcome o = callback(st.session, "c", st.query.get("state"));
        assertEquals(403, o.status());
        assertNull(st.session.get(AuthService.USER_KEY).orElse(null));

        Started again = start("/");
        idToken.set(sign(key, claims(again.query.get("nonce")).claim("roles", "ROLE_ADMIN").build()));
        assertTrue(callback(again.session, "c", again.query.get("state")).ok());
    }

    // ---- token checks -----------------------------------------------------------------------

    private void assertRejected(String token, String nonce) {
        assertThrows(SsoException.class, () -> oidc.verify(new OidcClient.Token(token, true), nonce));
    }

    @Test
    void acceptsAGoodTokenAndRejectsEveryKindOfBadOne() throws Exception {
        assertNotNull(oidc.verify(new OidcClient.Token(sign(key, claims("n1").build()), true), "n1"));

        assertRejected(sign(key, claims("n1").issuer("https://evil.example").build()), "n1");
        assertRejected(sign(key, claims("n1").audience("another-client").build()), "n1");
        assertRejected(sign(key, claims("n1").expirationTime(new Date(System.currentTimeMillis() - 600_000)).build()), "n1");
        assertRejected(sign(key, claims("n1").notBeforeTime(new Date(System.currentTimeMillis() + 600_000)).build()), "n1");
        assertRejected(sign(key, claims("other-nonce").build()), "n1");
        assertRejected(sign(key, claims(null).build()), "n1");
        assertRejected(sign(otherKey, claims("n1").build()), "n1"); // right kid, wrong key: bad signature
        assertRejected("not.a.jwt", "n1");

        // an HMAC token claiming the same kid must not be accepted (algorithm confusion)
        SignedJWT hs = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).keyID("k1").build(), claims("n1").build());
        hs.sign(new MACSigner(new byte[32]));
        assertRejected(hs.serialize(), "n1");

        // an unsigned token must not be accepted either
        String none = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"alg\":\"none\",\"kid\":\"k1\"}".getBytes(StandardCharsets.UTF_8))
                + "." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                claims("n1").build().toString().getBytes(StandardCharsets.UTF_8)) + ".";
        assertRejected(none, "n1");
    }

    @Test
    void anAccessTokenFallbackNeedsNoNonceButStillNeedsEverythingElse() throws Exception {
        String ok = sign(key, claims(null).build());
        assertNotNull(oidc.verify(new OidcClient.Token(ok, false), "ignored"));
        String wrongIssuer = sign(key, claims(null).issuer("https://evil.example").build());
        assertThrows(SsoException.class, () -> oidc.verify(new OidcClient.Token(wrongIssuer, false), "ignored"));
    }

    // ---- claims, URLs, config ---------------------------------------------------------------

    @Test
    void mapsClaimsLikeTheCmsDoes() {
        RuntimeUser u = SsoService.toUser(new JWTClaimsSet.Builder().subject("bob@example.com")
                .claim("roles", "ROLE_CMS_ADMIN, viewer").build());
        assertEquals("bob@example.com", u.username());
        assertEquals("bob@example.com", u.email());
        assertEquals("bob", u.displayName());
        assertEquals("bob@example.com", u.id());
        assertEquals(Set.of("CMS_ADMIN", "VIEWER"), u.roles());
        assertNull(u.avatarUrl());

        RuntimeUser bad = SsoService.toUser(new JWTClaimsSet.Builder().subject("x")
                .claim("avatar_url", "javascript:alert(1)").build());
        assertNull(bad.avatarUrl());
    }

    @Test
    void onlyPathsOnThisSiteAreValidReturnTargets() {
        for (String ok : new String[]{"/", "/about", "/a/b?x=1#top"}) {
            assertEquals(ok, SsoService.safeReturn(ok));
        }
        for (String bad : new String[]{null, "", "about", "//evil.example", "/\\evil.example", "https://evil.example",
                "javascript:alert(1)", "/_vsd/api/session", "/x\r\nSet-Cookie: a=b", "/" + "a".repeat(600)}) {
            assertEquals("/", SsoService.safeReturn(bad), String.valueOf(bad));
        }
    }

    @Test
    void endSessionUrlSendsVisitorsBackToTheSite() {
        String url = oidc.endSessionUrl();
        assertTrue(url.startsWith(ISSUER + "/logout?"));
        assertTrue(url.contains("post_logout_redirect_uri=https%3A%2F%2Fsite.example%2F"));
        props.getSso().setEndSessionEndpoint(null);
        build();
        assertNull(oidc.endSessionUrl());
    }

    @Test
    void passwordSignInCanBeSwitchedOff() {
        props.getSso().setPasswordLogin(false);
        build();
        assertFalse(oidc.passwordLogin());
        AuthService auth = new AuthService((u, p) -> Optional.of(new RuntimeUser("1", "u", "U", null, Set.of())), sessions, oidc, shared);
        Session s = store.create();
        AuthService.Result r = auth.login(s, "u", "p", "1.1.1.1", new MockHttpServletRequest(), new MockHttpServletResponse());
        assertEquals(403, r.status());
        assertNull(s.get(AuthService.USER_KEY).orElse(null));
    }

    @Test
    void startupFailsOnUnsafeOrIncompleteConfig() {
        props.getSso().setAllowInsecureHttp(false); // token and JWKS URLs in this test are plain http
        assertThrows(IllegalStateException.class, () -> new OidcClient(props));

        RuntimeProperties missing = new RuntimeProperties();
        missing.getSso().setEnabled(true);
        assertThrows(IllegalStateException.class, () -> new OidcClient(missing));

        props.getSso().setAllowInsecureHttp(true);
        props.getSso().setRedirectUri("https://site.example/somewhere-else");
        assertThrows(IllegalStateException.class, () -> new OidcClient(props));
    }

    @Test
    void logoutOfASsoUserPointsAtTheAuthServer() throws Exception {
        Started st = start("/");
        idToken.set(sign(key, claims(st.query.get("nonce")).build()));
        assertTrue(callback(st.session, "c", st.query.get("state")).ok());

        AuthService auth = new AuthService((u, p) -> Optional.empty(), sessions, oidc, shared);
        AuthService.Result out = auth.logout(st.session, new MockHttpServletRequest(), new MockHttpServletResponse());
        Map<?, ?> body = new com.fasterxml.jackson.databind.ObjectMapper().readValue(out.json(), LinkedHashMap.class);
        assertTrue(String.valueOf(body.get("redirect")).startsWith(ISSUER + "/logout"));

        // a password user gets no redirect
        Session plain = store.create();
        auth.signIn(plain, new RuntimeUser("1", "u", "U", null, Set.of()), AuthService.PROVIDER_PASSWORD,
                new MockHttpServletRequest(), new MockHttpServletResponse());
        AuthService.Result out2 = auth.logout(plain, new MockHttpServletRequest(), new MockHttpServletResponse());
        assertFalse(out2.json().contains("redirect"));
    }
}
