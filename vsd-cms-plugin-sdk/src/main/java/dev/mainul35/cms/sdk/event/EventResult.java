package dev.mainul35.cms.sdk.event;

import java.util.HashMap;
import java.util.Map;

/**
 * Result returned from an event handler.
 *
 * The result is sent back to the frontend and can contain:
 * - Success/failure status
 * - Response data
 * - Commands for the frontend (navigate, showMessage, updateProps, etc.)
 * - Real-time events to broadcast via WebSocket
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Simple success
 * return EventResult.success().build();
 *
 * // Success with data
 * return EventResult.success()
 *     .withData("message", "Form submitted successfully")
 *     .withData("recordId", savedRecord.getId())
 *     .build();
 *
 * // Trigger frontend action
 * return EventResult.success()
 *     .navigate("/thank-you")
 *     .showMessage("Success!", MessageType.SUCCESS)
 *     .build();
 *
 * // Update component props
 * return EventResult.success()
 *     .updateProp("text", "Clicked!")
 *     .updateProp("disabled", true)
 *     .build();
 *
 * // Failure with error
 * return EventResult.failure("Validation failed")
 *     .withError("email", "Invalid email format")
 *     .build();
 *
 * // Broadcast real-time event
 * return EventResult.success()
 *     .broadcast("cartUpdated", Map.of("itemCount", 5))
 *     .build();
 * }</pre>
 */
public class EventResult {

    public enum Status {
        SUCCESS,
        FAILURE,
        PARTIAL  // Some operations succeeded, some failed
    }

    public enum MessageType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    private final Status status;
    private final String message;
    private final Map<String, Object> data;
    private final Map<String, String> errors;
    private final Map<String, Object> propUpdates;
    private final Map<String, String> styleUpdates;
    private final Map<String, Object> frontendCommands;
    private final Map<String, Object> broadcastEvents;

    private EventResult(Builder builder) {
        this.status = builder.status;
        this.message = builder.message;
        this.data = builder.data;
        this.errors = builder.errors;
        this.propUpdates = builder.propUpdates;
        this.styleUpdates = builder.styleUpdates;
        this.frontendCommands = builder.frontendCommands;
        this.broadcastEvents = builder.broadcastEvents;
    }

    // ==================== Factory Methods ====================

    /**
     * Create a success result builder
     */
    public static Builder success() {
        return new Builder(Status.SUCCESS);
    }

    /**
     * Create a success result builder with a message
     */
    public static Builder success(String message) {
        return new Builder(Status.SUCCESS).withMessage(message);
    }

    /**
     * Create a failure result builder
     */
    public static Builder failure(String message) {
        return new Builder(Status.FAILURE).withMessage(message);
    }

    /**
     * Create a partial success result builder
     */
    public static Builder partial(String message) {
        return new Builder(Status.PARTIAL).withMessage(message);
    }

    // ==================== Getters ====================

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public Map<String, Object> getPropUpdates() {
        return propUpdates;
    }

    public Map<String, String> getStyleUpdates() {
        return styleUpdates;
    }

    public Map<String, Object> getFrontendCommands() {
        return frontendCommands;
    }

