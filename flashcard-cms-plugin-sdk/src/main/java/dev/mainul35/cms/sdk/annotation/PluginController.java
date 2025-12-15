package dev.mainul35.cms.sdk.annotation;

import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.*;

/**
 * Marks a class as a plugin REST controller.
 * This is a specialization of Spring's @RestController annotation for plugin components.
 * Controllers marked with this annotation will be registered in the plugin's Spring context
 * and automatically mounted under /api/plugins/{pluginId}/
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RestController
public @interface PluginController {

    /**
     * The value may indicate a suggestion for a logical component name,
     * to be turned into a Spring bean in case of an autodetected component.
     */
    String value() default "";

    /**
     * The plugin ID this controller belongs to
     * If not specified, will be auto-detected from the plugin context
     */
    String pluginId() default "";

    /**
     * The base path for this controller's endpoints (relative to /api/plugins/{pluginId}/)
     * For example, if basePath = "courses", endpoints will be at /api/plugins/{pluginId}/courses
     */
    String basePath() default "";
}
