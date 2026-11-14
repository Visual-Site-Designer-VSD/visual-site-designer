package dev.mainul35.vsd.runtime.shared;

import dev.mainul35.cms.sdk.runtime.RuntimeUser;
import dev.mainul35.cms.sdk.runtime.StateKey;
import dev.mainul35.cms.sdk.runtime.StatePatch;
import dev.mainul35.cms.sdk.runtime.StateScope;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import dev.mainul35.vsd.runtime.session.Session;
import dev.mainul35.vsd.runtime.util.Json;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies the declarations: checks every write to shared state, gives new sessions their starting
 * values and decides what happens to each state when the visitor signs in or out.
 *
 * <p>Shared state lives at {@code SESSION/shared/<name>}. Because it is the same session store
 * everything else uses, changes reach every plugin and tab through the normal state stream.
 */
@Service
public class SharedStateService {

    public static final String OWNER = "shared";

    private final SharedStateRegistry registry;
    private final RuntimeProperties props;

    public SharedStateService(SharedStateRegistry registry, RuntimeProperties props) {
        this.registry = registry;
        this.props = props;
    }

    public static StateKey key(String name) {
        return StateKey.session(OWNER, name);
    }

    // ---- checking writes ----------------------------------------------------------------------

    /**
     * Throws IllegalArgumentException if this write is not allowed.
     *
     * @param value      the new value as a plain JSON tree (ignored when {@code remove})
     * @param writer     the plugin making the write (from the event, or claimed by the browser); may be null
     * @param fromClient true for writes sent by browser code, false for server-side handlers
     */
    public void checkWrite(Session session, StateKey key, Object value, boolean remove, String writer, boolean fromClient) {
        if (key.isReserved()) {
            return; // runtime-owned: the callers already limit these to server code
        }
        boolean shared = OWNER.equals(key.owner());
        if (key.scope() == StateScope.SESSION && !shared) {
            if (props.isStrictSharedState()) {
                throw new IllegalArgumentException("Session state must be declared as shared state in the runtime console"
                        + " (use PLUGIN scope for state only your plugin needs)");
            }
            return;
        }
        if (!shared) {
            return;
        }
        if (key.scope() != StateScope.SESSION) {
            throw new IllegalArgumentException("Shared state lives in SESSION scope");
        }
        SharedStateDef def = registry.get(key.key()).orElseThrow(() -> new IllegalArgumentException(
                "Shared state '" + key.key() + "' is not declared. Declare it in the runtime console first."));
        String n = "Shared state '" + def.getName() + "'";
        if (remove) {
            throw new IllegalArgumentException(n + " cannot be removed; write its default value instead");
        }
        if (def.getLifetime() == SharedStateDef.Lifetime.LOGIN && session.user().isEmpty()) {
            throw new IllegalArgumentException(n + " can only be changed while signed in");
        }
        if (fromClient && !def.isClientWritable()) {
            throw new IllegalArgumentException(n + " can only be changed by server-side handlers");
        }
        if (!def.getWriters().isEmpty() && (writer == null || !def.getWriters().contains(writer))) {
            throw new IllegalArgumentException("Plugin '" + writer + "' may not write " + n);
        }
        SharedStateValidator.validateValue(def, value);
    }

    // ---- starting values and lifetimes --------------------------------------------------------

    /** Gives a new session the starting value of every visitor-lifetime state. */
    public List<StatePatch> seed(Session session) {
        List<StatePatch> sets = new ArrayList<>();
        for (SharedStateDef d : registry.all()) {
            if (d.getLifetime() == SharedStateDef.Lifetime.VISITOR && session.get(key(d.getName())).isEmpty()) {
                sets.add(StatePatch.set(key(d.getName()), copy(d.getDefaultValue())));
            }
        }
        return session.apply(sets);
    }

    /**
     * Called after the visitor signed in. Login-lifetime state always starts fresh. Visitor state is kept
     * unless it says {@code onSignIn: RESET}, or the account changed without a sign-out in between.
     */
    public List<StatePatch> onSignIn(Session session, boolean accountChanged) {
        List<StatePatch> sets = new ArrayList<>();
        for (SharedStateDef d : registry.all()) {
            boolean reset = d.getLifetime() == SharedStateDef.Lifetime.LOGIN
                    || d.getOnSignIn() == SharedStateDef.OnSignIn.RESET
                    || (accountChanged && d.getOnSignOut() == SharedStateDef.OnSignOut.CLEAR);
            if (reset || session.get(key(d.getName())).isEmpty() && d.getLifetime() == SharedStateDef.Lifetime.VISITOR) {
                sets.add(StatePatch.set(key(d.getName()), copy(d.getDefaultValue())));
            }
        }
        return session.apply(sets);
    }

    /**
     * Called at sign-out: removes all session state, then puts back the visitor-lifetime state that says
     * {@code onSignOut: KEEP} and the starting values of the rest. Login-lifetime state stays gone.
     */
    public List<StatePatch> onSignOut(Session session) {
        Map<StateKey, Object> kept = new LinkedHashMap<>();
        for (SharedStateDef d : registry.all()) {
            if (d.getLifetime() == SharedStateDef.Lifetime.VISITOR && d.getOnSignOut() == SharedStateDef.OnSignOut.KEEP) {
                session.get(key(d.getName())).ifPresent(v -> kept.put(key(d.getName()), v));
            }
        }
        List<StatePatch> all = new ArrayList<>(session.clearAll());
        List<StatePatch> sets = new ArrayList<>();
        for (SharedStateDef d : registry.all()) {
            if (d.getLifetime() != SharedStateDef.Lifetime.VISITOR) {
                continue;
            }
            Object v = kept.containsKey(key(d.getName())) ? kept.get(key(d.getName())) : copy(d.getDefaultValue());
            sets.add(StatePatch.set(key(d.getName()), v));
        }
        all.addAll(session.apply(sets));
        return all;
    }

    // ---- for the browser ----------------------------------------------------------------------

    /**
     * This visitor's value of every declared shared state, for template bindings. Login-lifetime state that
     * does not exist yet (signed out) shows its starting value, the same as the browser does.
     */
    public Map<String, Object> values(Session session) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (SharedStateDef d : registry.all()) {
            out.put(d.getName(), session.get(key(d.getName())).orElse(d.getDefaultValue()));
        }
        return out;
    }

    /** What the browser needs to know: names, lifetime and starting values (it falls back to these). */
    public List<Map<String, Object>> publicView() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SharedStateDef d : registry.all()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", d.getName());
            m.put("kind", d.getKind().name());
            m.put("lifetime", d.getLifetime().name());
            m.put("default", d.getDefaultValue());
            m.put("clientWritable", d.isClientWritable());
            m.put("writers", d.getWriters());
            out.add(m);
        }
        return out;
    }

    private static Object copy(Object tree) {
        return Json.MAPPER.convertValue(tree, Object.class);
    }
}
