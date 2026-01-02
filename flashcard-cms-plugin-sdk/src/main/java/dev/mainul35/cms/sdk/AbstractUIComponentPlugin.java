package dev.mainul35.cms.sdk;

import dev.mainul35.cms.sdk.annotation.UIComponent;
import dev.mainul35.cms.sdk.component.ComponentManifest;
import dev.mainul35.cms.sdk.component.PropDefinition;
import dev.mainul35.cms.sdk.component.SizeConstraints;
import dev.mainul35.cms.sdk.component.StyleDefinition;
import dev.mainul35.cms.sdk.component.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Abstract base class for UI component plugins.
 * Reduces boilerplate by automatically reading metadata from @UIComponent annotation.
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * @UIComponent(
 *     componentId = "HorizontalRow",
 *     displayName = "Horizontal Row",
 *     category = "layout",
 *     icon = "━",
 *     description = "A horizontal divider line"
 * )
 * public class HorizontalRowPlugin extends AbstractUIComponentPlugin {
 *
 *     @Override
 *     protected List<PropDefinition> defineProps() {
 *         return List.of(
 *             selectProp("thickness", "1px", List.of("1px", "2px", "3px", "5px")),
 *             selectProp("lineStyle", "solid", List.of("solid", "dashed", "dotted"))
 *         );
 *     }
 *
 *     @Override
 *     protected List<StyleDefinition> defineStyles() {
 *         return List.of(
 *             colorStyle("color", "#cccccc"),
 *             sizeStyle("marginTop", "16px"),
 *             sizeStyle("marginBottom", "16px")
 *         );
 *     }
 * }
 * }
 * </pre>
 */
public abstract class AbstractUIComponentPlugin implements UIComponentPlugin {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected PluginContext context;
    private ComponentManifest manifest;
    private UIComponent annotation;

    /**
     * Get the @UIComponent annotation, throwing if not present
     */
    private UIComponent getAnnotation() {
        if (annotation == null) {
            annotation = getClass().getAnnotation(UIComponent.class);
            if (annotation == null) {
                throw new IllegalStateException(
                    "Class " + getClass().getName() + " must be annotated with @UIComponent"
                );
            }
        }
        return annotation;
    }

    // ==================== Plugin Interface Methods (Auto-implemented) ====================

    @Override
    public String getPluginId() {
        // Convert componentId to plugin ID format: "Label" -> "label-component-plugin"
        String componentId = getAnnotation().componentId();
        return toKebabCase(componentId) + "-plugin";
    }

    @Override
    public String getName() {
        return getAnnotation().displayName();
    }

    @Override
    public String getVersion() {
        return "1.0.0"; // Override if different version needed
    }

    @Override
    public String getDescription() {
        String desc = getAnnotation().description();
        return desc.isEmpty() ? getName() + " component" : desc;
    }

    // ==================== Lifecycle Methods (Default implementations) ====================

    @Override
    public final void onLoad(PluginContext context) throws Exception {
        this.context = context;
        log.info("[{}] Loading plugin v{}", getPluginId(), getVersion());

        // Build component manifest
        this.manifest = buildManifest();
        log.debug("[{}] Component manifest built: {}", getPluginId(), manifest.getComponentId());

        // Call subclass initialization hook
        doOnLoad();

        log.info("[{}] Plugin loaded successfully", getPluginId());
    }

    @Override
    public final void onActivate(PluginContext context) throws Exception {
        log.info("[{}] Activating plugin", getPluginId());

        // Call subclass activation hook
        doOnActivate();

        log.info("[{}] Plugin activated - component '{}' is ready", getPluginId(), manifest.getComponentId());
    }

    @Override
    public final void onDeactivate(PluginContext context) throws Exception {
        log.info("[{}] Deactivating plugin", getPluginId());

        // Call subclass deactivation hook
        doOnDeactivate();

        log.info("[{}] Plugin deactivated", getPluginId());
    }

    @Override
    public final void onUninstall(PluginContext context) throws Exception {
        log.info("[{}] Uninstalling plugin", getPluginId());

        // Call subclass uninstall hook
        doOnUninstall();

        // Clear internal state
        this.manifest = null;
        this.context = null;

        log.info("[{}] Plugin uninstalled", getPluginId());
    }

    // ==================== Lifecycle Hooks (Override for custom behavior) ====================

    /**
     * Called during plugin loading, after manifest is built.
     * Override to perform custom initialization (e.g., load configuration, initialize caches).
     */
    protected void doOnLoad() throws Exception {
        // Default: no-op. Override in subclass if needed.
    }

