package dev.mainul35.plugins.profile;

import dev.mainul35.cms.sdk.PluginContext;
import dev.mainul35.cms.sdk.UIComponentPlugin;
import dev.mainul35.cms.sdk.annotation.UIComponent;
import dev.mainul35.cms.sdk.component.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * User Profile Plugin
 * Provides a UserProfileBadge component that displays logged-in user info
 * (avatar + name) or a "Sign In" link when not authenticated.
 *
 * Designed to be placed inside a Navbar or header area.
 * Requires auth-context-plugin for authentication state, but degrades
 * gracefully if auth context is not available.
 */
@Slf4j
@UIComponent(
    componentId = "UserProfileBadge",
    displayName = "User Profile Badge",
    category = "ui",
    icon = "\uD83D\uDC64",
    resizable = true,
    defaultWidth = "200px",
    defaultHeight = "auto"
)
public class UserProfilePlugin implements UIComponentPlugin {

    private static final String PLUGIN_ID = "user-profile-plugin";
    private static final String PLUGIN_VERSION = "1.0.0";

    private List<ComponentManifest> manifests;

    @Override
    public void onLoad(PluginContext context) throws Exception {
        log.info("Loading User Profile Plugin");
        this.manifests = buildAllComponentManifests();
        log.info("User Profile Plugin loaded with {} components", manifests.size());
    }

    @Override
    public ComponentManifest getComponentManifest() {
        return manifests.get(0);
    }

    @Override
    public List<ComponentManifest> getComponentManifests() {
        return manifests;
    }

    @Override
    public String getReactComponentPath() {
        return "/renderers/UserProfileBadgeRenderer";
    }

    @Override
    public byte[] getComponentThumbnail() {
        return null;
    }

    @Override
    public ValidationResult validateProps(Map<String, Object> props) {
        return ValidationResult.builder()
                .isValid(true)
                .errors(List.of())
                .build();
    }

    private List<ComponentManifest> buildAllComponentManifests() {
        return List.of(buildUserProfileBadgeManifest());
    }

    private ComponentManifest buildUserProfileBadgeManifest() {
        return ComponentManifest.builder()
                .componentId("UserProfileBadge")
                .displayName("User Profile Badge")
                .category("ui")
                .icon("\uD83D\uDC64")
                .description("Shows logged-in user avatar and name, or a sign-in link when not authenticated")
                .pluginId(PLUGIN_ID)
                .pluginVersion(PLUGIN_VERSION)
                .reactComponentPath("/renderers/UserProfileBadgeRenderer")
                .defaultProps(buildDefaultProps())
                .defaultStyles(buildDefaultStyles())
                .configurableProps(buildConfigurableProps())
                .sizeConstraints(buildSizeConstraints())
                .requiredContexts(List.of("auth"))
                .capabilities(ComponentCapabilities.builder()
                        .canHaveChildren(false)
                        .isContainer(false)
                        .hasDataSource(false)
                        .autoHeight(true)
                        .isResizable(true)
                        .supportsTemplateBindings(false)
                        .build())
                .build();
    }

    private Map<String, Object> buildDefaultProps() {
        Map<String, Object> props = new HashMap<>();
        props.put("showAvatar", true);
        props.put("showName", true);
        props.put("avatarSize", "32");
        props.put("variant", "compact");
        props.put("showLogoutButton", false);
        props.put("loginText", "Sign In");
        props.put("logoutText", "Sign Out");
        props.put("loginUrl", "/login");
        props.put("greeting", "Hello,");
        props.put("showGreeting", false);
        return props;
    }

    private Map<String, String> buildDefaultStyles() {
        Map<String, String> styles = new HashMap<>();
        styles.put("gap", "8px");
        styles.put("fontSize", "14px");
        styles.put("color", "#333333");
        return styles;
    }

    private List<PropDefinition> buildConfigurableProps() {
        List<PropDefinition> props = new ArrayList<>();

        props.add(PropDefinition.builder()
                .name("showAvatar")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Avatar")
                .defaultValue(true)
                .helpText("Display user avatar image")
                .build());

        props.add(PropDefinition.builder()
                .name("showName")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Name")
                .defaultValue(true)
                .helpText("Display user name")
                .build());

        props.add(PropDefinition.builder()
                .name("avatarSize")
                .type(PropDefinition.PropType.SELECT)
                .label("Avatar Size")
                .defaultValue("32")
                .options(List.of("24", "32", "40", "48"))
                .helpText("Avatar image size in pixels")
                .build());

        props.add(PropDefinition.builder()
                .name("variant")
                .type(PropDefinition.PropType.SELECT)
                .label("Variant")
                .defaultValue("compact")
                .options(List.of("compact", "full", "avatar-only"))
                .helpText("Display variant: compact (avatar+name), full (avatar+name+logout), avatar-only")
                .build());

        props.add(PropDefinition.builder()
                .name("showLogoutButton")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Logout Button")
                .defaultValue(false)
                .helpText("Show inline logout button next to user info")
                .build());

        props.add(PropDefinition.builder()
                .name("loginText")
                .type(PropDefinition.PropType.STRING)
                .label("Login Text")
                .defaultValue("Sign In")
                .helpText("Text shown when user is not authenticated")
                .build());

        props.add(PropDefinition.builder()
                .name("logoutText")
                .type(PropDefinition.PropType.STRING)
                .label("Logout Text")
                .defaultValue("Sign Out")
                .helpText("Text for the logout action")
                .build());

        props.add(PropDefinition.builder()
                .name("loginUrl")
                .type(PropDefinition.PropType.STRING)
                .label("Login URL")
                .defaultValue("/login")
                .helpText("URL to redirect for login")
                .build());

        props.add(PropDefinition.builder()
                .name("showGreeting")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Greeting")
                .defaultValue(false)
                .helpText("Show a greeting prefix before the user name")
                .build());

        props.add(PropDefinition.builder()
                .name("greeting")
                .type(PropDefinition.PropType.STRING)
                .label("Greeting Text")
                .defaultValue("Hello,")
                .helpText("Greeting text prefix (e.g., 'Hello,' or 'Welcome,')")
                .build());

        return props;
    }

    private SizeConstraints buildSizeConstraints() {
        return SizeConstraints.builder()
                .resizable(true)
                .defaultWidth("200px")
                .defaultHeight("auto")
                .minWidth("80px")
                .maxWidth("400px")
                .widthLocked(false)
                .heightLocked(false)
                .build();
    }

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getName() {
        return "User Profile Plugin";
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getDescription() {
        return "User profile badge component showing logged-in user info or sign-in link";
    }
}
