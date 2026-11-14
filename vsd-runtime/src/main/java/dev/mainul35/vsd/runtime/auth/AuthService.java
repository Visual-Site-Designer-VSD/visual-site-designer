package dev.mainul35.vsd.runtime.auth;

import dev.mainul35.cms.sdk.runtime.IdentityProvider;
import dev.mainul35.cms.sdk.runtime.RuntimeUser;
import dev.mainul35.cms.sdk.runtime.StateKey;
import dev.mainul35.cms.sdk.runtime.StatePatch;
import dev.mainul35.vsd.runtime.auth.sso.OidcClient;
import dev.mainul35.vsd.runtime.session.Session;
import dev.mainul35.vsd.runtime.session.SessionService;
import dev.mainul35.vsd.runtime.util.Json;
import dev.mainul35.vsd.runtime.util.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Attaches an identity to a session. The identity is also published as the reserved state
 * entry {@code SESSION/$auth/user}, which clients can read but never write, so every plugin
 * on the page sees sign-in and sign-out through the normal state stream.
 */
@Service
public class AuthService {

    public static final StateKey USER_KEY = StateKey.session("$auth", "user");
    public static final String PROVIDER_PASSWORD = "password";
    public static final String PROVIDER_SSO = "sso";

    private final IdentityProvider provider;
    private final SessionService sessions;
    private final OidcClient oidc;
    private final RateLimiter loginLimiter = new RateLimiter(5, 60_000);

    public AuthService(IdentityProvider provider, SessionService sessions, OidcClient oidc) {
        this.provider = provider;
        this.sessions = sessions;
        this.oidc = oidc;
    }

    public record Result(int status, String json) {
    }

    public Result login(Session session, String username, String password, String ip,
                        HttpServletRequest request, HttpServletResponse response) {
        if (!oidc.passwordLogin()) {
            return error(403, "Password sign-in is turned off. Use single sign-on.");
        }
        String limitKey = ip + "|" + (username == null ? "" : username.toLowerCase());
        if (!loginLimiter.tryAcquire(limitKey)) {
            return error(429, "Too many attempts. Try again later.");
        }
        Optional<RuntimeUser> user = provider.authenticate(username, password);
        if (user.isEmpty()) {
            return error(401, "Invalid credentials");
        }
        return signIn(session, user.get(), PROVIDER_PASSWORD, request, response);
    }

    /** Attaches an already verified identity to the session (new session id and CSRF token). */
    public Result signIn(Session session, RuntimeUser user, String via,
                         HttpServletRequest request, HttpServletResponse response) {
        session.lock().lock();
        try {
            sessions.rotate(session, request, response);
            session.setUser(user);
            List<StatePatch> applied = session.apply(List.of(StatePatch.set(USER_KEY, toMap(user, via))));
            return new Result(200, Json.write(body(session, applied, null)));
        } finally {
            session.lock().unlock();
        }
    }

    public Result logout(Session session, HttpServletRequest request, HttpServletResponse response) {
        session.lock().lock();
        try {
            // Visitors who came in through the auth server are sent there to end its session too,
            // otherwise the next click would sign them straight back in.
            boolean viaSso = session.get(USER_KEY)
                    .filter(v -> v instanceof Map<?, ?> m && PROVIDER_SSO.equals(m.get("provider"))).isPresent();
            session.setUser(null);
            List<StatePatch> applied = new ArrayList<>(session.clearAll());
            sessions.rotate(session, request, response);
            return new Result(200, Json.write(body(session, applied, viaSso ? oidc.endSessionUrl() : null)));
        } finally {
            session.lock().unlock();
        }
    }

    private static Map<String, Object> body(Session session, List<StatePatch> applied, String redirect) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "success");
        out.put("csrf", session.csrf());
        out.put("rev", session.rev());
        out.put("patches", applied.stream().map(Session::toWire).toList());
        if (redirect != null) {
            out.put("redirect", redirect);
        }
        return out;
    }

    static Map<String, Object> toMap(RuntimeUser u, String via) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.id());
        m.put("username", u.username());
        m.put("displayName", u.displayName());
        m.put("email", u.email());
        m.put("roles", u.roles().stream().sorted().toList());
        if (u.avatarUrl() != null) {
            m.put("avatarUrl", u.avatarUrl());
        }
        m.put("provider", via);
        return m;
    }

    private static Result error(int status, String message) {
        return new Result(status, Json.write(Map.of("status", "failure", "message", message)));
    }
}
