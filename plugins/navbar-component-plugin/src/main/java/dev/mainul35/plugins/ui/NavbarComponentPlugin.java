package dev.mainul35.plugins.ui;

import dev.mainul35.cms.sdk.PluginContext;
import dev.mainul35.cms.sdk.UIComponentPlugin;
import dev.mainul35.cms.sdk.annotation.UIComponent;
import dev.mainul35.cms.sdk.component.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Navbar Component Plugin
 * Provides a customizable navigation bar component for the visual site builder.
 *
 * Features:
 * - Logo/brand display (text or image)
 * - Navigation links (configurable via JSON)
 * - Multiple layout variants (horizontal, centered, split)
 * - Sticky/fixed positioning option
 * - Mobile-responsive hamburger menu support
 * - Customizable colors, fonts, and spacing
 */
@Slf4j
@UIComponent(
    componentId = "navbar",
    displayName = "Navigation Bar",
    category = "ui",
    icon = "🧭",
    resizable = true,
    defaultWidth = "100%",
    defaultHeight = "60px",
    minWidth = "100%",
    maxWidth = "100%",
    minHeight = "40px",
    maxHeight = "120px"
)
public class NavbarComponentPlugin implements UIComponentPlugin {

    private PluginContext context;
    private ComponentManifest manifest;

    @Override
    public void onLoad(PluginContext context) throws Exception {
        this.context = context;
        log.info("Loading Navbar Component Plugin");

        // Build component manifest
        this.manifest = buildComponentManifest();

        log.info("Navbar Component Plugin loaded successfully");
    }

    @Override
    public void onActivate(PluginContext context) throws Exception {
        log.info("Activating Navbar Component Plugin");
    }

    @Override
    public void onDeactivate(PluginContext context) throws Exception {
        log.info("Deactivating Navbar Component Plugin");
    }

    @Override
    public void onUninstall(PluginContext context) throws Exception {
        log.info("Uninstalling Navbar Component Plugin");
    }

    @Override
    public ComponentManifest getComponentManifest() {
        return manifest;
    }

    @Override
    public String getReactComponentPath() {
        return "/components/Navbar.jsx";
    }

    @Override
    public byte[] getComponentThumbnail() {
        return null;
    }