    /**
     * Called when plugin is activated.
     * Override to start services, schedule tasks, or register event listeners.
     */
    protected void doOnActivate() throws Exception {
        // Default: no-op. Override in subclass if needed.
    }

    /**
     * Called when plugin is deactivated.
     * Override to stop services, cancel scheduled tasks, or release resources.
     */
    protected void doOnDeactivate() throws Exception {
        // Default: no-op. Override in subclass if needed.
    }

    /**
     * Called when plugin is uninstalled.
     * Override to perform cleanup (e.g., delete plugin-specific data, export data for backup).
     */
    protected void doOnUninstall() throws Exception {
        // Default: no-op. Override in subclass if needed.
    }

    // ==================== UIComponentPlugin Methods ====================

    @Override
    public ComponentManifest getComponentManifest() {
        if (manifest == null) {
            manifest = buildManifest();
        }
        return manifest;
    }

    @Override
    public String getReactComponentPath() {
        return "/components/" + getAnnotation().componentId() + ".jsx";
    }

    @Override
    public byte[] getComponentThumbnail() {
        return null; // Override if thumbnail needed
    }

    @Override
    public ValidationResult validateProps(Map<String, Object> props) {
        // Default: no validation errors
        // Override for custom validation
        return ValidationResult.success();
    }

    // ==================== Abstract Methods (Must implement) ====================

    /**
     * Define the configurable properties for this component.
     * Use the helper methods: prop(), selectProp(), booleanProp(), numberProp()
     *
     * @return List of property definitions
     */
    protected abstract List<PropDefinition> defineProps();

    /**
     * Define the configurable styles for this component.
     * Use the helper methods: style(), colorStyle(), sizeStyle()
     *
     * @return List of style definitions
     */
    protected abstract List<StyleDefinition> defineStyles();

    // ==================== Optional Overrides ====================

    /**
     * Override to provide default property values.
     * By default, extracts defaults from defineProps().
     */
    protected Map<String, Object> getDefaultProps() {
        Map<String, Object> defaults = new HashMap<>();
        for (PropDefinition prop : defineProps()) {
            if (prop.getDefaultValue() != null) {
                defaults.put(prop.getName(), prop.getDefaultValue());
            }
        }
        return defaults;
    }

    /**
     * Override to provide default style values.
     * By default, extracts defaults from defineStyles().
     */
    protected Map<String, String> getDefaultStyles() {
        Map<String, String> defaults = new HashMap<>();
        for (StyleDefinition style : defineStyles()) {
            if (style.getDefaultValue() != null) {
                defaults.put(style.getProperty(), style.getDefaultValue());
            }
        }
        return defaults;
    }

    /**
     * Override to specify if component can have children.
     * Default: false
     */
    protected boolean canHaveChildren() {
        return false;
    }

    /**
     * Override to specify allowed child component types.
     * Default: empty (all types allowed if canHaveChildren is true)
     */
    protected List<String> getAllowedChildTypes() {
        return Collections.emptyList();
    }

    // ==================== Helper Methods for Defining Props ====================

    /**
     * Create a string property definition
     */
    protected PropDefinition prop(String name, String label, String defaultValue) {
        return PropDefinition.builder()
                .name(name)
                .type(PropDefinition.PropType.STRING)
                .label(label)
                .defaultValue(defaultValue)
                .build();
    }

    /**
     * Create a property with specific type
     */
    protected PropDefinition prop(String name, PropDefinition.PropType type, Object defaultValue) {
        return PropDefinition.builder()
                .name(name)
                .type(type)
                .label(toLabel(name))
                .defaultValue(defaultValue)
                .build();
    }

    /**
     * Create a select property with options
     */
    protected PropDefinition prop(String name, PropDefinition.PropType type, Object defaultValue, List<String> options) {
        return PropDefinition.builder()
                .name(name)
                .type(type)
                .label(toLabel(name))
                .defaultValue(defaultValue)
                .options(options)
                .build();
    }

    /**
     * Create a select property
     */
    protected PropDefinition selectProp(String name, String defaultValue, List<String> options) {
        return PropDefinition.builder()
                .name(name)
                .type(PropDefinition.PropType.SELECT)
                .label(toLabel(name))
                .defaultValue(defaultValue)
                .options(options)
                .build();
    }

    /**
     * Create a boolean property
     */
    protected PropDefinition booleanProp(String name, boolean defaultValue) {
        return PropDefinition.builder()
                .name(name)
                .type(PropDefinition.PropType.BOOLEAN)
                .label(toLabel(name))
                .defaultValue(defaultValue)
                .build();
    }

