package dev.mainul35.cms.sdk;

import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;

import java.nio.file.Path;

/**
 * Context object provided to plugins, giving access to platform services and resources.
 * This is the main interface between a plugin and the CMS platform.
 */
public interface PluginContext {

    /**
     * Get the unique plugin ID
     */
    String getPluginId();

    /**
     * Get the plugin's data directory
     * Plugins should store their data files in this directory
     */
    Path getDataDirectory();

    /**
     * Get the plugin's configuration directory
     * Plugins should store their configuration files in this directory
     */
    Path getConfigDirectory();

    /**
     * Get the plugin's logger
     * All plugin logging should use this logger for proper log management
     */
    Logger getLogger();

    /**
     * Get the plugin's Spring ApplicationContext
     * Use this to access Spring beans within the plugin
     */
    ApplicationContext getApplicationContext();

    /**
     * Get the main platform ApplicationContext
     * Use this to access shared platform services
     */
    ApplicationContext getPlatformContext();

    /**
     * Get a configuration value from plugin configuration
     *
     * @param key Configuration key
     * @return Configuration value, or null if not found
     */
    String getConfig(String key);

    /**
     * Get a configuration value with a default
     *
     * @param key Configuration key
     * @param defaultValue Default value if key not found
     * @return Configuration value or default
     */
    String getConfig(String key, String defaultValue);

    /**
     * Set a configuration value
     * This persists the configuration for the plugin
     *
     * @param key Configuration key
     * @param value Configuration value
     */
    void setConfig(String key, String value);

    /**
     * Get the ClassLoader for this plugin
     * Use this to load resources from the plugin JAR
     */
    ClassLoader getPluginClassLoader();

    /**
     * Check if the plugin is currently active
     */
    boolean isActive();

    /**
     * Get plugin version
     */
    String getVersion();
}
