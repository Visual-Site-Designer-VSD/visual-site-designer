package dev.mainul35.plugins.article.editor;

import dev.mainul35.cms.sdk.PluginContext;
import dev.mainul35.cms.sdk.UIComponentPlugin;
import dev.mainul35.cms.sdk.annotation.UIComponent;
import dev.mainul35.cms.sdk.component.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Article Editor Plugin
 * Provides a Medium-like rich text editor for writing articles.
 * Uses TipTap editor (bundled in the IIFE, not externalized).
 * Requires authentication - only logged-in users can write articles.
 */
@Slf4j
@UIComponent(
    componentId = "ArticleEditor",
    displayName = "Article Editor",
    category = "content",
    icon = "\u270F\uFE0F",
    resizable = true,
    defaultWidth = "100%",
    defaultHeight = "auto"
)
public class ArticleEditorPlugin implements UIComponentPlugin {

    private static final String PLUGIN_ID = "article-editor-plugin";
    private static final String PLUGIN_VERSION = "1.0.0";

    private List<ComponentManifest> manifests;

    @Override
    public void onLoad(PluginContext context) throws Exception {
        log.info("Loading Article Editor Plugin");
        this.manifests = List.of(buildArticleEditorManifest());
        log.info("Article Editor Plugin loaded");
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
        return "/renderers/ArticleEditorRenderer";
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

    private ComponentManifest buildArticleEditorManifest() {
        return ComponentManifest.builder()
                .componentId("ArticleEditor")
                .displayName("Article Editor")
                .category("content")
                .icon("\u270F\uFE0F")
                .description("Medium-like rich text editor for writing and publishing articles")
                .pluginId(PLUGIN_ID)
                .pluginVersion(PLUGIN_VERSION)
                .reactComponentPath("/renderers/ArticleEditorRenderer")
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
        props.put("apiEndpoint", "/api/proxy/articles");
        props.put("placeholder", "Start writing your story...");
        props.put("showToolbar", true);
        props.put("showTitleField", true);
        props.put("showTagsField", true);
        props.put("showCoverImage", true);
        props.put("showSaveDraft", true);
        props.put("showPublish", true);
        props.put("redirectAfterPublish", "/");
        props.put("titlePlaceholder", "Article title...");
        props.put("tagsPlaceholder", "Add tags separated by commas...");
        props.put("coverImagePlaceholder", "Cover image URL...");
        props.put("saveDraftText", "Save Draft");
        props.put("publishText", "Publish");
        props.put("loginPromptText", "Please sign in to write articles.");
        props.put("loginButtonText", "Sign In");
        props.put("loginUrl", "/login");
        return props;
    }

    private Map<String, String> buildDefaultStyles() {
        Map<String, String> styles = new HashMap<>();
        styles.put("maxWidth", "768px");
        styles.put("padding", "24px");
        styles.put("margin", "0 auto");
        return styles;
    }

    private List<PropDefinition> buildConfigurableProps() {
        List<PropDefinition> props = new ArrayList<>();

        props.add(PropDefinition.builder()
                .name("apiEndpoint")
                .type(PropDefinition.PropType.STRING)
                .label("API Endpoint")
                .defaultValue("/api/proxy/articles")
                .helpText("Backend endpoint for creating/saving articles")
                .build());

        props.add(PropDefinition.builder()
                .name("placeholder")
                .type(PropDefinition.PropType.STRING)
                .label("Editor Placeholder")
                .defaultValue("Start writing your story...")
                .build());

        props.add(PropDefinition.builder()
                .name("showToolbar")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Toolbar")
                .defaultValue(true)
                .helpText("Show formatting toolbar above the editor")
                .build());

        props.add(PropDefinition.builder()
                .name("showTitleField")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Title Field")
                .defaultValue(true)
                .build());

        props.add(PropDefinition.builder()
                .name("showTagsField")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Tags Field")
                .defaultValue(true)
                .build());

        props.add(PropDefinition.builder()
                .name("showCoverImage")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Cover Image Field")
                .defaultValue(true)
                .build());

        props.add(PropDefinition.builder()
                .name("showSaveDraft")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Save Draft Button")
                .defaultValue(true)
                .build());

        props.add(PropDefinition.builder()
                .name("showPublish")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Publish Button")
                .defaultValue(true)
                .build());

        props.add(PropDefinition.builder()
                .name("redirectAfterPublish")
                .type(PropDefinition.PropType.STRING)
                .label("Redirect After Publish")
                .defaultValue("/")
                .helpText("URL to navigate to after publishing")
                .build());

        props.add(PropDefinition.builder()
                .name("loginPromptText")
                .type(PropDefinition.PropType.STRING)
                .label("Login Prompt Text")
                .defaultValue("Please sign in to write articles.")
                .build());

        props.add(PropDefinition.builder()
                .name("loginUrl")
                .type(PropDefinition.PropType.STRING)
                .label("Login URL")
                .defaultValue("/login")
                .build());

        return props;
    }

    private SizeConstraints buildSizeConstraints() {
        return SizeConstraints.builder()
                .resizable(true)
                .defaultWidth("100%")
                .defaultHeight("auto")
                .minWidth("320px")
                .maxWidth("100%")
                .widthLocked(false)
                .heightLocked(false)
                .build();
    }

    @Override
    public String getPluginId() { return PLUGIN_ID; }

    @Override
    public String getName() { return "Article Editor Plugin"; }

    @Override
    public String getVersion() { return PLUGIN_VERSION; }

    @Override
    public String getDescription() {
        return "Medium-like rich text article editor with TipTap, requires authentication";
    }
}
