package dev.mainul35.cms.sdk.component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Definition of a configurable CSS style property for a UI component.
 * These styles are exposed in the visual builder's style panel and CSS editor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StyleDefinition {

    /**
     * CSS property name (e.g., "fontSize", "backgroundColor", "padding")
     */
    private String property;

    /**
     * Style type for appropriate editor widget
     */
    private StyleType type;

    /**
     * Display label in style panel
     */
    private String label;

    /**
     * Default value for the style
     */
    private String defaultValue;

    /**
     * For select type: available options
     */
    private List<String> options;

    /**
     * For size type: allowed units (px, em, rem, %, vh, vw)
     */
    private List<String> allowedUnits;

    /**
     * Help text or description
     */
    private String helpText;

    /**
     * Category for grouping in style panel (e.g., "Typography", "Layout", "Colors")
     */
    private String category;

    /**
     * Style type enumeration
     */
    public enum StyleType {
        COLOR,           // Color picker
        SIZE,            // Size input with unit selector
        SPACING,         // Padding/margin with 4-side control
        SELECT,          // Dropdown select
        NUMBER,          // Number input
        BORDER,          // Border style editor
        SHADOW,          // Box shadow editor
        GRADIENT,        // Gradient editor
        FONT_FAMILY,     // Font family selector
        FONT_WEIGHT,     // Font weight selector
        TEXT_ALIGN,      // Text alignment
        DISPLAY,         // Display property
        POSITION,        // Position property
        FLEX,            // Flexbox properties
        GRID             // Grid properties
    }
}
