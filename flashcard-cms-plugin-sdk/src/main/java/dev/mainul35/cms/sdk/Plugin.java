package dev.mainul35.cms.sdk;

/**
 * Core interface that all plugins must implement.
 * Defines the lifecycle hooks and metadata for a plugin.
 */
public interface Plugin {

    /**
     * Get the unique plugin identifier (e.g., "course-plugin", "flashcard-plugin")
     * This must match the pluginId in plugin.yml manifest
     */
    String getPluginId();

    /**
     * Get the plugin version (e.g., "1.0.0")
     */
    String getVersion();

    /**
     * Get the plugin name for display purposes
     */
    String getName();

    /**
     * Get the plugin description
     */
    String getDescription();

    /**
     * Called when the plugin is loaded and initialized
     * Use this to set up plugin resources, register event listeners, etc.
     *
     * @param context Plugin context providing access to platform services
     * @throws Exception if initialization fails
     */
    void onLoad(PluginContext context) throws Exception;

    /**
     * Called when the plugin is activated
     * Use this to start services, register controllers, etc.
     *
     * @param context Plugin context
     * @throws Exception if activation fails
     */
    void onActivate(PluginContext context) throws Exception;

    /**
     * Called when the plugin is deactivated
     * Use this to stop services, unregister controllers, cleanup resources, etc.
     *
     * @param context Plugin context
     * @throws Exception if deactivation fails
     */
    void onDeactivate(PluginContext context) throws Exception;

    /**
     * Called when the plugin is uninstalled
     * Use this for final cleanup, data migration, etc.
     *
     * @param context Plugin context
     * @throws Exception if uninstallation fails
     */
    void onUninstall(PluginContext context) throws Exception;
}
