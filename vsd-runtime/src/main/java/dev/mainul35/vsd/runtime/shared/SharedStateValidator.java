package dev.mainul35.vsd.runtime.shared;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks definitions when they are saved and values every time they are written. Messages name the
 * state and the field so a plugin author can see at once what was wrong.
 */
public final class SharedStateValidator {

    public static final Pattern NAME = Pattern.compile("^[a-z][A-Za-z0-9_-]{0,63}$");
    private static final Pattern FIELD = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0,63}$");
    private static final Pattern MAP_KEY = Pattern.compile("^[A-Za-z0-9_.:@-]{1,128}$");
    private static final Pattern PLUGIN_ID = Pattern.compile("^[A-Za-z0-9_.:-]{1,128}$");
    private static final Set<String> TYPES = Set.of("string", "number", "integer", "boolean", "object", "array", "any");
    private static final int MAX_FIELDS = 50;
    private static final int MAX_WRITERS = 50;

    private SharedStateValidator() {
    }

    // ---- definitions ------------------------------------------------------------------------

    /** Throws IllegalArgumentException if the definition is not usable. Fills in the default value if missing. */
    public static void validateDefinition(SharedStateDef d) {
        if (d.getName() == null || !NAME.matcher(d.getName()).matches()) {
            throw new IllegalArgumentException("Name must start with a lowercase letter and use letters, digits, - or _ (max 64)");
        }
        String n = "Shared state '" + d.getName() + "'";
        if (d.getKind() == null || d.getLifetime() == null || d.getOnSignIn() == null || d.getOnSignOut() == null) {
            throw new IllegalArgumentException(n + ": kind, lifetime, onSignIn and onSignOut must be set");
        }
        if (d.getDescription().length() > 500) {
            throw new IllegalArgumentException(n + ": description is too long (max 500)");
        }
        if (d.getKind() == SharedStateDef.Kind.OBJECT) {
            if (d.getFields().isEmpty() || d.getFields().size() > MAX_FIELDS) {
                throw new IllegalArgumentException(n + ": an object needs 1 to " + MAX_FIELDS + " fields");
            }
            for (var e : d.getFields().entrySet()) {
                SharedStateDef.Field f = e.getValue();
                if (!FIELD.matcher(e.getKey()).matches()) {
                    throw new IllegalArgumentException(n + ": invalid field name '" + e.getKey() + "'");
                }
                if (f == null || !TYPES.contains(f.getType())) {
                    throw new IllegalArgumentException(n + ": field '" + e.getKey() + "' has an unknown type");
                }
                if (f.getMaxLength() < 1 || f.getMaxLength() > 100_000) {
                    throw new IllegalArgumentException(n + ": field '" + e.getKey() + "' maxLength must be 1 to 100000");
                }
            }
        } else {
            if (!TYPES.contains(d.getValueType())) {
                throw new IllegalArgumentException(n + ": unknown valueType");
            }
            if (d.getMaxEntries() < 1 || d.getMaxEntries() > 1000) {
                throw new IllegalArgumentException(n + ": maxEntries must be 1 to 1000");
            }
        }
        if (d.getWriters().size() > MAX_WRITERS) {
            throw new IllegalArgumentException(n + ": too many writers");
        }
        for (String w : d.getWriters()) {
            if (w == null || !PLUGIN_ID.matcher(w).matches()) {
                throw new IllegalArgumentException(n + ": invalid plugin id in writers");
            }
        }
        if (d.getDefaultValue() == null) {
            d.setDefaultValue(zeroValue(d));
        }
        validateValue(d, d.getDefaultValue());
    }

    /** Natural starting value: empty strings, zeros, false, empty collections; an empty map for a MAP. */
    static Object zeroValue(SharedStateDef d) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (d.getKind() == SharedStateDef.Kind.OBJECT) {
            d.getFields().forEach((k, f) -> out.put(k, switch (f.getType()) {
                case "string" -> "";
                case "number", "integer" -> 0;
                case "boolean" -> false;
                case "object" -> new LinkedHashMap<String, Object>();
                case "array" -> new ArrayList<Object>();
                default -> null;
            }));
        }
        return out;
    }

    // ---- values -----------------------------------------------------------------------------

    /** Throws IllegalArgumentException if {@code value} (a plain JSON tree) does not fit the definition. */
    public static void validateValue(SharedStateDef d, Object value) {
        String n = "Shared state '" + d.getName() + "'";
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(n + " must be a JSON object");
        }
        if (d.getKind() == SharedStateDef.Kind.OBJECT) {
            for (Object k : map.keySet()) {
                if (!d.getFields().containsKey(String.valueOf(k))) {
                    throw new IllegalArgumentException(n + ": unknown field '" + k + "'");
                }
            }
            for (var e : d.getFields().entrySet()) {
                Object v = map.get(e.getKey());
                if (v == null) {
                    if (e.getValue().isRequired()) {
                        throw new IllegalArgumentException(n + ": field '" + e.getKey() + "' is required");
                    }
                    continue;
                }
                checkType(n + ": field '" + e.getKey() + "'", e.getValue().getType(), e.getValue().getMaxLength(), v);
            }
        } else {
            if (map.size() > d.getMaxEntries()) {
                throw new IllegalArgumentException(n + " holds at most " + d.getMaxEntries() + " entries");
            }
            for (var e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                if (!MAP_KEY.matcher(key).matches()) {
                    throw new IllegalArgumentException(n + ": invalid key '" + key + "'");
                }
                if (e.getValue() == null) {
                    throw new IllegalArgumentException(n + ": key '" + key + "' has no value");
                }
                checkType(n + ": key '" + key + "'", d.getValueType(), 10_000, e.getValue());
            }
        }
    }

    private static void checkType(String where, String type, int maxLength, Object v) {
        boolean ok = switch (type) {
            case "string" -> v instanceof String;
            case "number" -> v instanceof Number;
            case "integer" -> isInteger(v);
            case "boolean" -> v instanceof Boolean;
            case "object" -> v instanceof Map;
            case "array" -> v instanceof List;
            default -> true;
        };
        if (!ok) {
            throw new IllegalArgumentException(where + " must be " + (type.equals("object") || type.equals("array") ? "an " : "a ") + type);
        }
        if (v instanceof String s && s.length() > maxLength) {
            throw new IllegalArgumentException(where + " is longer than " + maxLength + " characters");
        }
    }

    private static boolean isInteger(Object v) {
        if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte || v instanceof BigInteger) {
            return true;
        }
        if (v instanceof Double d) {
            return !d.isNaN() && !d.isInfinite() && d == Math.rint(d);
        }
        return v instanceof BigDecimal b && b.stripTrailingZeros().scale() <= 0;
    }
}
