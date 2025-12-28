package dev.mainul35.siteruntime.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Registry of data fetchers. Selects appropriate fetcher based on data source type.
 * Uses Strategy pattern to handle different data source types.
 */
public class DataSourceRegistry {

    private static final Logger log = LoggerFactory.getLogger(DataSourceRegistry.class);

    private final List<DataFetcher> fetchers;

    public DataSourceRegistry(List<DataFetcher> fetchers) {
        this.fetchers = fetchers;
        log.info("DataSourceRegistry initialized with {} fetchers", fetchers.size());
    }

    /**
     * Find a fetcher that supports the given data source type.
     */
    public DataFetcher getFetcher(DataSourceType type) {
        return fetchers.stream()
                .filter(f -> f.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No data fetcher found for type: " + type +
                        ". Check your site.runtime.* properties."));
    }

    /**
     * Fetch data using the appropriate fetcher.
     */
    public Object fetch(DataSourceConfig config, Map<String, String> params) {
        DataFetcher fetcher = getFetcher(config.getType());
        return fetcher.fetch(config, params);
    }

    /**
     * Check if a fetcher exists for the given type.
     */
    public boolean hasFetcher(DataSourceType type) {
        return fetchers.stream().anyMatch(f -> f.supports(type));
    }

    /**
     * Get all registered fetcher types.
     */
    public List<DataSourceType> getSupportedTypes() {
        return fetchers.stream()
                .flatMap(f -> Arrays.stream(DataSourceType.values())
                        .filter(f::supports))
                .distinct()
                .toList();
    }
}
