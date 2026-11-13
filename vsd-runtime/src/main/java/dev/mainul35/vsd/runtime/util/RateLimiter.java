package dev.mainul35.vsd.runtime.util;

import java.util.concurrent.ConcurrentHashMap;

/** Small fixed-window rate limiter. In memory, per node. */
public final class RateLimiter {

    private record Window(long start, int count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final long windowMillis;

    public RateLimiter(int limit, long windowMillis) {
        this.limit = limit;
        this.windowMillis = windowMillis;
    }

    /** @return true if the call is allowed, false if the limit for this key is exceeded. */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> now - e.getValue().start() > windowMillis);
        }
        Window w = windows.compute(key, (k, old) ->
                old == null || now - old.start() >= windowMillis ? new Window(now, 1) : new Window(old.start(), old.count() + 1));
        return w.count() <= limit;
    }
}
