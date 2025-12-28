package dev.mainul35.siteruntime.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache provider.
 * Used as default when Redis is not configured.
 */
public class InMemoryCacheProvider {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCacheProvider.class);

    private final Map<String, CachedData> cache = new ConcurrentHashMap<>();
    private final long defaultTtlMs;

    public InMemoryCacheProvider(long defaultTtlMs) {
        this.defaultTtlMs = defaultTtlMs;
        log.info("InMemoryCacheProvider initialized with default TTL: {}ms", defaultTtlMs);
    }

    /**
     * Get a value from cache.
     */
    public Object get(String key) {
        CachedData cached = cache.get(key);
        if (cached == null) {
            return null;
        }

        if (cached.isExpired()) {
            cache.remove(key);
            return null;
        }

        return cached.getData();
    }

    /**
     * Put a value in cache with default TTL.
     */
    public void put(String key, Object value) {
        put(key, value, defaultTtlMs);
    }

    /**
     * Put a value in cache with custom TTL.
     */
    public void put(String key, Object value, long ttlMs) {
        cache.put(key, new CachedData(value, ttlMs));
    }

    /**
     * Remove a value from cache.
     */
    public void remove(String key) {
        cache.remove(key);
    }

    /**
     * Clear all cache entries.
     */
    public void clear() {
        cache.clear();
        log.info("Cache cleared");
    }

    /**
     * Get cache statistics.
     */
    public Map<String, Object> getStats() {
        long validEntries = cache.values().stream()
                .filter(c -> !c.isExpired())
                .count();
        long expiredEntries = cache.size() - validEntries;

        return Map.of(
                "totalEntries", cache.size(),
                "validEntries", validEntries,
                "expiredEntries", expiredEntries
        );
    }

    /**
     * Simple cache entry with TTL.
     */
    private static class CachedData {
        private final Object data;
        private final long expiresAt;

        CachedData(Object data, long ttlMs) {
            this.data = data;
            this.expiresAt = System.currentTimeMillis() + ttlMs;
        }

        Object getData() { return data; }
        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }
}
