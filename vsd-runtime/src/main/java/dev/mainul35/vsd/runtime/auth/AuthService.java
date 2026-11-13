package dev.mainul35.vsd.runtime.auth;

import dev.mainul35.cms.sdk.runtime.IdentityProvider;
import dev.mainul35.cms.sdk.runtime.RuntimeUser;
import dev.mainul35.cms.sdk.runtime.StateKey;
import dev.mainul35.cms.sdk.runtime.StatePatch;
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

    private final IdentityProvider provider;
    private final SessionService sessions;
    private final RateLimiter loginLimiter = new RateLimiter(5, 60_000);

    public AuthService(IdentityProvider provider, SessionService sessions) {
        this.provider = provider;
        this.sessions = sessions;
    }

    public record Result(int status, String json) {
    }

    public Result login(Session session, String username, String password, String ip,
                        HttpServletRequest request, HttpServletResponse response) {
        String limitKey = ip + "|" + (username == null ? "" : username.toLowerCase());
        if (!loginLimiter.tryAcquire(limitKey)) {
            return error(429, "Too many attempts. Try again later.");
        }
        Optional<RuntimeUser> user = provider.authenticate(username, password);
        if (user.isEmpty()) {
            return error(401, "Invalid credentials");
        }
        session.lock().lock();
        try {
            sessions.rotate(session, request, response);
            session.setUser(user.get());
            List<StatePatch> applied = session.apply(List.of(StatePatch.set(USER_KEY, toMap(user.get()))));
            return new Result(200, Json.write(body(session, applied)));
        } finally {
            session.lock().unlock();
        }
    }

    public Result logout(Session session, HttpServletRequest request, HttpServletResponse response) {
        session.lock().lock();
        try {
            session.setUser(null);
            List<StatePatch> applied = new ArrayList<>(session.clearAll());
            sessions.rotate(session, request, response);
            return new Result(200, Json.write(body(session, applied)));
        } finally {
            session.lock().unlock();
        }
    }

    private static Map<String, Object> body(Session session, List<StatePatch> applied) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "success");
        out.put("csrf", session.csrf());
        out.put("rev", session.rev());
        out.put("patches", applied.stream().map(Session::toWire).toList());
        return out;
    }

    static Map<String, Object> toMap(RuntimeUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.id());
        m.put("username", u.username());
        m.put("displayName", u.displayName());
        m.put("email", u.email());
        m.put("roles", u.roles().stream().sorted().toList());
        return m;
    }

    private static Result error(int status, String message) {
        return new Result(status, Json.write(Map.of("status", "failure", "message", message)));
    }
}
