package dev.mainul35.cms.security.authserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jwt.JwtException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServerSettingsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AtomicReference<AuthServerSettings> row = new AtomicReference<>();
    private AuthServerSettingsRepository repository;
    private HttpServer authServer;
    private volatile String jwksBody = "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"k1\"}]}";

    @BeforeEach
    void setUp() throws Exception {
        repository = mock(AuthServerSettingsRepository.class);
        when(repository.findById(anyLong())).thenAnswer(i -> Optional.ofNullable(row.get()));
        when(repository.save(any(AuthServerSettings.class))).thenAnswer(i -> {
            row.set(i.getArgument(0));
            return i.getArgument(0);
        });
        doAnswer(i -> {
            row.set(null);
            return null;
        }).when(repository).deleteById(anyLong());

        authServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        authServer.createContext("/oauth2/authorize", ex -> {
            ex.getResponseHeaders().add("Location", "/login");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        authServer.createContext("/oauth2/token", ex -> {
            ex.sendResponseHeaders(400, -1);
            ex.close();
        });
        authServer.createContext("/oauth2/jwks", ex -> {
            byte[] b = jwksBody.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        authServer.start();
    }

    @AfterEach
    void tearDown() {
        authServer.stop(0);
    }

    private String base() {
        return "http://127.0.0.1:" + authServer.getAddress().getPort();
    }

    private AuthServerSettingsService service(boolean envEnabled, String envIssuer, String jwtSecret) {
        return new AuthServerSettingsService(repository, MAPPER, envEnabled, envIssuer, "", "", "", "",
                envEnabled ? "env-client" : "", envEnabled ? "env-secret" : "", "", jwtSecret);
    }

    private static AuthServerSettingsService.Update update(boolean enabled, String issuer, String clientId, String secret) {
        return new AuthServerSettingsService.Update(enabled, issuer, "", "", "", "", clientId, secret, "");
    }

    // ---- settings ---------------------------------------------------------------------------

    @Test
    void theApplicationConfigurationAppliesUntilSomeoneSavesSettings() {
        AuthServerSettingsService s = service(true, "http://localhost:9001", "secret-a");
        var e = s.effective();
        assertFalse(e.fromDatabase());
        assertTrue(e.usable());
        assertEquals("http://localhost:9001/oauth2/authorize", e.authorizationUri(), "endpoints come from the issuer");
        assertEquals("http://localhost:9001/oauth2/jwks", e.jwkSetUri());
        assertEquals("env-client", e.clientId());

        assertFalse(service(false, "", "secret-a").effective().usable(), "off by default without configuration");
    }

    @Test
    void savingFillsInEndpointsKeepsTheSecretEncryptedAndKeepsItWhenLeftBlank() {
        AuthServerSettingsService s = service(false, "", "secret-a");
        long before = s.version();
        var saved = s.update(update(true, "https://auth.example.com/", "site", "s3cret-value"), 5L);

        assertTrue(saved.usable());
        assertEquals("https://auth.example.com/oauth2/token", saved.tokenUri());
        assertTrue(s.version() > before, "caches built from the old settings are dropped");

        AuthServerSettings stored = row.get();
        assertNotNull(stored.getClientSecretEnc());
        assertFalse(stored.getClientSecretEnc().contains("s3cret-value"), "never stored as plain text");
        assertEquals(5L, stored.getUpdatedBy());
        assertEquals("s3cret-value", s.effective().clientSecret());

        // a later save without a secret keeps the saved one
        s.update(update(true, "https://auth.example.com", "site-2", ""), 5L);
        assertEquals("s3cret-value", s.effective().clientSecret());
        assertEquals("site-2", s.effective().clientId());
    }

    @Test
    void refusesSettingsThatCannotWorkOrAreUnsafe() {
        AuthServerSettingsService s = service(false, "", "secret-a");
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> s.update(update(true, "https://auth.example.com", "site", ""), 1L)).getMessage().contains("secret"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> s.update(update(true, "", "site", "x"), 1L)).getMessage().toLowerCase().contains("authorization"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> s.update(update(true, "http://auth.example.com", "site", "x"), 1L)).getMessage().contains("https"));
        for (String bad : new String[]{"javascript:alert(1)", "https://user:pw@auth.example.com", "ftp://auth.example.com", "not a url"}) {
            assertThrows(IllegalArgumentException.class, () -> s.update(update(false, bad, "site", "x"), 1L), bad);
        }
        assertNull(row.get(), "nothing is saved when it is refused");
        // plain http is fine for this machine
        assertTrue(s.update(update(true, "http://localhost:9001", "site", "x"), 1L).usable());
    }

    @Test
    void turningItOffAndResettingWorkWithoutLosingTheRest() {
        AuthServerSettingsService s = service(true, "http://localhost:9001", "secret-a");
        s.update(update(true, "https://auth.example.com", "site", "x"), 1L);
        var off = s.update(update(false, "https://auth.example.com", "site", ""), 1L);
        assertFalse(off.usable());
        assertEquals("site", s.effective().clientId(), "the rest of the settings are kept");

        var back = s.resetToEnvironment(1L);
        assertFalse(back.fromDatabase());
        assertEquals("env-client", back.clientId());
    }

    @Test
    void aSecretThatCannotBeReadAnyMoreIsTreatedAsNotSet() {
        service(false, "", "secret-a").update(update(true, "https://auth.example.com", "site", "x"), 1L);
        // the application secret the key comes from was changed
        var other = service(false, "", "secret-b").effective();
        assertTrue(other.fromDatabase());
        assertNull(other.clientSecret());
        assertFalse(other.usable(), "an administrator has to enter it again");
    }

    // ---- connection test --------------------------------------------------------------------

    @Test
    void theConnectionTestSaysWhatIsReachableAndWhatIsNot() {
        AuthServerSettingsService s = service(false, "", "secret-a");
        var all = s.test(new AuthServerSettingsService.Update(true, base(), "", "", "", "", "", "", ""));
        assertEquals(4, all.size(), "authorization, token, keys and (derived from the issuer) user info");
        assertTrue(all.subList(0, 3).stream().allMatch(AuthServerSettingsService.Check::ok), all.toString());
        assertFalse(all.get(3).ok(), "this stand-in has no user info endpoint");
        assertTrue(all.get(3).message().contains("404"), all.get(3).message());

        jwksBody = "{\"keys\":[]}";
        var noKeys = s.test(new AuthServerSettingsService.Update(true, base(), "", "", "", "", "", "", ""));
        assertFalse(noKeys.get(2).ok());
        assertTrue(noKeys.get(2).message().contains("no keys"));

        var down = s.test(new AuthServerSettingsService.Update(true, "http://127.0.0.1:1", "", "", "", "", "", "", ""));
        assertTrue(down.stream().noneMatch(AuthServerSettingsService.Check::ok));
        assertTrue(down.get(0).message().contains("connect"));
    }

    // ---- used at sign-in --------------------------------------------------------------------

    @Test
    void theRegistrationFollowsTheSettingsWithoutARestartAndNeverCallsTheAuthServer() {
        AuthServerSettingsService s = service(false, "", "secret-a");
        var repo = new DynamicClientRegistrationRepository(s, "{baseUrl}/login/oauth2/code/vsd-auth", "sub");
        assertNull(repo.findByRegistrationId("vsd-auth"), "off: Spring answers unknown client");
        assertNull(repo.findByRegistrationId("other"));

        // an address nothing listens on: building the registration must not need it
        s.update(update(true, "https://unreachable.invalid", "client-1", "secret-1"), 1L);
        ClientRegistration r = repo.findByRegistrationId("vsd-auth");
        assertEquals("client-1", r.getClientId());
        assertEquals("https://unreachable.invalid/oauth2/authorize", r.getProviderDetails().getAuthorizationUri());
        assertEquals("https://unreachable.invalid", r.getProviderDetails().getIssuerUri());
        assertTrue(r.getScopes().contains("openid"));

        s.update(update(true, "https://unreachable.invalid", "client-2", ""), 1L);
        assertEquals("client-2", repo.findByRegistrationId("vsd-auth").getClientId());

        s.update(update(false, "https://unreachable.invalid", "client-2", ""), 1L);
        assertNull(repo.findByRegistrationId("vsd-auth"));
    }

    @Test
    void tokensFromTheAuthServerAreRefusedWhileItIsOffAndNothingIsFetchedAtStartup() {
        AuthServerSettingsService s = service(false, "", "secret-a");
        DynamicJwtDecoder decoder = new DynamicJwtDecoder(s);
        JwtException off = assertThrows(JwtException.class, () -> decoder.decode("a.b.c"));
        assertTrue(off.getMessage().contains("turned off"), off.getMessage());

        s.update(update(true, "https://unreachable.invalid", "c", "s"), 1L);
        // on: the token is now judged (and refused, as it is not a real one), but the auth server is not needed to try
        assertThrows(JwtException.class, () -> decoder.decode("not-a-jwt"));
        assertNotEquals("Sign-in through the VSD Auth Server is turned off",
                assertThrows(JwtException.class, () -> decoder.decode("not-a-jwt")).getMessage());
    }
}
