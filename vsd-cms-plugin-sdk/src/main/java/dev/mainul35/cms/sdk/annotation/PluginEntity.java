package dev.mainul35.cms.sdk.annotation;

import java.lang.annotation.*;

/**
 * Marks a class as a plugin entity.
 * All plugin entities must extend the PluginEntity base class and use this annotation.
 * This annotation helps with entity discovery and validation during plugin installation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginEntity {

    /**
     * The plugin ID this entity belongs to
     * If not specified, will be auto-detected from the plugin context
     */
    String pluginId() default "";

    /**
     * Optional description of what this entity represents
     */
    String description() default "";
}
