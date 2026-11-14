package dev.mainul35.cms.sdk.annotation;

import java.lang.annotation.*;

/**
 * Marks a class as a UI component plugin.
 * UI component plugins provide React components that can be used in the visual site builder.
 *
 * Example usage:
 * <pre>
 * {@code
 * @UIComponent(
 *     componentId = "header",
 *     displayName = "Header",
 *     category = "ui",
 *     icon = "header-icon",
 *     resizable = true,
 *     defaultWidth = "100%",
 *     defaultHeight = "auto"
 * )
 * public class HeaderComponentPlugin implements UIComponentPlugin {
 *     // Implementation
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UIComponent {

    /**
     * Unique component identifier (e.g., "header", "button", "grid")
     * This must be unique within the plugin.
     */
    String componentId();

    /**
     * Display name shown in the component palette (e.g., "Header", "Button", "Grid Layout")
     */
    String displayName();

    /**
     * Component category for organization in the palette
     * Supported values: "ui", "layout", "form", "widget"
     */
    String category();

    /**
     * Icon identifier for the component palette
     * Can be a font-awesome icon name, material icon, or custom icon path
     */
    String icon() default "";

    /**
     * Whether the component can be resized in the builder
     * Default: true
     */
    boolean resizable() default true;

    /**
     * Default width when component is first added to canvas
     * Can be pixel value (e.g., "300px"), percentage (e.g., "50%"), or "auto"
     * Default: "auto"
     */
    String defaultWidth() default "auto";

    /**
     * Default height when component is first added to canvas
     * Can be pixel value (e.g., "200px"), percentage (e.g., "100%"), or "auto"
     * Default: "auto"
     */
    String defaultHeight() default "auto";

    /**
     * Minimum width constraint (optional)
     * Default: "0px"
     */
    String minWidth() default "0px";

    /**
     * Maximum width constraint (optional)
     * Default: "100%"
     */
    String maxWidth() default "100%";

    /**
     * Minimum height constraint (optional)
     * Default: "0px"
     */
    String minHeight() default "0px";

    /**
     * Maximum height constraint (optional)
     * Default: "none"
     */
    String maxHeight() default "none";

    /**
     * Optional description of the component
     */
    String description() default "";
}
