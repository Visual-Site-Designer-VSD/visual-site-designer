package dev.mainul35.cms.sdk.runtime;

import java.util.Map;
import java.util.Optional;

/** Read/write view of one visitor session's state. Writes are versioned and pushed to clients. */
public interface RuntimeState {

    Optional<Object> get(StateKey key);

    <T> Optional<T> get(StateKey key, Class<T> type);

    void set(StateKey key, Object value);

    void remove(StateKey key);

    /** Immutable copy of the current state keyed by {@link StateKey#id()}. */
    Map<String, Object> snapshot();
}
