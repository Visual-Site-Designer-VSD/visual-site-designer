package dev.mainul35.siteruntime.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.siteruntime.config.SiteRuntimeProperties;
import dev.mainul35.siteruntime.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for loading and providing page data in exported sites.
 * Reads page definitions from resources and fetches data from configured sources.
 */
public class PageDataService {

    private static final Logger log = LoggerFactory.getLogger(PageDataService.class);

    private final DataSourceRegistry dataSourceRegistry;
    private final ObjectMapper objectMapper;
    private final SiteRuntimeProperties properties;

    // Simple in-memory cache
    private final Map<String, CachedData> cache = new ConcurrentHashMap<>();

    public PageDataService(
            DataSourceRegistry dataSourceRegistry,
            ObjectMapper objectMapper,
            SiteRuntimeProperties properties) {
        this.dataSourceRegistry = dataSourceRegistry;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Load page data from a page definition file.
     *
     * @param pageName The page name (e.g., "home", "about")
     * @return PageData containing fetched data and page metadata
     */
    public PageData loadPageData(String pageName) {
        return loadPageData(pageName, Map.of());
    }

    /**
     * Load page data from a page definition file with parameters.
     *
     * @param pageName The page name (e.g., "home", "about")
     * @param params Request parameters for data fetching
     * @return PageData containing fetched data and page metadata
     */
    public PageData loadPageData(String pageName, Map<String, String> params) {
        long startTime = System.currentTimeMillis();

        // Load page definition from resources
        PageDefinition pageDefinition = loadPageDefinition(pageName);

        if (pageDefinition == null) {
            throw new RuntimeException("Page not found: " + pageName);
        }

        // Parse data sources from page definition
        Map<String, DataSourceConfig> dataSources = pageDefinition.getDataSources();

        if (dataSources == null || dataSources.isEmpty()) {
            return PageData.builder()
                    .data(Map.of())
                    .errors(Map.of())
                    .pageMeta(createPageMeta(pageDefinition))
                    .fetchTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // Fetch all data sources in parallel
        Map<String, Object> aggregatedData = new ConcurrentHashMap<>();
        Map<String, String> errors = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = dataSources.entrySet().stream()
                .map(entry -> CompletableFuture.runAsync(() -> {
                    String key = entry.getKey();
                    DataSourceConfig config = entry.getValue();
                    try {
                        Object data = fetchFromDataSource(config, params);
                        aggregatedData.put(key, data);
                    } catch (Exception e) {
                        log.error("Failed to fetch data source '{}': {}", key, e.getMessage());
                        errors.put(key, e.getMessage());
                    }
                }))
                .collect(Collectors.toList());

        // Wait for all fetches to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return PageData.builder()
                .data(aggregatedData)
                .errors(errors)
                .pageMeta(createPageMeta(pageDefinition))
                .fetchTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * Fetch a single data source by key.
     */
    public Object fetchDataSource(String pageName, String dataSourceKey, Map<String, String> params) {
        PageDefinition pageDefinition = loadPageDefinition(pageName);

        if (pageDefinition == null) {
            throw new RuntimeException("Page not found: " + pageName);
        }

        Map<String, DataSourceConfig> dataSources = pageDefinition.getDataSources();
        if (dataSources == null) {
            throw new RuntimeException("No data sources configured for page: " + pageName);
        }

        DataSourceConfig config = dataSources.get(dataSourceKey);
        if (config == null) {
            throw new RuntimeException("Data source not found: " + dataSourceKey);
        }

        return fetchFromDataSource(config, params);
    }

    /**
     * Load page definition from resources/pages/{pageName}.json
     */
    private PageDefinition loadPageDefinition(String pageName) {
        String resourcePath = "pages/" + pageName + ".json";

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("Page definition not found: {}", resourcePath);
                return null;
            }
            return objectMapper.readValue(is, PageDefinition.class);
        } catch (IOException e) {
            log.error("Failed to load page definition: {}", resourcePath, e);
            return null;
        }
    }

    /**
     * Fetch data from a configured data source.
     */
    private Object fetchFromDataSource(DataSourceConfig config, Map<String, String> params) {
        // Check cache first
        if (config.getCacheKey() != null) {
            CachedData cached = cache.get(config.getCacheKey());
            if (cached != null && !cached.isExpired()) {
                log.debug("Cache hit for key: {}", config.getCacheKey());
                return cached.getData();
            }
        }

        // Fetch using registry
        Object result = dataSourceRegistry.fetch(config, params);

        // Apply field mapping if configured
        if (config.getFieldMapping() != null && result != null) {
            result = applyFieldMapping(result, config.getFieldMapping());
        }

        // Cache the result if caching is enabled
        if (config.getCacheKey() != null && result != null) {
            long ttl = config.getCacheTtlMs() != null
                    ? config.getCacheTtlMs()
                    : properties.getCache().getDefaultTtlMs();
            cache.put(config.getCacheKey(), new CachedData(result, ttl));
        }

        return result;
    }

    /**
     * Apply field mapping to transform data.
     */
    private Object applyFieldMapping(Object data, Map<String, FieldMappingConfig> fieldMapping) {
        if (fieldMapping == null || fieldMapping.isEmpty()) {
            return data;
        }

        try {
            JsonNode rootNode = objectMapper.valueToTree(data);
            Map<String, Object> mappedResult = new HashMap<>();

            for (Map.Entry<String, FieldMappingConfig> entry : fieldMapping.entrySet()) {
                String targetField = entry.getKey();
                FieldMappingConfig mappingConfig = entry.getValue();

                Object value = extractValueByPath(rootNode, mappingConfig.getPath());

                // Apply transform if specified
                if (value != null && mappingConfig.getTransform() != null) {
                    value = applyTransform(value, mappingConfig.getTransform());
                }

                // Use fallback if value is null
                if (value == null && mappingConfig.getFallback() != null) {
                    value = mappingConfig.getFallback();
                }

                mappedResult.put(targetField, value);
            }

            return mappedResult;
        } catch (Exception e) {
            log.warn("Failed to apply field mapping: {}", e.getMessage());
            return data;
        }
    }

    /**
     * Extract value from JSON using dot-notation path.
     */
    private Object extractValueByPath(JsonNode node, String path) {
        if (path == null || path.isEmpty()) {
            return objectMapper.convertValue(node, Object.class);
        }

        String[] parts = path.split("\\.");
        JsonNode current = node;

        for (String part : parts) {
            if (current == null || current.isMissingNode()) {
                return null;
            }

            if (part.contains("[")) {
                int bracketStart = part.indexOf('[');
                int bracketEnd = part.indexOf(']');
                String fieldName = part.substring(0, bracketStart);
                int index = Integer.parseInt(part.substring(bracketStart + 1, bracketEnd));

                current = current.get(fieldName);
                if (current != null && current.isArray()) {
                    current = current.get(index);
                }
            } else {
                current = current.get(part);
            }
        }

        if (current == null || current.isMissingNode()) {
            return null;
        }

        return objectMapper.convertValue(current, Object.class);
    }

    /**
     * Apply a transformation to a value.
     */
    private Object applyTransform(Object value, String transform) {
        if (value == null) return null;

        switch (transform.toLowerCase()) {
            case "uppercase":
                return value.toString().toUpperCase();
            case "lowercase":
                return value.toString().toLowerCase();
            case "trim":
                return value.toString().trim();
            case "number":
                return Double.parseDouble(value.toString());
            case "integer":
                return Integer.parseInt(value.toString());
            case "boolean":
                return Boolean.parseBoolean(value.toString());
            case "string":
                return value.toString();
            default:
                return value;
        }
    }

    /**
     * Create page metadata.
     */
    private PageMeta createPageMeta(PageDefinition pageDefinition) {
        PageMeta meta = new PageMeta();
        meta.setPageName(pageDefinition.getPageName());
        meta.setTitle(pageDefinition.getTitle());
        meta.setDescription(pageDefinition.getDescription());
        meta.setPath(pageDefinition.getPath());
        return meta;
    }

    /**
     * Clear cache.
     */
    public void clearCache() {
        cache.clear();
        log.info("Page data cache cleared");
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

    /**
     * Internal page definition representation.
     */
    public static class PageDefinition {
        private String pageName;
        private String title;
        private String description;
        private String path;
        private Map<String, DataSourceConfig> dataSources;

        public String getPageName() { return pageName; }
        public void setPageName(String pageName) { this.pageName = pageName; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public Map<String, DataSourceConfig> getDataSources() { return dataSources; }
        public void setDataSources(Map<String, DataSourceConfig> dataSources) { this.dataSources = dataSources; }
    }

    /**
     * Page metadata for responses.
     */
    public static class PageMeta {
        private String pageName;
        private String title;
        private String description;
        private String path;

        public String getPageName() { return pageName; }
        public void setPageName(String pageName) { this.pageName = pageName; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    /**
     * Page data response.
     */
    public static class PageData {
        private Map<String, Object> data;
        private Map<String, String> errors;
        private PageMeta pageMeta;
        private long fetchTimeMs;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private final PageData pageData = new PageData();

            public Builder data(Map<String, Object> data) {
                pageData.data = data;
                return this;
            }

            public Builder errors(Map<String, String> errors) {
                pageData.errors = errors;
                return this;
            }

            public Builder pageMeta(PageMeta pageMeta) {
                pageData.pageMeta = pageMeta;
                return this;
            }

            public Builder fetchTimeMs(long fetchTimeMs) {
                pageData.fetchTimeMs = fetchTimeMs;
                return this;
            }

            public PageData build() { return pageData; }
        }

        public Map<String, Object> getData() { return data; }
        public Map<String, String> getErrors() { return errors; }
        public PageMeta getPageMeta() { return pageMeta; }
        public long getFetchTimeMs() { return fetchTimeMs; }
    }
}
