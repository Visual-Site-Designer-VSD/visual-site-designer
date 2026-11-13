package dev.mainul35.vsd.runtime.event;

import dev.mainul35.cms.sdk.runtime.RuntimeState;
import dev.mainul35.cms.sdk.runtime.StateKey;
import dev.mainul35.cms.sdk.runtime.StatePatch;
import dev.mainul35.vsd.runtime.session.Session;
import dev.mainul35.vsd.runtime.session.StateRules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The state view handed to a handler. Writes are staged and only applied to the session if
 * the handler succeeds, so a handler that throws or returns a failure changes nothing.
 * Reads see the handler's own staged writes.
 */
final class EventState implements RuntimeState {

    private final Session session;
    private final int maxValueBytes;
    private final int maxKeys;
    private final List<StatePatch> pending = new ArrayList<>();

    EventState(Session session, int maxValueBytes, int maxKeys) {
        this.session = session;
        this.maxValueBytes = maxValueBytes;
        this.maxKeys = maxKeys;
    }

    List<StatePatch> pending() {
        return pending;
    }

    @Override
    public Optional<Object> get(StateKey key) {
        for (int i = pending.size() - 1; i >= 0; i--) {
            StatePatch p = pending.get(i);
            if (p.key().equals(key)) {
                return p.op() == StatePatch.Op.SET ? Optional.ofNullable(p.value()) : Optional.empty();
            }
        }
        return session.get(key);
    }

    @Override
    public <T> Optional<T> get(StateKey key, Class<T> type) {
        return get(key).filter(type::isInstance).map(type::cast);
    }

    @Override
    public void set(StateKey key, Object value) {
        // Handlers are server code, so they may write reserved owners; names are still validated.
        StateRules.check(key, true);
        Object normalized = StateRules.normalize(value, maxValueBytes);
        if (session.get(key).isEmpty() && session.size() + countNew() >= maxKeys) {
            throw new IllegalArgumentException("Too many state entries");
        }
        pending.add(StatePatch.set(key, normalized));
    }

    @Override
    public void remove(StateKey key) {
        pending.add(StatePatch.remove(key));
    }

    @Override
    public Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>(session.snapshot());
        for (StatePatch p : pending) {
            if (p.op() == StatePatch.Op.SET) {
                out.put(p.key().id(), p.value());
            } else {
                out.remove(p.key().id());
            }
        }
        return Map.copyOf(out);
    }

    private int countNew() {
        return (int) pending.stream()
                .filter(p -> p.op() == StatePatch.Op.SET)
                .map(StatePatch::key)
                .distinct()
                .filter(k -> session.get(k).isEmpty())
                .count();
    }
}
