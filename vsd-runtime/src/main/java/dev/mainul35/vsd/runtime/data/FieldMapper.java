package dev.mainul35.vsd.runtime.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Locale;
import java.util.Map;

/** Reshapes a fetched document: each target field is filled from a path in the source, with an optional transform. */
public final class FieldMapper {

    private static final JsonNodeFactory F = JsonNodeFactory.instance;

    private FieldMapper() {
    }

    public static JsonNode apply(JsonNode data, Map<String, SourceSpec.Mapping> mapping) {
        if (mapping == null || mapping.isEmpty() || data == null) {
            return data;
        }
        ObjectNode out = F.objectNode();
        for (var e : mapping.entrySet()) {
            SourceSpec.Mapping m = e.getValue();
            JsonNode v = JsonPaths.get(data, m.path());
            if (v != null && !v.isNull() && m.transform() != null) {
                v = transform(v, m.transform());
            }
            if ((v == null || v.isNull()) && m.fallback() != null) {
                v = m.fallback();
            }
            out.set(e.getKey(), v == null ? F.nullNode() : v);
        }
        return out;
    }

    /** A failed or unknown transform leaves the value unchanged. */
    static JsonNode transform(JsonNode v, String name) {
        String s = v.isValueNode() ? v.asText() : v.toString();
        try {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "uppercase" -> F.textNode(s.toUpperCase(Locale.ROOT));
                case "lowercase" -> F.textNode(s.toLowerCase(Locale.ROOT));
                case "trim" -> F.textNode(s.trim());
                case "string" -> F.textNode(s);
                case "number" -> F.numberNode(Double.parseDouble(s.trim()));
                case "integer" -> F.numberNode(Long.parseLong(s.trim()));
                case "boolean" -> F.booleanNode(Boolean.parseBoolean(s.trim()));
                default -> v;
            };
        } catch (NumberFormatException e) {
            return v;
        }
    }
}
