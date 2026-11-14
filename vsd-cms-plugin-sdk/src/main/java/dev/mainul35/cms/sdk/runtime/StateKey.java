package dev.mainul35.cms.sdk.runtime;

import java.util.Objects;

/** Address of one state value: {@code scope / owner / key}. */
public record StateKey(StateScope scope, String owner, String key) {

    public static final String RESERVED_PREFIX = "$";

    public StateKey {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(key, "key");
    }

    public static StateKey session(String owner, String key) {
        return new StateKey(StateScope.SESSION, owner, key);
    }

    public static StateKey plugin(String pluginId, String key) {
        return new StateKey(StateScope.PLUGIN, pluginId, key);
    }

    public static StateKey page(String pageName, String key) {
        return new StateKey(StateScope.PAGE, pageName, key);
    }

    public static StateKey component(String instanceId, String key) {
        return new StateKey(StateScope.COMPONENT, instanceId, key);
    }

    /** True if this key belongs to the runtime and must not be written by clients. */
    public boolean isReserved() {
        return owner.startsWith(RESERVED_PREFIX);
    }

    /** Canonical string form, also used on the wire. */
    public String id() {
        return scope.name() + "/" + owner + "/" + key;
    }
}
