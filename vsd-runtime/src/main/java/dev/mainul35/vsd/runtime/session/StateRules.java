package dev.mainul35.vsd.runtime.session;

import dev.mainul35.cms.sdk.runtime.StateKey;
import dev.mainul35.cms.sdk.runtime.StateScope;
import dev.mainul35.vsd.runtime.util.Json;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Validation shared by client-originated and handler-originated state writes. */
public final class StateRules {

    /** Owners and keys: no separators, so the wire ids stay unambiguous. */
    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_.:$@-]{1,128}$");

    private StateRules() {
    }

    public static StateKey parse(String scope, String owner, String key, boolean allowReserved) {
        StateScope s;
        try {
            s = StateScope.valueOf(String.valueOf(scope).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown state scope");
        }
        return check(new StateKey(s, String.valueOf(owner), String.valueOf(key)), allowReserved);
    }

    public static StateKey check(StateKey key, boolean allowReserved) {
        if (!NAME.matcher(key.owner()).matches() || !NAME.matcher(key.key()).matches()) {
            throw new IllegalArgumentException("Invalid state owner or key");
        }
        if (!allowReserved && key.isReserved()) {
            throw new IllegalArgumentException("State owner is reserved for the runtime");
        }
        return key;
    }

    /** Converts a value to a plain JSON tree (Map/List/String/Number/Boolean/null) and enforces the size limit. */
    public static Object normalize(Object value, int maxBytes) {
        Object tree = value == null ? null : Json.MAPPER.convertValue(value, Object.class);
        int size = Json.write(tree).getBytes(StandardCharsets.UTF_8).length;
        if (size > maxBytes) {
            throw new IllegalArgumentException("State value too large");
        }
        return tree;
    }
}