    @Override
    public ValidationResult validateProps(Map<String, Object> props) {
        List<String> errors = new ArrayList<>();

        // Validate brand text length
        if (props.containsKey("brandText")) {
            Object brandText = props.get("brandText");
            if (brandText != null && brandText.toString().length() > 50) {
                errors.add("Brand text must not exceed 50 characters");
            }
        }

        // Validate layout variant
        if (props.containsKey("layout")) {
            String layout = props.get("layout").toString();
            if (!List.of("default", "centered", "split", "minimal").contains(layout)) {
                errors.add("Invalid layout. Must be one of: default, centered, split, minimal");
            }
        }

        // Validate nav items JSON if present
        if (props.containsKey("navItems")) {
            Object navItems = props.get("navItems");
            if (navItems != null && !(navItems instanceof List)) {
                // Try to validate it's valid JSON array structure
                try {
                    if (navItems instanceof String) {
                        // Will be parsed by frontend
                    }
                } catch (Exception e) {
                    errors.add("navItems must be a valid JSON array");
                }
            }
        }

        return ValidationResult.builder()
                .isValid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    /**
     * Build the complete component manifest
     */
    private ComponentManifest buildComponentManifest() {
        return ComponentManifest.builder()
                .componentId("navbar")
                .displayName("Navigation Bar")
                .category("ui")
                .icon("🧭")
                .description("Customizable navigation bar with logo, links, and responsive menu. Drop inside a Container/Layout component.")
                .pluginId("navbar-component-plugin")
                .pluginVersion("1.0.0")
                .reactComponentPath("/components/Navbar.jsx")
                .defaultProps(buildDefaultProps())
                .defaultStyles(buildDefaultStyles())
                .configurableProps(buildConfigurableProps())
                .configurableStyles(buildConfigurableStyles())
                .sizeConstraints(buildSizeConstraints())
                .canHaveChildren(false)
                .build();
    }

    /**
     * Default props for the navbar
     */
    private Map<String, Object> buildDefaultProps() {
        Map<String, Object> props = new HashMap<>();

        // Brand/Logo settings
        props.put("brandText", "My Site");
        props.put("brandImageUrl", "");
        props.put("brandLink", "/");

        // Navigation items (JSON array with support for nested children)
        // Structure: [{label, href, active, children: [{label, href, children: [...]}]}]
        props.put("navItems", List.of(
            Map.of("label", "Home", "href", "/", "active", true),
            Map.of("label", "About", "href", "/about", "active", false),
            Map.of("label", "Services", "href", "#", "active", false, "children", List.of(
                Map.of("label", "Web Development", "href", "/services/web"),
                Map.of("label", "Mobile Apps", "href", "/services/mobile"),
                Map.of("label", "Consulting", "href", "/services/consulting")
            )),
            Map.of("label", "Contact", "href", "/contact", "active", false)
        ));

        // Layout and behavior
        props.put("layout", "default");
        props.put("sticky", false);
        props.put("showMobileMenu", true);
        props.put("mobileBreakpoint", "768px");

        return props;
    }

    /**
     * Default styles for the navbar
     */
    private Map<String, String> buildDefaultStyles() {
        Map<String, String> styles = new HashMap<>();
        styles.put("backgroundColor", "#ffffff");
        styles.put("textColor", "#333333");
        styles.put("accentColor", "#007bff");
        styles.put("padding", "0 20px");
        styles.put("boxShadow", "0 2px 4px rgba(0,0,0,0.1)");
        styles.put("borderBottom", "1px solid #e0e0e0");
        styles.put("fontFamily", "inherit");
        styles.put("fontSize", "16px");
        return styles;
    }

    /**
     * Configurable properties for the navbar
     */
    private List<PropDefinition> buildConfigurableProps() {
        List<PropDefinition> props = new ArrayList<>();

        // Brand Text
        props.add(PropDefinition.builder()
                .name("brandText")
                .type(PropDefinition.PropType.STRING)
                .label("Brand Text")
                .defaultValue("My Site")
                .required(false)
                .helpText("Text displayed as the brand/logo (leave empty to use image only)")
                .build());

        // Brand Image URL
        props.add(PropDefinition.builder()
                .name("brandImageUrl")
                .type(PropDefinition.PropType.URL)
                .label("Brand Logo URL")
                .defaultValue("")
                .required(false)
                .helpText("URL for the brand logo image")
                .build());

        // Brand Link
        props.add(PropDefinition.builder()
                .name("brandLink")
                .type(PropDefinition.PropType.URL)
                .label("Brand Link")
                .defaultValue("/")
                .required(false)
                .helpText("URL the brand/logo links to")
                .build());

        // Navigation Items (JSON) - supports multi-level dropdowns
        props.add(PropDefinition.builder()
                .name("navItems")
                .type(PropDefinition.PropType.JSON)
                .label("Navigation Items")
                .defaultValue("[{\"label\":\"Home\",\"href\":\"/\"},{\"label\":\"Services\",\"href\":\"#\",\"children\":[{\"label\":\"Web Dev\",\"href\":\"/web\"}]}]")
                .required(false)
                .helpText("JSON array with nested children for dropdowns: [{\"label\":\"Menu\",\"href\":\"#\",\"children\":[{\"label\":\"Sub\",\"href\":\"/sub\"}]}]")
                .build());

        // Layout variant
        props.add(PropDefinition.builder()
                .name("layout")
                .type(PropDefinition.PropType.SELECT)
                .label("Layout")
                .defaultValue("default")
                .options(List.of("default", "centered", "split", "minimal"))
                .helpText("Navbar layout variant")
                .build());

        // Sticky
        props.add(PropDefinition.builder()
                .name("sticky")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Sticky Header")
                .defaultValue(false)
                .helpText("Fix navbar to top of viewport when scrolling")
                .build());

        // Show Mobile Menu
        props.add(PropDefinition.builder()
                .name("showMobileMenu")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Mobile Menu")
                .defaultValue(true)
                .helpText("Show hamburger menu on mobile devices")
                .build());

        // Mobile Breakpoint
        props.add(PropDefinition.builder()
                .name("mobileBreakpoint")
                .type(PropDefinition.PropType.SELECT)
                .label("Mobile Breakpoint")
                .defaultValue("768px")
                .options(List.of("576px", "768px", "992px", "1024px"))
                .helpText("Screen width below which mobile menu appears")
                .build());

        return props;
    }

    /**
     * Configurable styles for the navbar
     */
    private List<StyleDefinition> buildConfigurableStyles() {
        List<StyleDefinition> styles = new ArrayList<>();

        // Background Color
        styles.add(StyleDefinition.builder()
                .property("backgroundColor")
                .type(StyleDefinition.StyleType.COLOR)
                .label("Background Color")
                .defaultValue("#ffffff")
                .category("colors")
                .build());

        // Text Color
        styles.add(StyleDefinition.builder()
                .property("textColor")
                .type(StyleDefinition.StyleType.COLOR)
                .label("Text Color")
                .defaultValue("#333333")
                .category("colors")
                .build());

        // Accent/Active Color
        styles.add(StyleDefinition.builder()
                .property("accentColor")
                .type(StyleDefinition.StyleType.COLOR)
                .label("Accent Color")
                .defaultValue("#007bff")
                .category("colors")
                .helpText("Color for active links and hover states")
                .build());

        // Padding
        styles.add(StyleDefinition.builder()
                .property("padding")
                .type(StyleDefinition.StyleType.SPACING)
                .label("Padding")
                .defaultValue("0 20px")
                .allowedUnits(List.of("px", "rem", "em"))
                .category("spacing")
                .build());

        // Box Shadow
        styles.add(StyleDefinition.builder()
                .property("boxShadow")
                .type(StyleDefinition.StyleType.SHADOW)
                .label("Box Shadow")
                .defaultValue("0 2px 4px rgba(0,0,0,0.1)")
                .category("effects")
                .build());

        // Border Bottom
        styles.add(StyleDefinition.builder()
                .property("borderBottom")
                .type(StyleDefinition.StyleType.BORDER)
                .label("Border Bottom")
                .defaultValue("1px solid #e0e0e0")
                .category("border")
                .build());

        // Font Size
        styles.add(StyleDefinition.builder()
                .property("fontSize")
                .type(StyleDefinition.StyleType.SIZE)
                .label("Font Size")
                .defaultValue("16px")
                .allowedUnits(List.of("px", "rem", "em"))
                .category("text")
                .build());

        // Font Family
        styles.add(StyleDefinition.builder()
                .property("fontFamily")
                .type(StyleDefinition.StyleType.FONT_FAMILY)
                .label("Font Family")
                .defaultValue("inherit")
                .options(List.of("inherit", "Arial, sans-serif", "Georgia, serif", "'Roboto', sans-serif", "'Open Sans', sans-serif"))
                .category("text")
                .build());

        return styles;
    }

    /**
     * Size constraints for the navbar
     */
    private SizeConstraints buildSizeConstraints() {
        return SizeConstraints.builder()
                .resizable(true)
                .defaultWidth("100%")
                .defaultHeight("60px")
                .minWidth("100%")
                .maxWidth("100%")
                .minHeight("40px")
                .maxHeight("120px")
                .widthLocked(true)  // Width should always be 100% to fill parent container
                .heightLocked(false)
                .maintainAspectRatio(false)
                .build();
    }

    @Override
    public String getPluginId() {
        return "navbar-component-plugin";
    }

    @Override
    public String getName() {
        return "Navigation Bar Component";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "A customizable navigation bar component for the visual site builder";
    }
}
