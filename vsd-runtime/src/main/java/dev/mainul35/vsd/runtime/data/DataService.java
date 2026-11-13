package dev.mainul35.vsd.runtime.data;

import com.fasterxml.jackson.databind.JsonNode;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import dev.mainul35.vsd.runtime.util.RateLimiter;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads API data sources for pages, with a small shared cache. The cache is only used for sources whose author
 * asked for caching ({@code cacheKey} or {@code cacheTtlMs}); entries are keyed by what is fetched (URL, method
 * and headers), never by visitor, so nothing visitor-specific is ever stored.
 */
@Component
public class DataService implements SourceLoader {

    private record Cached(JsonNode value, long expiresAt) {
    }

    private final SourceLoader delegate;
    private final Duration defaultTtl;
    private final Map<String, Cached> cache;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Duration pageDeadline;
    private final RateLimiter upstreamLimiter;

    @Autowired
    public DataService(RuntimeProperties props) {
        this(new HttpSourceFetcher(props.getData()), props.getData());
    }

    /** For tests: a custom loader in place of HTTP. */
    DataService(SourceLoader delegate, RuntimeProperties.Data config) {
        this.delegate = delegate;
        this.defaultTtl = config.getDefaultCacheTtl();
        this.pageDeadline = config.getPageDeadline();
        this.upstreamLimiter = new RateLimiter(Math.max(1, config.getMaxCallsPerSecond()), 1000);
        int max = Math.max(1, config.getCacheMaxEntries());
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Cached> eldest) {
                return size() > max;
            }
        };
    }

    public ExecutorService executor() {
        return executor;
    }

    public Duration pageDeadline() {
        return pageDeadline;
    }

    @Override
    public JsonNode load(SourceSpec spec) throws Exception {
        String key = spec.fingerprint();
        if (!spec.cached()) {
            if (!upstreamLimiter.tryAcquire(key)) {
                throw new IllegalStateException("too many calls to this data source");
            }
            return delegate.load(spec);
        }
        long now = System.currentTimeMillis();
        synchronized (cache) {
            Cached e = cache.get(key);
            if (e != null && e.expiresAt() > now) {
                return e.value();
            }
        }
        if (!upstreamLimiter.tryAcquire(key)) {
            throw new IllegalStateException("too many calls to this data source");
        }
        JsonNode fresh = delegate.load(spec);
        long ttl = spec.cacheTtlMs() != null ? spec.cacheTtlMs() : defaultTtl.toMillis();
        synchronized (cache) {
            cache.put(key, new Cached(fresh, System.currentTimeMillis() + Math.max(0, ttl)));
        }
        return fresh;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
