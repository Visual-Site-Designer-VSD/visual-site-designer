package dev.mainul35.vsd.runtime.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.mainul35.vsd.runtime.plugin.LoadedPlugin;
import dev.mainul35.vsd.runtime.plugin.PluginHost;
import dev.mainul35.vsd.runtime.session.Session;
import dev.mainul35.vsd.runtime.snapshot.SiteSnapshot;
import dev.mainul35.vsd.runtime.snapshot.SnapshotService;
import dev.mainul35.vsd.runtime.util.Json;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builds the HTML shell for a page. The shell carries the page definition, the pinned plugin
 * list and the visitor's current state as JSON; the browser client mounts the plugins'
 * own React renderers from it.
 */
@Component
public class PageRenderer {

    private static final Pattern CSS_NAME = Pattern.compile("^--?[A-Za-z0-9_-]{1,64}$");
    private static final Pattern CSS_VALUE = Pattern.compile("^[^;{}<>\\\\]{1,256}$");

    private final SnapshotService snapshots;
    private final PluginHost plugins;

    public PageRenderer(SnapshotService snapshots, PluginHost plugins) {
        this.snapshots = snapshots;
        this.plugins = plugins;
    }

    public String render(SiteSnapshot.PageEntry page, Session session) {
        SiteSnapshot site = snapshots.site();

        List<Map<String, Object>> pluginList = new ArrayList<>();
        StringBuilder cssLinks = new StringBuilder();
        for (Map.Entry<String, String> pin : site.plugins().entrySet()) {
            LoadedPlugin lp = plugins.get(pin.getKey()).orElseThrow();
            boolean hasCss = lp.bundleCss().get() != null;
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", lp.id());
            p.put("version", lp.version());
            p.put("css", hasCss);
            pluginList.add(p);
            if (hasCss) {
                cssLinks.append("<link rel=\"stylesheet\" href=\"/_vsd/plugins/")
                        .append(HtmlUtils.htmlEscape(lp.id())).append("/bundle.css?v=")
                        .append(HtmlUtils.htmlEscape(lp.version())).append("\">\n");
            }
        }

        List<Map<String, Object>> pageList = site.pages().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.name());
            m.put("path", e.path());
            m.put("title", e.title());
            return m;
        }).toList();

        Map<String, Object> pageObj = new LinkedHashMap<>();
        pageObj.put("name", page.name());
        pageObj.put("path", page.path());
        pageObj.put("title", page.title());
        pageObj.put("definition", page.definition());

        Map<String, Object> sessionObj = new LinkedHashMap<>();
        sessionObj.put("csrf", session.csrf());
        sessionObj.put("rev", session.rev());
        sessionObj.put("entries", session.snapshotEntries());

        Map<String, Object> boot = new LinkedHashMap<>();
        boot.put("site", Map.of("name", site.name(), "title", site.title(), "version", site.version()));
        boot.put("page", pageObj);
        boot.put("pages", pageList);
        boot.put("plugins", pluginList);
        boot.put("session", sessionObj);
        boot.put("api", "/_vsd/api");

        String title = HtmlUtils.htmlEscape(page.title() + " - " + site.title());
        String description = page.definition().path("metaDescription").asText("");

        return "<!doctype html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "<title>" + title + "</title>\n"
                + (description.isBlank() ? "" : "<meta name=\"description\" content=\"" + HtmlUtils.htmlEscape(description) + "\">\n")
                + "<link rel=\"stylesheet\" href=\"/_vsd/client/runtime.css\">\n"
                + cssLinks
                + "<style id=\"vsd-page-styles\">" + pageStyles(page.definition()) + "</style>\n"
                + "</head>\n<body>\n<div id=\"vsd-root\"></div>\n"
                + "<noscript>This site needs JavaScript to run.</noscript>\n"
                + "<script id=\"vsd-boot\" type=\"application/json\">" + Json.forScript(Json.write(boot)) + "</script>\n"
                + "<script src=\"/_vsd/client/runtime.js\"></script>\n"
                + "</body>\n</html>\n";
    }

    public static String notFound() {
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Not found</title></head>"
                + "<body style=\"font-family:system-ui;padding:3rem\"><h1>404</h1><p>Page not found.</p></body></html>";
    }

    /** CSS variables and custom CSS authored in the builder, made safe to embed in a style element. */
    private static String pageStyles(JsonNode definition) {
        JsonNode gs = definition.path("globalStyles");
        StringBuilder css = new StringBuilder();
        JsonNode vars = gs.path("cssVariables");
        if (vars.isObject()) {
            StringBuilder decl = new StringBuilder();
            vars.fields().forEachRemaining(e -> {
                String name = e.getKey().startsWith("--") ? e.getKey() : "--" + e.getKey();
                String value = e.getValue().asText();
                if (CSS_NAME.matcher(name).matches() && CSS_VALUE.matcher(value).matches()) {
                    decl.append(name).append(':').append(value).append(';');
                }
            });
            if (!decl.isEmpty()) {
                css.append(":root{").append(decl).append("}");
            }
        }
        String custom = gs.path("customCSS").asText("");
        if (!custom.isBlank()) {
            css.append(custom.replaceAll("(?i)</style", "<\\\\/style"));
        }
        return css.toString();
    }
}
