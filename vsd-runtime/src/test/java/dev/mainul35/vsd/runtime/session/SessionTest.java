package dev.mainul35.vsd.runtime.session;

import dev.mainul35.cms.sdk.runtime.StateKey;
import dev.mainul35.cms.sdk.runtime.StatePatch;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTest {

    private static final StateKey A = StateKey.plugin("p", "a");
    private static final StateKey B = StateKey.plugin("p", "b");

    private Session session() {
        return new Session("sid", "csrf");
    }

    @Test
    void revisionsIncreaseStrictlyAcrossApplies() {
        Session s = session();
        List<StatePatch> first = s.apply(List.of(StatePatch.set(A, 1), StatePatch.set(B, 2)));
        List<StatePatch> second = s.apply(List.of(StatePatch.set(A, 3)));
        assertEquals(List.of(1L, 2L), first.stream().map(StatePatch::rev).toList());
        assertEquals(3L, second.get(0).rev());
        assertEquals(3L, s.rev());
    }

    @Test
    void patchesThatChangeNothingAreDropped() {
        Session s = session();
        s.apply(List.of(StatePatch.set(A, Map.of("x", 1))));
        assertTrue(s.apply(List.of(StatePatch.set(A, Map.of("x", 1)))).isEmpty(), "same value");
        assertTrue(s.apply(List.of(StatePatch.remove(B))).isEmpty(), "removing a missing key");
        assertEquals(1L, s.rev());
    }

    @Test
    void removeDeletesTheEntry() {
        Session s = session();
        s.apply(List.of(StatePatch.set(A, 1)));
        s.apply(List.of(StatePatch.remove(A)));
        assertTrue(s.get(A).isEmpty());
        assertEquals(0, s.size());
    }

    @Test
    void clearAllRemovesEverythingWithRevisions() {
        Session s = session();
        s.apply(List.of(StatePatch.set(A, 1), StatePatch.set(B, 2)));
        List<StatePatch> removed = s.clearAll();
        assertEquals(2, removed.size());
        assertTrue(removed.stream().allMatch(p -> p.op() == StatePatch.Op.REMOVE && p.rev() > 2));
        assertEquals(0, s.size());
    }

    @Test
    void snapshotCarriesTheRevisionThatLastSetEachKey() {
        Session s = session();
        s.apply(List.of(StatePatch.set(A, 1)));
        s.apply(List.of(StatePatch.set(B, 2)));
        s.apply(List.of(StatePatch.set(A, 9)));
        Map<Object, Object> revs = new java.util.HashMap<>();
        s.snapshotEntries().forEach(e -> revs.put(e.get("key"), e.get("rev")));
        assertEquals(3L, revs.get("a"));
        assertEquals(2L, revs.get("b"));
    }

    @Test
    void retriedEventsAreAnsweredFromTheCache() {
        Session s = session();
        s.cacheResponse("evt-1", "{\"status\":\"success\"}");
        assertEquals("{\"status\":\"success\"}", s.cachedResponse("evt-1"));
        assertEquals(null, s.cachedResponse("evt-2"));
    }

    @Test
    void idempotencyCacheIsBounded() {
        Session s = session();
        for (int i = 0; i < 500; i++) {
            s.cacheResponse("e" + i, "r");
        }
        assertEquals(null, s.cachedResponse("e0"), "oldest entries are evicted");
        assertEquals("r", s.cachedResponse("e499"));
    }

    @Test
    void streamsCanAttachFromAnyRevisionIncludingAfterLogOverflow() throws Exception {
        Session s = session();
        // More changes than the patch log retains: a client resuming from 0 must be given a
        // full snapshot instead of an incomplete replay. attach() would throw if sending failed.
        List<StatePatch> many = new ArrayList<>();
        for (int i = 0; i < Session.LOG_LIMIT + 50; i++) {
            many.add(StatePatch.set(StateKey.plugin("p", "k" + i), i));
        }
        s.apply(many);
        s.attach(new SseEmitter(1000L), 0);
        s.attach(new SseEmitter(1000L), s.rev());
        s.attach(new SseEmitter(1000L), s.rev() - 3);
        assertFalse(s.snapshotEntries().isEmpty());
    }

    @Test
    void sessionRotationIsVisibleToTheStore() {
        InMemorySessionStore store = new InMemorySessionStore();
        Session s = store.create();
        String old = s.id();
        store.rekey(s);
        assertTrue(store.find(old).isEmpty(), "old id must stop working");
        assertEquals(s, store.find(s.id()).orElseThrow());
    }
}
