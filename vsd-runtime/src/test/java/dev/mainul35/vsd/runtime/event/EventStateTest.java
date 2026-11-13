package dev.mainul35.vsd.runtime.event;

import dev.mainul35.cms.sdk.runtime.StateKey;
import dev.mainul35.cms.sdk.runtime.StatePatch;
import dev.mainul35.vsd.runtime.session.InMemorySessionStore;
import dev.mainul35.vsd.runtime.session.Session;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventStateTest {

    private static final StateKey K = StateKey.plugin("p", "k");

    private final Session session = new InMemorySessionStore().create();

    @Test
    void writesAreStagedUntilTheDispatcherAppliesThem() {
        EventState state = new EventState(session, 1024, 10);
        state.set(K, "v");
        assertTrue(session.get(K).isEmpty(), "session untouched while the handler runs");
        assertEquals("v", state.get(K).orElseThrow(), "handler reads its own write");
        assertEquals(0, session.rev());

        session.apply(state.pending());
        assertEquals("v", session.get(K).orElseThrow());
    }

    @Test
    void discardingPendingWritesLeavesNoTrace() {
        EventState state = new EventState(session, 1024, 10);
        state.set(K, "v");
        // a failing handler simply never has its pending list applied
        assertTrue(session.get(K).isEmpty());
        assertEquals(0, session.rev());
    }

    @Test
    void stagedRemovalHidesExistingValue() {
        session.apply(List.of(StatePatch.set(K, "old")));
        EventState state = new EventState(session, 1024, 10);
        state.remove(K);
        assertTrue(state.get(K).isEmpty());
        assertTrue(state.snapshot().isEmpty());
        assertEquals("old", session.get(K).orElseThrow());
    }

    @Test
    void handlersMayWriteReservedOwnersButNotInvalidNames() {
        EventState state = new EventState(session, 1024, 10);
        state.set(StateKey.session("$auth", "user"), Map.of("id", "u1"));
        assertThrows(IllegalArgumentException.class, () -> state.set(StateKey.session("bad/owner", "k"), 1));
    }

    @Test
    void enforcesValueSizeAndKeyCount() {
        EventState small = new EventState(session, 16, 10);
        assertThrows(IllegalArgumentException.class, () -> small.set(K, "x".repeat(100)));

        EventState few = new EventState(session, 1024, 2);
        few.set(StateKey.plugin("p", "a"), 1);
        few.set(StateKey.plugin("p", "b"), 1);
        assertThrows(IllegalArgumentException.class, () -> few.set(StateKey.plugin("p", "c"), 1));
    }
}
