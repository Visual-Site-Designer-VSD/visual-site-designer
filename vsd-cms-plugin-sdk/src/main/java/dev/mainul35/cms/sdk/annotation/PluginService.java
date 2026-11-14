package dev.mainul35.cms.sdk.annotation;

import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * Marks a class as a plugin service.
 * This is a specialization of Spring's @Service annotation for plugin components.
 * Services marked with this annotation will be registered in the plugin's Spring context.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface PluginService {

    /**
     * The value may indicate a suggestion for a logical component name,
     * to be turned into a Spring bean in case of an autodetected component.
     */
    String value() default "";

    /**
     * The plugin ID this service belongs to
     * If not specified, will be auto-detected from the plugin context
     */
    String pluginId() default "";
}
