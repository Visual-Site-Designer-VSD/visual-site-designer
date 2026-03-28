package dev.mainul35.plugins.article.viewer;

import dev.mainul35.cms.sdk.PluginContext;
import dev.mainul35.cms.sdk.UIComponentPlugin;
import dev.mainul35.cms.sdk.annotation.UIComponent;
import dev.mainul35.cms.sdk.component.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Article Viewer Plugin
 * Displays a single article with full content, cover image, author, date, and tags.
 * Public component - no authentication required.
 * Reads the article ID from the URL path.
 */
@Slf4j
@UIComponent(
    componentId = "ArticleViewer",
    displayName = "Article Viewer",
    category = "content",
    icon = "\uD83D\uDCDD",
    resizable = true,
    defaultWidth = "100%",
    defaultHeight = "auto"
)
public class ArticleViewerPlugin implements UIComponentPlugin {

    private static final String PLUGIN_ID = "article-viewer-plugin";
    private static final String PLUGIN_VERSION = "1.0.0";

    private List<ComponentManifest> manifests;

    @Override
    public void onLoad(PluginContext context) throws Exception {
        log.info("Loading Article Viewer Plugin");
        this.manifests = List.of(buildArticleViewerManifest());
        log.info("Article Viewer Plugin loaded");
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
        return "/renderers/ArticleViewerRenderer";
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

    private ComponentManifest buildArticleViewerManifest() {
        return ComponentManifest.builder()
                .componentId("ArticleViewer")
                .displayName("Article Viewer")
                .category("content")
                .icon("\uD83D\uDCDD")
                .description("Displays a single article with full content, cover image, author info, and metadata")
                .pluginId(PLUGIN_ID)
                .pluginVersion(PLUGIN_VERSION)
                .reactComponentPath("/renderers/ArticleViewerRenderer")
                .defaultProps(buildDefaultProps())
                .defaultStyles(buildDefaultStyles())
                .configurableProps(buildConfigurableProps())
                .sizeConstraints(buildSizeConstraints())
                .requiredContexts(List.of())
                .capabilities(ComponentCapabilities.builder()
                        .canHaveChildren(false)
                        .isContainer(false)
                        .hasDataSource(true)
                        .autoHeight(true)
                        .isResizable(true)
                        .supportsTemplateBindings(false)
                        .build())
                .build();
    }

    private Map<String, Object> buildDefaultProps() {
        Map<String, Object> props = new HashMap<>();
        props.put("apiEndpoint", "/api/proxy/articles");
        props.put("articleIdParam", "id");
        props.put("showAuthor", true);
        props.put("showDate", true);
        props.put("showTags", true);
        props.put("showCoverImage", true);
        props.put("notFoundMessage", "Article not found.");
        props.put("backLinkText", "Back to articles");
        props.put("backLinkUrl", "/");
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
                .helpText("Backend endpoint for fetching articles. Article ID is appended as /{id}")
                .build());

        props.add(PropDefinition.builder()
                .name("articleIdParam")
                .type(PropDefinition.PropType.STRING)
                .label("Article ID Parameter")
                .defaultValue("id")
                .helpText("URL path segment name to extract article ID from")
                .build());

        props.add(PropDefinition.builder()
                .name("showAuthor")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Author")
                .defaultValue(true)
                .build());

        props.add(PropDefinition.builder()
                .name("showDate")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Date")
                .defaultValue(true)
                .build());

        props.add(PropDefinition.builder()
                .name("showTags")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Tags")
                .defaultValue(true)
                .build());

        props.add(PropDefinition.builder()
                .name("showCoverImage")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Cover Image")
                .defaultValue(true)
                .build());

        props.add(PropDefinition.builder()
                .name("notFoundMessage")
                .type(PropDefinition.PropType.STRING)
                .label("Not Found Message")
                .defaultValue("Article not found.")
                .build());

        props.add(PropDefinition.builder()
                .name("backLinkText")
                .type(PropDefinition.PropType.STRING)
                .label("Back Link Text")
                .defaultValue("Back to articles")
                .build());

        props.add(PropDefinition.builder()
                .name("backLinkUrl")
                .type(PropDefinition.PropType.STRING)
                .label("Back Link URL")
                .defaultValue("/")
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
    public String getName() { return "Article Viewer Plugin"; }

    @Override
    public String getVersion() { return PLUGIN_VERSION; }

    @Override
    public String getDescription() {
        return "Single article viewer component displaying full article content with metadata";
    }
}
