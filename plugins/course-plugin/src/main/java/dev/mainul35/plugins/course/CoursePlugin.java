package dev.mainul35.plugins.course;

import dev.mainul35.cms.sdk.Plugin;
import dev.mainul35.cms.sdk.PluginContext;
import org.slf4j.Logger;

/**
 * Course Plugin - Manages courses and modules
 * This is a bundled plugin that provides core course management functionality
 */
public class CoursePlugin implements Plugin {

    private static final String PLUGIN_ID = "course-plugin";
    private static final String VERSION = "1.0.0";
    private static final String NAME = "Course Management Plugin";
    private static final String DESCRIPTION = "Provides course and module management capabilities";

    private PluginContext context;
    private Logger logger;

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public void onLoad(PluginContext context) throws Exception {
        this.context = context;
        this.logger = context.getLogger();
        logger.info("Loading {} v{}", NAME, VERSION);

        // Initialize plugin-specific resources
        logger.info("Initializing course management resources");

        // Register entities with JPA if needed
        logger.debug("Course and Module entities will be registered");

        logger.info("{} loaded successfully", NAME);
    }

    @Override
    public void onActivate(PluginContext context) throws Exception {
        this.logger = context.getLogger();
        logger.info("Activating {} v{}", NAME, VERSION);

        // Register REST controllers
        logger.debug("Registering CourseController and ModuleController");

        // Start any background services if needed
        logger.info("{} activated successfully", NAME);
    }

    @Override
    public void onDeactivate(PluginContext context) throws Exception {
        this.logger = context.getLogger();
        logger.info("Deactivating {} v{}", NAME, VERSION);

        // Unregister controllers
        logger.debug("Unregistering controllers");

        // Stop any background services
        logger.info("{} deactivated successfully", NAME);
    }

    @Override
    public void onUninstall(PluginContext context) throws Exception {
        this.logger = context.getLogger();
        logger.info("Uninstalling {} v{}", NAME, VERSION);

        // Clean up resources
        logger.warn("Note: Course and module data will remain in database");
        logger.info("{} uninstalled successfully", NAME);
    }
}
