package dev.mainul35.vsd.runtime.web;

import dev.mainul35.cms.sdk.runtime.StateKey;
import dev.mainul35.cms.sdk.runtime.StatePatch;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import dev.mainul35.vsd.runtime.auth.AuthService;
import dev.mainul35.vsd.runtime.event.EventDispatcher;
import dev.mainul35.vsd.runtime.session.Session;
import dev.mainul35.vsd.runtime.session.SessionService;
import dev.mainul35.vsd.runtime.session.StateRules;
import dev.mainul35.vsd.runtime.shared.SharedStateService;
import dev.mainul35.vsd.runtime.snapshot.SnapshotService;
import dev.mainul35.vsd.runtime.util.Json;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Browser-facing runtime API: session info, the state stream (SSE), events, direct state
 * writes and sign-in/out. Every POST needs the session cookie, the CSRF header and a
 * matching Origin. No CORS headers are sent, so other sites cannot read responses.
 */
@RestController
@RequestMapping("/_vsd/api")
public class ApiController {

    private static final int MAX_BODY_CHARS = 64 * 1024;
    private static final int MAX_PATCHES_PER_REQUEST = 50;
    private static final long STREAM_TIMEOUT_MS = 30 * 60 * 1000L;

    private final SessionService sessions;
    private final EventDispatcher dispatcher;
    private final AuthService auth;
    private final RuntimeProperties props;
    private final SnapshotService snapshots;
    private final PageView views;
    private final SharedStateService shared;

    public ApiController(SessionService sessions, EventDispatcher dispatcher, AuthService auth, RuntimeProperties props,
                         SnapshotService snapshots, PageView views, SharedStateService shared) {
        this.shared = shared;
        this.snapshots = snapshots;
        this.views = views;
        this.sessions = sessions;
        this.dispatcher = dispatcher;
        this.auth = auth;
        this.props = props;
    }

    /**
     * The page as this visitor should see it now. The client asks again after sign-in or sign-out, because
     * bindings such as {{auth.user.displayName}} depend on who is signed in.
     */
    @GetMapping(value = "/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> page(@RequestParam("path") String path,
                                       @RequestParam(value = "query", required = false) String query,
                                       HttpServletRequest request, HttpServletResponse response) {
        var entry = snapshots.byPath(path);
        if (entry.isEmpty()) {
            return json(404, "{\"error\":\"not_found\"}").build();
        }
        Session s = sessions.getOrCreate(request, response);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("definition", views.definition(entry.get(), s, PageView.query(query)));
        return json(200, Json.write(out)).build();
    }