    public Map<String, Object> getBroadcastEvents() {
        return broadcastEvents;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    // ==================== Builder ====================

    public static class Builder {
        private final Status status;
        private String message;
        private final Map<String, Object> data = new HashMap<>();
        private final Map<String, String> errors = new HashMap<>();
        private final Map<String, Object> propUpdates = new HashMap<>();
        private final Map<String, String> styleUpdates = new HashMap<>();
        private final Map<String, Object> frontendCommands = new HashMap<>();
        private final Map<String, Object> broadcastEvents = new HashMap<>();

        private Builder(Status status) {
            this.status = status;
        }

        /**
         * Set a result message
         */
        public Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        /**
         * Add response data
         */
        public Builder withData(String key, Object value) {
            this.data.put(key, value);
            return this;
        }

        /**
         * Add all response data
         */
        public Builder withData(Map<String, Object> data) {
            this.data.putAll(data);
            return this;
        }

        /**
         * Add a field-specific error
         */
        public Builder withError(String field, String errorMessage) {
            this.errors.put(field, errorMessage);
            return this;
        }

        // ==================== Component Updates ====================

        /**
         * Update a component prop (applied on frontend after handler completes)
         */
        public Builder updateProp(String propName, Object value) {
            this.propUpdates.put(propName, value);
            return this;
        }

        /**
         * Update multiple component props
         */
        public Builder updateProps(Map<String, Object> props) {
            this.propUpdates.putAll(props);
            return this;
        }

        /**
         * Update a component style
         */
        public Builder updateStyle(String styleName, String value) {
            this.styleUpdates.put(styleName, value);
            return this;
        }

        /**
         * Update multiple component styles
         */
        public Builder updateStyles(Map<String, String> styles) {
            this.styleUpdates.putAll(styles);
            return this;
        }

        // ==================== Frontend Commands ====================

        /**
         * Navigate to a URL/page
         */
        public Builder navigate(String url) {
            this.frontendCommands.put("navigate", url);
            return this;
        }

        /**
         * Navigate with options
         */
        public Builder navigate(String url, boolean newTab) {
            Map<String, Object> navOptions = new HashMap<>();
            navOptions.put("url", url);
            navOptions.put("newTab", newTab);
            this.frontendCommands.put("navigate", navOptions);
            return this;
        }

        /**
         * Show a message/toast on the frontend
         */
        public Builder showMessage(String message, MessageType type) {
            Map<String, Object> msgConfig = new HashMap<>();
            msgConfig.put("message", message);
            msgConfig.put("type", type.name().toLowerCase());
            this.frontendCommands.put("showMessage", msgConfig);
            return this;
        }

        /**
         * Show a success message
         */
        public Builder showSuccess(String message) {
            return showMessage(message, MessageType.SUCCESS);
        }

        /**
         * Show an error message
         */
        public Builder showError(String message) {
            return showMessage(message, MessageType.ERROR);
        }

        /**
         * Open a modal/dialog
         */
        public Builder openModal(String modalId) {
            this.frontendCommands.put("openModal", modalId);
            return this;
        }

        /**
         * Close a modal/dialog
         */
        public Builder closeModal(String modalId) {
            this.frontendCommands.put("closeModal", modalId);
            return this;
        }

        /**
         * Close all modals
         */
        public Builder closeAllModals() {
            this.frontendCommands.put("closeModal", "*");
            return this;
        }

        /**
         * Refresh data on a component
         */
        public Builder refreshComponent(String instanceId) {
            this.frontendCommands.put("refreshComponent", instanceId);
            return this;
        }

        /**
         * Add a custom frontend command
         */
        public Builder addCommand(String command, Object config) {
            this.frontendCommands.put(command, config);
            return this;
        }

        // ==================== Real-time Broadcast ====================

        /**
         * Broadcast an event to all connected clients via WebSocket
         */
        public Builder broadcast(String eventName, Map<String, Object> eventData) {
            this.broadcastEvents.put(eventName, eventData);
            return this;
        }

        /**
         * Broadcast to specific users
         */
        public Builder broadcastToUsers(String eventName, Map<String, Object> eventData, String... userIds) {
            Map<String, Object> broadcastConfig = new HashMap<>();
            broadcastConfig.put("data", eventData);
            broadcastConfig.put("targetUsers", userIds);
            this.broadcastEvents.put(eventName, broadcastConfig);
            return this;
        }

        /**
         * Broadcast to a specific session
         */
        public Builder broadcastToSession(String eventName, Map<String, Object> eventData, String sessionId) {
            Map<String, Object> broadcastConfig = new HashMap<>();
            broadcastConfig.put("data", eventData);
            broadcastConfig.put("targetSession", sessionId);
            this.broadcastEvents.put(eventName, broadcastConfig);
            return this;
        }

        /**
         * Build the EventResult
         */
        public EventResult build() {
            return new EventResult(this);
        }
    }
}