    /**
     * Create a number property
     */
    protected PropDefinition numberProp(String name, Number defaultValue) {
        return PropDefinition.builder()
                .name(name)
                .type(PropDefinition.PropType.NUMBER)
                .label(toLabel(name))
                .defaultValue(defaultValue)
                .build();
    }

    /**
     * Create a number property with min/max
     */
    protected PropDefinition numberProp(String name, Number defaultValue, Number min, Number max) {
        return PropDefinition.builder()
                .name(name)
                .type(PropDefinition.PropType.NUMBER)
                .label(toLabel(name))
                .defaultValue(defaultValue)
                .min(min)
                .max(max)
                .build();
    }

    // ==================== Helper Methods for Defining Styles ====================

    /**
     * Create a style definition
     */
    protected StyleDefinition style(String property, StyleDefinition.StyleType type, String defaultValue) {
        return StyleDefinition.builder()
                .property(property)
                .type(type)
                .label(toLabel(property))
                .defaultValue(defaultValue)
                .build();
    }

    /**
     * Create a style with allowed units
     */
    protected StyleDefinition style(String property, StyleDefinition.StyleType type, String defaultValue, List<String> units) {
        return StyleDefinition.builder()
                .property(property)
                .type(type)
                .label(toLabel(property))
                .defaultValue(defaultValue)
                .allowedUnits(units)
                .build();
    }

    /**
     * Create a color style
     */
    protected StyleDefinition colorStyle(String property, String defaultValue) {
        return StyleDefinition.builder()
                .property(property)
                .type(StyleDefinition.StyleType.COLOR)
                .label(toLabel(property))
                .defaultValue(defaultValue)
                .category("appearance")
                .build();
    }

    /**
     * Create a size style (with px, rem, em, % units)
     */
    protected StyleDefinition sizeStyle(String property, String defaultValue) {
        return StyleDefinition.builder()
                .property(property)
                .type(StyleDefinition.StyleType.SIZE)
                .label(toLabel(property))
                .defaultValue(defaultValue)
                .allowedUnits(List.of("px", "rem", "em", "%"))
                .category("dimensions")
                .build();
    }

    /**
     * Create a select style
     */
    protected StyleDefinition selectStyle(String property, String defaultValue, List<String> options) {
        return StyleDefinition.builder()
                .property(property)
                .type(StyleDefinition.StyleType.SELECT)
                .label(toLabel(property))
                .defaultValue(defaultValue)
                .options(options)
                .build();
    }

    // ==================== Private Helper Methods ====================

    /**
     * Build the complete ComponentManifest from annotation and defined props/styles
     */
    private ComponentManifest buildManifest() {
        UIComponent ann = getAnnotation();

        return ComponentManifest.builder()
                .componentId(ann.componentId())
                .displayName(ann.displayName())
                .category(ann.category())
                .icon(ann.icon())
                .description(getDescription())
                .pluginId(getPluginId())
                .pluginVersion(getVersion())
                .reactComponentPath(getReactComponentPath())
                .defaultProps(getDefaultProps())
                .defaultStyles(getDefaultStyles())
                .configurableProps(defineProps())
                .configurableStyles(defineStyles())
                .sizeConstraints(buildSizeConstraints())
                .canHaveChildren(canHaveChildren())
                .allowedChildTypes(getAllowedChildTypes())
                .build();
    }

    /**
     * Build SizeConstraints from annotation
     */
    private SizeConstraints buildSizeConstraints() {
        UIComponent ann = getAnnotation();

        return SizeConstraints.builder()
                .resizable(ann.resizable())
                .defaultWidth(ann.defaultWidth())
                .defaultHeight(ann.defaultHeight())
                .minWidth(ann.minWidth())
                .maxWidth(ann.maxWidth())
                .minHeight(ann.minHeight())
                .maxHeight(ann.maxHeight())
                .widthLocked(false)
                .heightLocked(false)
                .maintainAspectRatio(false)
                .build();
    }

    /**
     * Convert camelCase to kebab-case
     * Example: "HorizontalRow" -> "horizontal-row"
     */
    private String toKebabCase(String str) {
        return str.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    /**
     * Convert camelCase to human-readable label
     * Example: "lineStyle" -> "Line Style"
     */
    private String toLabel(String str) {
        String spaced = str.replaceAll("([a-z])([A-Z])", "$1 $2");
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }
}
