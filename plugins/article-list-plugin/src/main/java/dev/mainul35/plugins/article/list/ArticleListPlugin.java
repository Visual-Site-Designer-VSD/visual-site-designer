package dev.mainul35.plugins.article.list;

import dev.mainul35.cms.sdk.PluginContext;
import dev.mainul35.cms.sdk.UIComponentPlugin;
import dev.mainul35.cms.sdk.annotation.UIComponent;
import dev.mainul35.cms.sdk.component.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Article List Plugin
 * Provides a paginated article list/grid component for displaying blog articles.
 * Public component - no authentication required to view.
 */
@Slf4j
@UIComponent(
    componentId = "ArticleList",
    displayName = "Article List",
    category = "content",
    icon = "\uD83D\uDCF0",
    resizable = true,
    defaultWidth = "100%",
    defaultHeight = "auto"
)
public class ArticleListPlugin implements UIComponentPlugin {

    private static final String PLUGIN_ID = "article-list-plugin";
    private static final String PLUGIN_VERSION = "1.0.0";

    private List<ComponentManifest> manifests;

    @Override
    public void onLoad(PluginContext context) throws Exception {
        log.info("Loading Article List Plugin");
        this.manifests = List.of(buildArticleListManifest());
        log.info("Article List Plugin loaded");
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
        return "/renderers/ArticleListRenderer";
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

    private ComponentManifest buildArticleListManifest() {
        return ComponentManifest.builder()
                .componentId("ArticleList")
                .displayName("Article List")
                .category("content")
                .icon("\uD83D\uDCF0")
                .description("Paginated list/grid of article cards with cover images, titles, summaries, and metadata")
                .pluginId(PLUGIN_ID)
                .pluginVersion(PLUGIN_VERSION)
                .reactComponentPath("/renderers/ArticleListRenderer")
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
                        .supportsTemplateBindings(true)
                        .build())
                .build();
    }

    private Map<String, Object> buildDefaultProps() {
        Map<String, Object> props = new HashMap<>();
        props.put("apiEndpoint", "/api/proxy/articles");
        props.put("pageSize", 10);
        props.put("layout", "cards");
        props.put("showAuthor", true);
        props.put("showDate", true);
        props.put("showSummary", true);
        props.put("showCoverImage", true);
        props.put("showTags", true);
        props.put("showPagination", true);
        props.put("articleLinkPattern", "/article/{id}");
        props.put("emptyMessage", "No articles found.");
        props.put("columns", 3);
        return props;
    }

    private Map<String, String> buildDefaultStyles() {
        Map<String, String> styles = new HashMap<>();
        styles.put("gap", "24px");
        styles.put("padding", "16px");
        return styles;
    }

    private List<PropDefinition> buildConfigurableProps() {
        List<PropDefinition> props = new ArrayList<>();

        props.add(PropDefinition.builder()
                .name("apiEndpoint")
                .type(PropDefinition.PropType.STRING)
                .label("API Endpoint")
                .defaultValue("/api/proxy/articles")
                .helpText("Backend endpoint to fetch articles from")
                .build());

        props.add(PropDefinition.builder()
                .name("pageSize")
                .type(PropDefinition.PropType.NUMBER)
                .label("Page Size")
                .defaultValue(10)
                .helpText("Number of articles per page")
                .build());

        props.add(PropDefinition.builder()
                .name("layout")
                .type(PropDefinition.PropType.SELECT)
                .label("Layout")
                .defaultValue("cards")
                .options(List.of("cards", "list", "minimal"))
                .helpText("Display layout: cards grid, vertical list, or minimal text list")
                .build());

        props.add(PropDefinition.builder()
                .name("columns")
                .type(PropDefinition.PropType.SELECT)
                .label("Grid Columns")
                .defaultValue("3")
                .options(List.of("1", "2", "3", "4"))
                .helpText("Number of columns in cards layout")
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
                .name("showSummary")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Summary")
                .defaultValue(true)
                .build());

        props.add(PropDefinition.builder()
                .name("showCoverImage")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Cover Image")
                .defaultValue(true)
                .build());

        props.add(PropDefinition.builder()
                .name("showTags")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Tags")
                .defaultValue(true)
                .build());

        props.add(PropDefinition.builder()
                .name("showPagination")
                .type(PropDefinition.PropType.BOOLEAN)
                .label("Show Pagination")
                .defaultValue(true)
                .build());

        props.add(PropDefinition.builder()
                .name("articleLinkPattern")
                .type(PropDefinition.PropType.STRING)
                .label("Article Link Pattern")
                .defaultValue("/article/{id}")
                .helpText("URL pattern for article links. Use {id} as placeholder.")
                .build());

        props.add(PropDefinition.builder()
                .name("emptyMessage")
                .type(PropDefinition.PropType.STRING)
                .label("Empty Message")
                .defaultValue("No articles found.")
                .helpText("Message shown when no articles are available")
                .build());

        return props;
    }

    private SizeConstraints buildSizeConstraints() {
        return SizeConstraints.builder()
                .resizable(true)
                .defaultWidth("100%")
                .defaultHeight("auto")
                .minWidth("300px")
                .maxWidth("100%")
                .widthLocked(false)
                .heightLocked(false)
                .build();
    }

    @Override
    public String getPluginId() { return PLUGIN_ID; }

    @Override
    public String getName() { return "Article List Plugin"; }

    @Override
    public String getVersion() { return PLUGIN_VERSION; }

    @Override
    public String getDescription() {
        return "Paginated article list/grid component for displaying blog articles";
    }
}
