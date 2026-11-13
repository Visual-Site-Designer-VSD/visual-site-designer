package dev.mainul35.cms.sdk.runtime;

/**
 * One versioned change to session state. {@code rev} is a per-session, strictly increasing
 * revision assigned by the runtime when the patch is applied (0 while still pending).
 */
public record StatePatch(Op op, StateKey key, Object value, long rev) {

    public enum Op { SET, REMOVE }

    public static StatePatch set(StateKey key, Object value) {
        return new StatePatch(Op.SET, key, value, 0);
    }

    public static StatePatch remove(StateKey key) {
        return new StatePatch(Op.REMOVE, key, null, 0);
    }

    public StatePatch withRev(long newRev) {
        return new StatePatch(op, key, value, newRev);
    }
}
