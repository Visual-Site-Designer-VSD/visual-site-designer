package dev.mainul35.vsd.runtime.session;

import dev.mainul35.cms.sdk.runtime.RuntimeUser;
import dev.mainul35.cms.sdk.runtime.StateKey;
import dev.mainul35.cms.sdk.runtime.StatePatch;
import dev.mainul35.vsd.runtime.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * One visitor's server-side session: identity, versioned state, a bounded patch log used
 * to resume clients after a reconnect, and the open SSE streams.
 *
 * <p>All mutation goes through {@link #apply(List)}, which holds the session lock, assigns
 * strictly increasing revisions and pushes the patches to every open stream, so clients
 * observe changes in the same order everywhere.
 */
public final class Session {

    private static final Logger log = LoggerFactory.getLogger(Session.class);
    static final int LOG_LIMIT = 500;
    private static final int PROCESSED_LIMIT = 200;

    private volatile String id;
    private volatile String csrf;
    private volatile long lastAccess = System.currentTimeMillis();
    private volatile RuntimeUser user;

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<StateKey, StatePatch> state = new LinkedHashMap<>();
    private final ArrayDeque<StatePatch> patchLog = new ArrayDeque<>();
    private long rev;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** eventId -> serialized response, so retried events are answered, not re-run. */
    private final Map<String, String> processed = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > PROCESSED_LIMIT;
        }
    };

    Session(String id, String csrf) {
        this.id = id;
        this.csrf = csrf;
    }

    // ---- identity of the session itself -------------------------------------------------

    public String id() { return id; }
    void setId(String id) { this.id = id; }
    public String csrf() { return csrf; }
    void setCsrf(String csrf) { this.csrf = csrf; }
    long lastAccess() { return lastAccess; }
    void touch() { this.lastAccess = System.currentTimeMillis(); }
    public Optional<RuntimeUser> user() { return Optional.ofNullable(user); }
    public void setUser(RuntimeUser user) { this.user = user; }

    public ReentrantLock lock() { return lock; }

    // ---- state ---------------------------------------------------------------------------

    public Optional<Object> get(StateKey key) {
        lock.lock();
        try {
            StatePatch p = state.get(key);
            return p == null ? Optional.empty() : Optional.ofNullable(p.value());
        } finally {
            lock.unlock();
        }
    }

    public long rev() {
        lock.lock();
        try {
            return rev;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return state.size();
        } finally {
            lock.unlock();
        }
    }

    /** Current state as wire entries (one per key, each with the revision that last set it). */
    public List<Map<String, Object>> snapshotEntries() {
        lock.lock();
        try {
            List<Map<String, Object>> out = new ArrayList<>(state.size());
            for (StatePatch p : state.values()) {
                out.add(toWire(p));
            }
            return out;
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Object> snapshot() {
        lock.lock();
        try {
            Map<String, Object> out = new LinkedHashMap<>();
            for (StatePatch p : state.values()) {
                out.put(p.key().id(), p.value());
            }
            return out;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Applies patches atomically, assigning revisions, and pushes them to open streams.
     * Patches that change nothing are dropped.
     */
    public List<StatePatch> apply(List<StatePatch> patches) {
        lock.lock();
        try {
            List<StatePatch> applied = new ArrayList<>();
            for (StatePatch p : patches) {
                StatePatch existing = state.get(p.key());
                if (p.op() == StatePatch.Op.SET) {
                    if (existing != null && Objects.equals(existing.value(), p.value())) {
                        continue;
                    }
                } else if (existing == null) {
                    continue;
                }
                StatePatch ap = p.withRev(++rev);
                if (ap.op() == StatePatch.Op.SET) {
                    state.put(ap.key(), ap);
                } else {
                    state.remove(ap.key());
                }
                patchLog.addLast(ap);
                if (patchLog.size() > LOG_LIMIT) {
                    patchLog.removeFirst();
                }
                applied.add(ap);
            }
            broadcast(applied);
            return applied;
        } finally {
            lock.unlock();
        }
    }

    /** Removes every state entry (used on logout). */
    public List<StatePatch> clearAll() {
        lock.lock();
        try {
            List<StatePatch> removals = new ArrayList<>();
            for (StateKey k : new ArrayList<>(state.keySet())) {
                removals.add(StatePatch.remove(k));
            }
            return apply(removals);
        } finally {
            lock.unlock();
        }
    }

    // ---- idempotency ---------------------------------------------------------------------

    public String cachedResponse(String eventId) {
        lock.lock();
        try {
            return processed.get(eventId);
        } finally {
            lock.unlock();
        }
    }

    public void cacheResponse(String eventId, String json) {
        lock.lock();
        try {
            processed.put(eventId, json);
        } finally {
            lock.unlock();
        }
    }

    // ---- streams ---------------------------------------------------------------------------

    /**
     * Registers a stream and brings it up to date: replays patches newer than {@code since}
     * or, if the log no longer reaches back that far, sends a full snapshot. Done under the
     * lock so no patch can slip between the replay and the registration.
     */
    public void attach(SseEmitter emitter, long since) throws IOException {
        lock.lock();
        try {
            List<StatePatch> replay = replaySince(since);
            if (replay == null) {
                emitter.send(SseEmitter.event().id(String.valueOf(rev)).name("snapshot")
                        .data(Json.write(Map.of("rev", rev, "entries", snapshotEntries()))));
            } else if (!replay.isEmpty()) {
                emitter.send(SseEmitter.event().id(String.valueOf(rev)).name("patch")
                        .data(Json.write(Map.of("patches", replay.stream().map(Session::toWire).toList()))));
            } else {
                emitter.send(SseEmitter.event().name("ready").data(Json.write(Map.of("rev", rev))));
            }
            emitters.add(emitter);
        } finally {
            lock.unlock();
        }
    }

    public void detach(SseEmitter emitter) {
        emitters.remove(emitter);
    }

    public void heartbeat() {
        for (SseEmitter e : emitters) {
            try {
                e.send(SseEmitter.event().comment("hb"));
            } catch (IOException | IllegalStateException ex) {
                emitters.remove(e);
            }
        }
    }

    public void closeStreams() {
        for (SseEmitter e : emitters) {
            try {
                e.complete();
            } catch (RuntimeException ignored) {
                // already closed
            }
        }
        emitters.clear();
    }

    /** @return patches newer than {@code since}, or null if the log no longer covers that range. */
    private List<StatePatch> replaySince(long since) {
        if (since >= rev) {
            return List.of();
        }
        if (patchLog.isEmpty() || since < patchLog.peekFirst().rev() - 1) {
            return null;
        }
        List<StatePatch> out = new ArrayList<>();
        for (StatePatch p : patchLog) {
            if (p.rev() > since) {
                out.add(p);
            }
        }
        return out;
    }

    private void broadcast(List<StatePatch> applied) {
        if (applied.isEmpty() || emitters.isEmpty()) {
            return;
        }
        String data = Json.write(Map.of("patches", applied.stream().map(Session::toWire).toList()));
        for (SseEmitter e : emitters) {
            try {
                e.send(SseEmitter.event().id(String.valueOf(rev)).name("patch").data(data));
            } catch (IOException | IllegalStateException ex) {
                log.debug("Dropping closed stream for session");
                emitters.remove(e);
            }
        }
    }

    public static Map<String, Object> toWire(StatePatch p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("op", p.op().name().toLowerCase());
        m.put("scope", p.key().scope().name());
        m.put("owner", p.key().owner());
        m.put("key", p.key().key());
        m.put("value", p.value());
        m.put("rev", p.rev());
        return m;
    }
}
