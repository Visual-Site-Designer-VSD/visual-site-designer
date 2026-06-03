package dev.mainul35.siteruntime.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Adds external static resource locations when site.runtime.data-dir is configured.
 * Serves plugin bundles from {dataDir}/static/plugins/{pluginId}/bundle.js.
 */
@Configuration
@ConditionalOnProperty(name = "site.runtime.data-dir")
public class ExternalResourceConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ExternalResourceConfig.class);

    private final SiteRuntimeProperties properties;

    public ExternalResourceConfig(SiteRuntimeProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dataDir = properties.getDataDir();
        if (dataDir == null || dataDir.isEmpty()) {
            return;
        }

        Path staticDir = Path.of(dataDir, "static").toAbsolutePath();
        String resourceLocation = "file:" + staticDir.toString().replace('\\', '/') + "/";

        log.info("Adding external static resource location: {}", resourceLocation);

        registry.addResourceHandler("/plugins/**")
                .addResourceLocations(resourceLocation + "plugins/");

        registry.addResourceHandler("/static/**")
                .addResourceLocations(resourceLocation);
    }
}
