package dev.mainul35.vsd.runtime.data;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A data source as authored in the builder ({@code dataContext.dataSources} or a component's
 * {@code dataSource}). Parsed leniently: type names are case-insensitive and a field mapping entry may be
 * just a path string. Headers can carry secrets and never leave the server.
 */
public record SourceSpec(
        Type type,
        String endpoint,
        String method,
        Map<String, String> headers,
        Map<String, Mapping> fieldMapping,
        Long cacheTtlMs,
        boolean cached,
        JsonNode staticData,
        String contextKey) {

    public enum Type {API, STATIC, CONTEXT}

    public record Mapping(String path, String transform, JsonNode fallback) {
    }

    /** @throws IllegalArgumentException if the configuration is unusable */
    public static SourceSpec parse(JsonNode n) {
        if (n == null || !n.isObject()) {
            throw new IllegalArgumentException("data source must be an object");
        }
        Type type;
        try {
            type = Type.valueOf(n.path("type").asText("").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown data source type '" + n.path("type").asText() + "'");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        n.path("headers").fields().forEachRemaining(e -> headers.put(e.getKey(), e.getValue().asText()));
        Map<String, Mapping> mapping = null;
        if (n.path("fieldMapping").isObject() && n.path("fieldMapping").size() > 0) {
            mapping = new LinkedHashMap<>();
            for (var it = n.path("fieldMapping").fields(); it.hasNext(); ) {
                var e = it.next();
                JsonNode v = e.getValue();
                mapping.put(e.getKey(), v.isTextual()
                        ? new Mapping(v.asText(), null, null)
                        : new Mapping(v.path("path").asText(""), v.path("transform").asText(null),
                        v.has("fallback") ? v.get("fallback") : null));
            }
        }
        boolean cached = n.hasNonNull("cacheKey") || n.hasNonNull("cacheTtlMs");
        return new SourceSpec(type,
                n.path("endpoint").asText(null),
                n.path("method").asText("GET").toUpperCase(Locale.ROOT),
                headers, mapping,
                n.hasNonNull("cacheTtlMs") ? n.get("cacheTtlMs").asLong() : null,
                cached,
                n.get("staticData"),
                n.path("contextKey").asText(null));
    }

    /** Stable identity of what this source fetches, used as the cache key. */
    public String fingerprint() {
        return type + "|" + method + "|" + endpoint + "|" + headers;
    }
}
