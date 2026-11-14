package dev.mainul35.cms.sdk.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Outcome of a handler. State changes are not returned here; they go through
 * {@link RuntimeContext#state()}.
 */
public final class RuntimeResult {

    public enum Status { SUCCESS, FAILURE }

    private final Status status;
    private final String message;
    private final Map<String, Object> data;
    private final Map<String, String> errors;

    private RuntimeResult(Status status, String message, Map<String, Object> data, Map<String, String> errors) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.errors = errors;
    }

    public static RuntimeResult ok() {
        return new RuntimeResult(Status.SUCCESS, null, Map.of(), Map.of());
    }

    public static RuntimeResult ok(String message) {
        return new RuntimeResult(Status.SUCCESS, message, Map.of(), Map.of());
    }

    public static RuntimeResult ok(String message, Map<String, Object> data) {
        return new RuntimeResult(Status.SUCCESS, message, Map.copyOf(data), Map.of());
    }

    public static RuntimeResult failure(String message) {
        return new RuntimeResult(Status.FAILURE, message, Map.of(), Map.of());
    }

    public static RuntimeResult invalid(String message, Map<String, String> fieldErrors) {
        return new RuntimeResult(Status.FAILURE, message, Map.of(), new LinkedHashMap<>(fieldErrors));
    }

    public Status status() { return status; }
    public String message() { return message; }
    public Map<String, Object> data() { return data; }
    public Map<String, String> errors() { return errors; }
}
