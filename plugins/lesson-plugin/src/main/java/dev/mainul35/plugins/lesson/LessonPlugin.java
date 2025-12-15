package dev.mainul35.plugins.lesson;

import dev.mainul35.cms.sdk.Plugin;
import dev.mainul35.cms.sdk.PluginContext;
import org.slf4j.Logger;

public class LessonPlugin implements Plugin {

    private static final String PLUGIN_ID = "lesson-plugin";
    private static final String VERSION = "1.0.0";
    private static final String NAME = "Lesson Management Plugin";
    private static final String DESCRIPTION = "Provides lesson and media management capabilities";

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
        logger.info("{} loaded successfully", NAME);
    }

    @Override
    public void onActivate(PluginContext context) throws Exception {
        this.logger = context.getLogger();
        logger.info("Activating {} v{}", NAME, VERSION);
        logger.info("{} activated successfully", NAME);
    }

    @Override
    public void onDeactivate(PluginContext context) throws Exception {
        this.logger = context.getLogger();
        logger.info("Deactivating {} v{}", NAME, VERSION);
        logger.info("{} deactivated successfully", NAME);
    }

    @Override
    public void onUninstall(PluginContext context) throws Exception {
        this.logger = context.getLogger();
        logger.info("Uninstalling {} v{}", NAME, VERSION);
        logger.info("{} uninstalled successfully", NAME);
    }
}