    /** Current session snapshot. Clients call this to recover after their session expired. */
    @GetMapping(value = "/session", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> session(HttpServletRequest request, HttpServletResponse response) {
        Session s = sessions.getOrCreate(request, response);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("csrf", s.csrf());
        out.put("rev", s.rev());
        out.put("entries", s.snapshotEntries());
        out.put("shared", shared.publicView());
        return json(200, Json.write(out)).build();
    }

    @GetMapping("/stream")
    public Object stream(@RequestParam(name = "since", required = false) Long sinceParam,
                         HttpServletRequest request, HttpServletResponse response) {
        Optional<Session> found = sessions.find(request);
        if (found.isEmpty()) {
            return json(401, error("no_session")).build();
        }
        Session s = found.get();
        long since = sinceParam != null ? sinceParam : s.rev();
        String lastEventId = request.getHeader("Last-Event-ID");
        if (lastEventId != null) {
            try {
                since = Long.parseLong(lastEventId.trim());
            } catch (NumberFormatException ignored) {
                // keep the query value
            }
        }
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitter.onCompletion(() -> s.detach(emitter));
        emitter.onTimeout(() -> s.detach(emitter));
        emitter.onError(e -> s.detach(emitter));
        try {
            s.attach(emitter, since);
        } catch (IOException e) {
            s.detach(emitter);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @PostMapping(value = "/events", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> events(@RequestBody String body, HttpServletRequest request) {
        Guard g = guard(body, request);
        if (g.failure != null) {
            return g.failure;
        }
        EventDispatcher.Response r = dispatcher.dispatch(g.session, g.json, request.getRemoteAddr());
        return json(r.status(), r.json()).build();
    }

    /** Direct client writes to non-reserved state (for example a component remembering a tab choice). */
    @PostMapping(value = "/state", produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> state(@RequestBody String body, HttpServletRequest request) {
        Guard g = guard(body, request);
        if (g.failure != null) {
            return g.failure;
        }
        Object raw = g.json.get("patches");
        if (!(raw instanceof List<?> list) || list.isEmpty() || list.size() > MAX_PATCHES_PER_REQUEST) {
            return json(400, error("Invalid patches")).build();
        }
        List<StatePatch> patches = new ArrayList<>();
        try {
            for (Object o : list) {
                Map<String, Object> m = (Map<String, Object>) o;
                StateKey key = StateRules.parse(String.valueOf(m.get("scope")), String.valueOf(m.get("owner")),
                        String.valueOf(m.get("key")), false);
                // The browser says which plugin is writing. That is a guard against plugins stepping on each
                // other, not against hostile code: for that, declare the state with clientWritable=false.
                String writer = m.get("plugin") instanceof String w && w.length() <= 128 ? w : null;
                if ("remove".equals(m.get("op"))) {
                    shared.checkWrite(g.session, key, null, true, writer, true);
                    patches.add(StatePatch.remove(key));
                } else if ("set".equals(m.get("op"))) {
                    Object value = StateRules.normalize(m.get("value"), props.getMaxValueBytes());
                    shared.checkWrite(g.session, key, value, false, writer, true);
                    patches.add(StatePatch.set(key, value));
                } else {
                    return json(400, error("Invalid op")).build();
                }
            }
        } catch (IllegalArgumentException | ClassCastException e) {
            return json(400, error(e instanceof IllegalArgumentException ? e.getMessage() : "Invalid patches")).build();
        }
        Session s = g.session;
        s.lock().lock();
        try {
            long fresh = patches.stream().filter(p -> p.op() == StatePatch.Op.SET)
                    .map(StatePatch::key).distinct().filter(k -> s.get(k).isEmpty()).count();
            if (s.size() + fresh > props.getMaxStateKeys()) {
                return json(400, error("Too many state entries")).build();
            }
            List<StatePatch> applied = s.apply(patches);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("status", "success");
            out.put("patches", applied.stream().map(Session::toWire).toList());
            out.put("rev", s.rev());
            return json(200, Json.write(out)).build();
        } finally {
            s.lock().unlock();
        }
    }

    @PostMapping(value = "/auth/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> login(@RequestBody String body, HttpServletRequest request, HttpServletResponse response) {
        Guard g = guard(body, request);
        if (g.failure != null) {
            return g.failure;
        }
        Object user = g.json.get("username") != null ? g.json.get("username") : g.json.get("email");
        Object pass = g.json.get("password");
        if (!(user instanceof String u) || !(pass instanceof String p) || u.length() > 256 || p.length() > 256) {
            return json(400, error("Invalid credentials")).build();
        }
        AuthService.Result r = auth.login(g.session, u, p, request.getRemoteAddr(), request, response);
        return json(r.status(), r.json()).build();
    }

    @PostMapping(value = "/auth/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> logout(@RequestBody(required = false) String body, HttpServletRequest request, HttpServletResponse response) {
        Guard g = guard(body == null || body.isBlank() ? "{}" : body, request);
        if (g.failure != null) {
            return g.failure;
        }
        AuthService.Result r = auth.logout(g.session, request, response);
        return json(r.status(), r.json()).build();
    }

    // ---- helpers ---------------------------------------------------------------------------

    private record Guard(Session session, Map<String, Object> json, ResponseEntity<String> failure) {
    }

    @SuppressWarnings("unchecked")
    private Guard guard(String body, HttpServletRequest request) {
        Optional<Session> found = sessions.find(request);
        if (found.isEmpty()) {
            return new Guard(null, null, json(401, error("no_session")).build());
        }
        if (!sessions.passesCsrf(found.get(), request)) {
            return new Guard(null, null, json(403, error("csrf")).build());
        }
        if (body == null || body.length() > MAX_BODY_CHARS) {
            return new Guard(null, null, json(413, error("Request too large")).build());
        }
        try {
            Map<String, Object> parsed = Json.MAPPER.readValue(body, Map.class);
            return new Guard(found.get(), parsed, null);
        } catch (IOException | RuntimeException e) {
            return new Guard(null, null, json(400, error("Malformed JSON")).build());
        }
    }

    private static String error(String message) {
        return Json.write(Map.of("status", "failure", "message", message));
    }

    /** Tiny builder so every API reply is JSON with caching disabled. */
    private static Reply json(int status, String body) {
        return new Reply(status, body);
    }

    private record Reply(int status, String body) {
        ResponseEntity<String> build() {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Cache-Control", "no-store")
                    .body(body);
        }
    }
}
