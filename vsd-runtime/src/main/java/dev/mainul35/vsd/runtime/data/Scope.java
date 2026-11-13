package dev.mainul35.vsd.runtime.data;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

/** Named values visible to a template, with inner scopes shadowing outer ones. */
public final class Scope {

    private final Scope parent;
    private final Map<String, JsonNode> vars;

    public Scope(Map<String, JsonNode> vars) {
        this(null, vars);
    }

    private Scope(Scope parent, Map<String, JsonNode> vars) {
        this.parent = parent;
        this.vars = vars;
    }

    /** A new inner scope with one more variable. */
    public Scope with(String name, JsonNode value) {
        Map<String, JsonNode> m = new HashMap<>();
        m.put(name, value);
        return new Scope(this, m);
    }

    public Scope with(Map<String, JsonNode> more) {
        return new Scope(this, new HashMap<>(more));
    }

    public boolean has(String root) {
        return vars.containsKey(root) || (parent != null && parent.has(root));
    }

    private JsonNode root(String name) {
        for (Scope s = this; s != null; s = s.parent) {
            if (s.vars.containsKey(name)) {
                return s.vars.get(name);
            }
        }
        return null;
    }

    /** The value at {@code path} (for example {@code auth.user.displayName}), or null if there is none. */
    public JsonNode lookup(String path) {
        String head = JsonPaths.firstSegment(path);
        JsonNode base = root(head);
        if (base == null) {
            return null;
        }
        JsonNode v = JsonPaths.get(base, JsonPaths.rest(path));
        return v == null || v.isMissingNode() ? null : v;
    }
}
