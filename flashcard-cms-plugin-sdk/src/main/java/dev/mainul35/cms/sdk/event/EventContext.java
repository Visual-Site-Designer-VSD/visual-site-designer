package dev.mainul35.cms.sdk.event;

import java.util.Map;
import java.util.Optional;

/**
 * Context passed to event handlers containing all information about the event.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @EventHandler(componentId = "Button", eventType = "onClick")
 * public EventResult handleClick(EventContext context) {
 *     // Get component props
 *     String buttonText = context.getProp("text", String.class);
 *     String variant = context.getProp("variant", String.class);
 *
 *     // Get event data from frontend
 *     Integer clickCount = context.getEventData("clickCount", Integer.class);
 *
 *     // Access user/session info
 *     String userId = context.getUserId().orElse("anonymous");
 *
 *     return EventResult.success().build();
 * }
 * }</pre>
 */
public interface EventContext {

    // ==================== Component Information ====================

    /**
     * Get the unique instance ID of the component that triggered the event
     */
    String getInstanceId();

    /**
     * Get the component type ID (e.g., "Button", "Form")
     */
    String getComponentId();

    /**
     * Get the plugin ID that owns this component
     */
    String getPluginId();

    // ==================== Event Information ====================

    /**
     * Get the event type (e.g., "onClick", "onSubmit")
     */
    String getEventType();

    /**
     * Get the timestamp when the event was triggered (frontend time)
     */
    long getTimestamp();

    /**
     * Get the unique event ID for tracking/logging
     */
    String getEventId();

    // ==================== Component Props ====================

    /**
     * Get all component props as a map
     */
    Map<String, Object> getProps();

    /**
     * Get a specific prop value
     *
     * @param name The prop name
     * @param type The expected type class
     * @return The prop value, or null if not found
     */
    <T> T getProp(String name, Class<T> type);

    /**
     * Get a prop value with a default
     */
    <T> T getProp(String name, Class<T> type, T defaultValue);

    /**
     * Check if a prop exists
     */
    boolean hasProp(String name);

    // ==================== Component Styles ====================

    /**
     * Get all component styles as a map
     */
    Map<String, String> getStyles();

    /**
     * Get a specific style value
     */
    String getStyle(String name);

    // ==================== Event Data ====================

    /**
     * Get custom event data sent from the frontend
     * This contains any additional data the frontend passed with the event
     */
    Map<String, Object> getEventData();

    /**
     * Get a specific event data value
     */
    <T> T getEventData(String key, Class<T> type);

    /**
     * Get event data with a default value
     */
    <T> T getEventData(String key, Class<T> type, T defaultValue);

    // ==================== Page Context ====================

    /**
     * Get the page ID where the component is located
     */
    Optional<Long> getPageId();

    /**
     * Get the page name
     */
    Optional<String> getPageName();

    // ==================== User/Session Context ====================

    /**
     * Get the current user ID (if authenticated)
     */
    Optional<String> getUserId();

    /**
     * Get the current session ID
     */
    Optional<String> getSessionId();

    /**
     * Get a value from the user's session
     */
    <T> Optional<T> getSessionAttribute(String key, Class<T> type);

    /**
     * Check if the user is authenticated
     */
    boolean isAuthenticated();

    /**
     * Check if the user has a specific role
     */
    boolean hasRole(String role);

    // ==================== Request Context ====================

    /**
     * Get the source of the event (e.g., "builder-preview", "published-page", "mobile-app")
     */
    String getSource();

    /**
     * Get request headers (limited subset for security)
     */
    Map<String, String> getHeaders();

    /**
     * Get the client IP address
     */
    Optional<String> getClientIp();

    // ==================== Utility Methods ====================

    /**
     * Get a Spring bean from the application context
     * Allows handlers to access other services
     */
    <T> T getBean(Class<T> beanClass);

    /**
     * Get a Spring bean by name
     */
    <T> T getBean(String name, Class<T> beanClass);
}
