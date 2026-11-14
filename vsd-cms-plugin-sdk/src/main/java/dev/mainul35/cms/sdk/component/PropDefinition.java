package dev.mainul35.cms.sdk.component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Definition of a configurable property for a UI component.
 * These properties are exposed in the visual builder's properties panel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropDefinition {

    /**
     * Property name (e.g., "text", "level", "alignment")
     */
    private String name;

    /**
     * Property type (string, number, boolean, select, color, etc.)
     */
    private PropType type;

    /**
     * Display label in properties panel
     */
    private String label;

    /**
     * Default value for the property
     */
    private Object defaultValue;

    /**
     * For select type: available options
     */
    private List<String> options;

    /**
     * For number type: minimum value
     */
    private Number min;

    /**
     * For number type: maximum value
     */
    private Number max;

    /**
     * For number type: step increment
     */
    private Number step;

    /**
     * Whether property is required
     */
    private boolean required;

    /**
     * Help text or description
     */
    private String helpText;

    /**
     * For advanced use: validation regex pattern
     */
    private String validationPattern;

    /**
     * Property type enumeration
     */
    public enum PropType {
        STRING,
        NUMBER,
        BOOLEAN,
        SELECT,
        COLOR,
        IMAGE,
        URL,
        TEXTAREA,
        JSON,
        ARRAY
    }
}
