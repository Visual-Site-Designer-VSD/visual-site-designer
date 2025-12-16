package dev.mainul35.plugins.layout;

import dev.mainul35.cms.sdk.Plugin;
import dev.mainul35.cms.sdk.PluginContext;
import dev.mainul35.cms.sdk.UIComponentPlugin;
import dev.mainul35.cms.sdk.annotation.UIComponent;
import dev.mainul35.cms.sdk.component.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Container Layout Plugin
 * Provides a flexible container component that can hold child components
 */
@Slf4j
@UIComponent(
    componentId = "container",
    displayName = "Container",
    category = "layout",
    icon = "📦",
    resizable = true,
    defaultWidth = "100%",
    defaultHeight = "auto"
)
public class ContainerLayoutPlugin implements UIComponentPlugin {

    private PluginContext context;
    private ComponentManifest manifest;

    @Override
    public void onLoad(PluginContext context) throws Exception {
        this.context = context;
        log.info("Loading Container Layout Plugin");
        this.manifest = buildComponentManifest();
        log.info("Container Layout Plugin loaded successfully");
    }

    @Override
    public void onActivate(PluginContext context) throws Exception {
        log.info("Activating Container Layout Plugin");
    }

    @Override
    public void onDeactivate(PluginContext context) throws Exception {
        log.info("Deactivating Container Layout Plugin");
    }

    @Override
    public void onUninstall(PluginContext context) throws Exception {
        log.info("Uninstalling Container Layout Plugin");
    }

    @Override
    public ComponentManifest getComponentManifest() {
        return manifest;
    }

    @Override
    public String getReactComponentPath() {
        return "/components/Container.jsx";
    }

    @Override
    public byte[] getComponentThumbnail() {
        return null;
    }

    @Override
    public ValidationResult validateProps(Map<String, Object> props) {
        List<String> errors = new ArrayList<>();

        // Validate padding
        if (props.containsKey("padding")) {
            String padding = props.get("padding").toString();
            if (!padding.matches("\\d+(px|rem|em|%)")) {
                errors.add("Invalid padding format. Use format like '20px' or '1rem'");
            }
        }

        // Validate max-width
        if (props.containsKey("maxWidth")) {
            String maxWidth = props.get("maxWidth").toString();
            if (!maxWidth.equals("none") && !maxWidth.matches("\\d+(px|rem|em|%)")) {
                errors.add("Invalid maxWidth format. Use 'none' or format like '1200px'");
            }
        }

        return ValidationResult.builder()
                .isValid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    private ComponentManifest buildComponentManifest() {
        return ComponentManifest.builder()
                .componentId("container")
                .displayName("Container")
                .category("layout")
                .icon("📦")
                .description("Flexible container layout that holds child components")
                .pluginId("container-layout-plugin")
                .pluginVersion("1.0.0")
                .reactComponentPath("/components/Container.jsx")
                .defaultProps(buildDefaultProps())
                .defaultStyles(buildDefaultStyles())
                .configurableProps(buildConfigurableProps())
                .configurableStyles(buildConfigurableStyles())
                .sizeConstraints(buildSizeConstraints())
                .canHaveChildren(true)  // ← KEY: This container can hold children
                .allowedChildTypes(null) // null = allows all types
                .build();
    }

    private Map<String, Object> buildDefaultProps() {
        Map<String, Object> props = new HashMap<>();
        props.put("layoutType", "flex-column");
        props.put("padding", "20px");
        props.put("maxWidth", "1200px");
        props.put("centerContent", true);
        props.put("allowOverflow", false);
        return props;
    }

    private Map<String, String> buildDefaultStyles() {
        Map<String, String> styles = new HashMap<>();
        styles.put("display", "flex");
        styles.put("flexDirection", "column");
        styles.put("gap", "16px");
        styles.put("backgroundColor", "#ffffff");
        styles.put("borderRadius", "8px");
        styles.put("boxShadow", "0 1px 3px rgba(0,0,0,0.1)");
        return styles;
    }

    private List<PropDefinition> buildConfigurableProps() {
        List<PropDefinition> props = new ArrayList<>();

        props.add(PropDefinition.builder()
                .name("layoutType")
                .type(PropDefinition.PropType.SELECT)
                .label("Layout Type")
                .defaultValue("flex-column")
                .options(List.of(
                    "flex-column",
                    "flex-row",
                    "flex-wrap",
                    "grid-2col",
                    "grid-3col",
                    "grid-4col",
                    "grid-auto"
                ))
                .required(true)
                .helpText("Choose the layout type for arranging child components")
                .build());

        props.add(PropDefinition.builder()
                .name("padding")
                .type(PropDefinition.PropType.STRING)
                .label("Padding")
                .defaultValue("20px")
                .required(false)
                .helpText("Container padding (e.g., 20px, 1rem)")
                .build());

        props.add(PropDefinition.builder()
                .name("maxWidth")
                .type(PropDefinition.PropType.STRING)
                .label("Max Width")
                .defaultValue("1200px")
                .required(false)
                .helpText("Maximum width of container (e.g., 1200px, or 'none')")
                .build());

        props.add(PropDefinition.builder()
                .name("centerContent")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Center Content")
                .defaultValue(true)
                .helpText("Center the container horizontally")
                .build());

        props.add(PropDefinition.builder()
                .name("allowOverflow")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Allow Overflow")
                .defaultValue(false)
                .helpText("Allow content to overflow container")
                .build());

        return props;
    }

    private List<StyleDefinition> buildConfigurableStyles() {
        List<StyleDefinition> styles = new ArrayList<>();

        styles.add(StyleDefinition.builder()
                .property("gap")
                .type(StyleDefinition.StyleType.SIZE)
                .label("Gap Between Items")
                .defaultValue("16px")
                .allowedUnits(List.of("px", "rem", "em"))
                .category("spacing")
                .build());

        styles.add(StyleDefinition.builder()
                .property("backgroundColor")
                .type(StyleDefinition.StyleType.COLOR)
                .label("Background Color")
                .defaultValue("#ffffff")
                .category("appearance")
                .build());

        styles.add(StyleDefinition.builder()
                .property("borderRadius")
                .type(StyleDefinition.StyleType.SIZE)
                .label("Border Radius")
                .defaultValue("8px")
                .allowedUnits(List.of("px", "rem", "%"))
                .category("border")
                .build());

        return styles;
    }

    private SizeConstraints buildSizeConstraints() {
        return SizeConstraints.builder()
                .resizable(true)
                .defaultWidth("100%")
                .defaultHeight("auto")
                .minWidth("200px")
                .maxWidth("100%")
                .minHeight("auto")
                .heightLocked(false)
                .build();
    }

    @Override
    public String getPluginId() {
        return "container-layout-plugin";
    }

    @Override
    public String getName() {
        return "Container Layout";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "A flexible container layout component that can hold child components";
    }
}
