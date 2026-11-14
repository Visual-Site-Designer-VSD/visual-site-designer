package dev.mainul35.vsd.runtime.shared;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A declared piece of shared state, created in the runtime console. Plugins can only use state
 * that is declared here; the runtime checks every write against this definition.
 *
 * <p>A value is either an {@code OBJECT} (named, typed fields) or a {@code MAP} (any number of keys,
 * all values of one type). It lives in the visitor's session at {@code SESSION/shared/<name>}.
 */
public class SharedStateDef {

    public enum Kind { OBJECT, MAP }

    /** VISITOR: lives as long as the visitor's session. LOGIN: exists only while signed in. */
    public enum Lifetime { VISITOR, LOGIN }

    public enum OnSignIn { KEEP, RESET }

    public enum OnSignOut { CLEAR, KEEP }

    /** One field of an OBJECT. Types: string, number, integer, boolean, object, array, any. */
    public static class Field {
        private String type = "any";
        private boolean required = false;
        private int maxLength = 1000;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public int getMaxLength() { return maxLength; }
        public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
    }

    private String name;
    private String description = "";
    private Kind kind = Kind.OBJECT;
    private Lifetime lifetime = Lifetime.VISITOR;
    /** OBJECT: the fields. */
    private Map<String, Field> fields = new LinkedHashMap<>();
    /** MAP: type of every value. */
    private String valueType = "any";
    /** MAP: most keys allowed. */
    private int maxEntries = 100;
    /** Value a new session starts with. Left out, it is built from the field types (or an empty map). */
    private Object defaultValue;
    /** Plugins allowed to write. Empty means any plugin. */
    private List<String> writers = new ArrayList<>();
    /** False: only server-side handlers can change it, browser code cannot. */
    private boolean clientWritable = true;
    private OnSignIn onSignIn = OnSignIn.KEEP;
    private OnSignOut onSignOut = OnSignOut.CLEAR;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }
    public Lifetime getLifetime() { return lifetime; }
    public void setLifetime(Lifetime lifetime) { this.lifetime = lifetime; }
    public Map<String, Field> getFields() { return fields; }
    public void setFields(Map<String, Field> fields) { this.fields = fields == null ? new LinkedHashMap<>() : fields; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public int getMaxEntries() { return maxEntries; }
    public void setMaxEntries(int maxEntries) { this.maxEntries = maxEntries; }
    public Object getDefaultValue() { return defaultValue; }
    public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
    public List<String> getWriters() { return writers; }
    public void setWriters(List<String> writers) { this.writers = writers == null ? new ArrayList<>() : writers; }
    public boolean isClientWritable() { return clientWritable; }
    public void setClientWritable(boolean clientWritable) { this.clientWritable = clientWritable; }
    public OnSignIn getOnSignIn() { return onSignIn; }
    public void setOnSignIn(OnSignIn onSignIn) { this.onSignIn = onSignIn; }
    public OnSignOut getOnSignOut() { return onSignOut; }
    public void setOnSignOut(OnSignOut onSignOut) { this.onSignOut = onSignOut; }
}
