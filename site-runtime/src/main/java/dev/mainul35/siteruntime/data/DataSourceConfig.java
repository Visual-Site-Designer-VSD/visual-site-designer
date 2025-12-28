package dev.mainul35.siteruntime.data;

import java.util.Map;

/**
 * Configuration for a data source.
 */
public class DataSourceConfig {

    private DataSourceType type;
    private String endpoint;           // For API type
    private String method;             // GET or POST
    private Map<String, String> headers;
    private Map<String, FieldMappingConfig> fieldMapping;
    private String cacheKey;
    private Long cacheTtlMs;
    private Object staticData;         // For STATIC type
    private String contextKey;         // For CONTEXT type
    private String query;              // For DATABASE type

    // Getters and setters
    public DataSourceType getType() { return type; }
    public void setType(DataSourceType type) { this.type = type; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public Map<String, FieldMappingConfig> getFieldMapping() { return fieldMapping; }
    public void setFieldMapping(Map<String, FieldMappingConfig> fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }

    public Long getCacheTtlMs() { return cacheTtlMs; }
    public void setCacheTtlMs(Long cacheTtlMs) { this.cacheTtlMs = cacheTtlMs; }

    public Object getStaticData() { return staticData; }
    public void setStaticData(Object staticData) { this.staticData = staticData; }

    public String getContextKey() { return contextKey; }
    public void setContextKey(String contextKey) { this.contextKey = contextKey; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
}
