package dev.mainul35.vsd.runtime.web;

import dev.mainul35.cms.sdk.runtime.RuntimeUser;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import dev.mainul35.vsd.runtime.session.Session;
import dev.mainul35.vsd.runtime.session.SessionService;
import dev.mainul35.vsd.runtime.session.SessionStore;
import dev.mainul35.vsd.runtime.shared.SharedStateDef;
import dev.mainul35.vsd.runtime.shared.SharedStateRegistry;
import dev.mainul35.vsd.runtime.util.Json;
import dev.mainul35.vsd.runtime.util.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The runtime console: declare shared state and look at live sessions. Everything under
 * {@code /_vsd/console/api} needs the admin token ({@code X-VSD-Admin-Token}) or a signed-in user with
 * the ADMIN role. The page itself carries no secrets.
 */
@RestController
@RequestMapping("/_vsd/console")
public class ConsoleController {

    private static final int MAX_BODY_CHARS = 64 * 1024;
    private static final int MAX_SESSIONS_LISTED = 200;

    private final RuntimeProperties props;
    private final SharedStateRegistry registry;
    private final SessionService sessions;
    private final SessionStore store;
    private final RateLimiter tokenAttempts = new RateLimiter(60, 60_000);

    public ConsoleController(RuntimeProperties props, SharedStateRegistry registry, SessionService sessions, SessionStore store) {
        this.props = props;
        this.registry = registry;
        this.sessions = sessions;
        this.store = store;
    }

    // ---- the page ---------------------------------------------------------------------------

    @GetMapping({"", "/"})
    public ResponseEntity<byte[]> page() {
        return asset("console/index.html", MediaType.TEXT_HTML);
    }

    @GetMapping("/console.js")
    public ResponseEntity<byte[]> script() {
        return asset("console/console.js", MediaType.parseMediaType("text/javascript"));
    }

    @GetMapping("/console.css")
    public ResponseEntity<byte[]> style() {
        return asset("console/console.css", MediaType.parseMediaType("text/css"));
    }

    private ResponseEntity<byte[]> asset(String path, MediaType type) {
        if (!props.getConsole().isEnabled()) {
            return ResponseEntity.status(404).build();
        }
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return ResponseEntity.ok().contentType(type)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .header("Content-Security-Policy", "default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'")
                    .body(in.readAllBytes());
        } catch (IOException e) {
            return ResponseEntity.status(404).build();
        }
    }

    // ---- shared state declarations ----------------------------------------------------------

    @GetMapping(value = "/api/shared-state", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> list(HttpServletRequest request) {
        ResponseEntity<String> denied = authorize(request, false);
        if (denied != null) {
            return denied;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("strict", props.isStrictSharedState());
        out.put("definitions", registry.all());
        return json(200, Json.write(out));
    }

    @PutMapping(value = "/api/shared-state/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> put(@PathVariable String name, @RequestBody String body, HttpServletRequest request) {
        ResponseEntity<String> denied = authorize(request, true);
        if (denied != null) {
            return denied;
        }
        if (body.length() > MAX_BODY_CHARS) {
            return failure(413, "Request too large");
        }
        try {
            SharedStateDef def = SharedStateRegistry.parse(body);
            if (def.getName() != null && !def.getName().equals(name)) {
                return failure(400, "The name in the body must match the URL");
            }
            def.setName(name);
            return json(200, Json.write(registry.put(def)));
        } catch (IllegalArgumentException e) {
            return failure(400, e.getMessage());
        } catch (IllegalStateException e) {
            return failure(500, e.getMessage());
        }
    }

    @DeleteMapping(value = "/api/shared-state/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> delete(@PathVariable String name, HttpServletRequest request) {
        ResponseEntity<String> denied = authorize(request, true);
        if (denied != null) {
            return denied;
        }
        try {
            return registry.delete(name) ? json(200, "{\"status\":\"success\"}") : failure(404, "Not declared");
        } catch (IllegalStateException e) {
            return failure(500, e.getMessage());
        }
    }

    // ---- live sessions ----------------------------------------------------------------------

    @GetMapping(value = "/api/sessions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> sessions(HttpServletRequest request) {
        ResponseEntity<String> denied = authorize(request, false);
        if (denied != null) {
            return denied;
        }
        List<Session> all = new ArrayList<>(store.all());
        all.sort(Comparator.comparingLong(Session::idleSeconds));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Session s : all.stream().limit(MAX_SESSIONS_LISTED).toList()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("handle", handle(s));
            m.put("user", s.user().map(RuntimeUser::username).orElse(null));
            m.put("entries", s.size());
            m.put("rev", s.rev());
            m.put("idleSeconds", s.idleSeconds());
            rows.add(m);
        }
        return json(200, Json.write(Map.of("total", all.size(), "sessions", rows)));
    }

    @GetMapping(value = "/api/sessions/{handle}/state", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> sessionState(@PathVariable String handle, HttpServletRequest request) {
        ResponseEntity<String> denied = authorize(request, false);
        if (denied != null) {
            return denied;
        }
        Optional<Session> s = store.all().stream().filter(x -> handle(x).equals(handle)).findFirst();
        if (s.isEmpty()) {
            return failure(404, "No such session");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rev", s.get().rev());
        out.put("user", s.get().user().map(RuntimeUser::username).orElse(null));
        out.put("state", s.get().snapshot());
        return json(200, Json.write(out));
    }

    /** A short fingerprint of the session id. The id itself is the visitor's cookie and is never shown. */
    private static String handle(Session s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.id().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02x", d[i]));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- access -----------------------------------------------------------------------------

    /** @return null if the caller may use the console, otherwise the error to send. */
    private ResponseEntity<String> authorize(HttpServletRequest request, boolean changes) {
        if (!props.getConsole().isEnabled()) {
            return failure(404, "The console is turned off");
        }
        String configured = props.getConsole().getAdminToken();
        String given = request.getHeader("X-VSD-Admin-Token");
        if (given != null) {
            if (!tokenAttempts.tryAcquire(String.valueOf(request.getRemoteAddr()))) {
                return failure(429, "Too many attempts. Try again in a minute.");
            }
            if (configured != null && !configured.isBlank() && MessageDigest.isEqual(
                    given.getBytes(StandardCharsets.UTF_8), configured.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            return failure(401, "Wrong admin token");
        }
        // Cookie-based access for signed-in admins; changes also need the CSRF header and a matching Origin.
        Optional<Session> s = sessions.find(request);
        if (s.isPresent() && s.get().user().map(u -> u.hasRole("ADMIN")).orElse(false)) {
            if (!changes || sessions.passesCsrf(s.get(), request)) {
                return null;
            }
            return failure(403, "csrf");
        }
        return failure(401, "Admin token or an admin sign-in is required");
    }

    private static ResponseEntity<String> failure(int status, String message) {
        return json(status, Json.write(Map.of("status", "failure", "message", String.valueOf(message))));
    }

    private static ResponseEntity<String> json(int status, String body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(body);
    }
}
