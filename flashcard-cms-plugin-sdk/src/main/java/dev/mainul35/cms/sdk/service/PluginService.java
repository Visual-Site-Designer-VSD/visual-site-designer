package dev.mainul35.cms.sdk.service;

import dev.mainul35.cms.sdk.PluginContext;
import lombok.Getter;
import org.slf4j.Logger;

/**
 * Base class that all plugin services should extend.
 * Provides access to plugin context and common service infrastructure.
 */
@Getter
public abstract class PluginService {

    /**
     * Plugin context providing access to platform services
     */
    protected PluginContext pluginContext;

    /**
     * Logger for this service
     * Plugins should use this for all logging
     */
    protected Logger logger;

    /**
     * The plugin ID this service belongs to
     */
    protected String pluginId;

    /**
     * Set the plugin context
     * This is called automatically by the plugin framework during initialization
     */
    public void setPluginContext(PluginContext context) {
        this.pluginContext = context;
        this.logger = context.getLogger();
        this.pluginId = context.getPluginId();
        onContextSet();
    }

    /**
     * Called after the plugin context is set
     * Override this to perform initialization that requires the plugin context
     */
    protected void onContextSet() {
        // Default implementation does nothing
        // Subclasses can override for custom initialization
    }

    /**
     * Get the plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Log an info message
     */
    protected void logInfo(String message, Object... args) {
        if (logger != null) {
            logger.info(message, args);
        }
    }

    /**
     * Log a debug message
     */
    protected void logDebug(String message, Object... args) {
        if (logger != null) {
            logger.debug(message, args);
        }
    }

    /**
     * Log a warning message
     */
    protected void logWarn(String message, Object... args) {
        if (logger != null) {
            logger.warn(message, args);
        }
    }

    /**
     * Log an error message
     */
    protected void logError(String message, Object... args) {
        if (logger != null) {
            logger.error(message, args);
        }
    }

    /**
     * Log an error message with exception
     */
    protected void logError(String message, Throwable throwable) {
        if (logger != null) {
            logger.error(message, throwable);
        }
    }
}
