package dev.mainul35.vsd.runtime.snapshot;

import com.fasterxml.jackson.databind.JsonNode;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import dev.mainul35.vsd.runtime.plugin.LoadedPlugin;
import dev.mainul35.vsd.runtime.plugin.PluginHost;
import dev.mainul35.vsd.runtime.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads the published snapshot at startup and refuses to start if it cannot be served
 * faithfully: a missing plugin, a plugin at a different version than the one the site was
 * published with, or a page that uses a plugin the snapshot does not pin.
 *
 * <p>Layout of the snapshot directory:
 * <pre>
 * site.json            name, title, version, defaultPage, plugins{id:version}, pages[{name,path,title,file}]
 * pages/*.json         builder PageDefinition documents
 * assets/              optional static files, served under /assets/
 * </pre>
 */
@Service
public class SnapshotService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);

    private final SiteSnapshot site;

    public SnapshotService(RuntimeProperties props, PluginHost plugins) {
        this.site = load(props.snapshotPath(), plugins);
        log.info("Serving site '{}' v{} with {} page(s) and {} pinned plugin(s)",
                site.name(), site.version(), site.pages().size(), site.plugins().size());
    }

    public SiteSnapshot site() {
        return site;
    }

    public Optional<SiteSnapshot.PageEntry> byPath(String path) {
        String p = normalize(path);
        return site.pages().stream().filter(e -> e.path().equals(p)).findFirst();
    }

    public Optional<SiteSnapshot.PageEntry> byName(String name) {
        return site.pages().stream().filter(e -> e.name().equals(name)).findFirst();
    }

    static String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private static SiteSnapshot load(Path dir, PluginHost host) {
        Path siteFile = dir.resolve("site.json");
        if (!Files.isRegularFile(siteFile)) {
            throw new IllegalStateException("No site.json in snapshot dir " + dir.toAbsolutePath());
        }
        try {
            JsonNode root = Json.read(Files.readString(siteFile));
            String name = text(root, "siteName", "site");
            Map<String, String> pinned = new LinkedHashMap<>();
            root.path("plugins").fields().forEachRemaining(e -> pinned.put(e.getKey(), e.getValue().asText()));

            List<SiteSnapshot.PageEntry> pages = new ArrayList<>();
            Set<String> paths = new LinkedHashSet<>();
            Set<String> used = new LinkedHashSet<>();
            for (JsonNode p : root.path("pages")) {
                String pageName = text(p, "name", null);
                String path = normalize(text(p, "path", "/" + pageName));
                if (pageName == null || !path.startsWith("/") || !paths.add(path)) {
                    throw new IllegalStateException("Invalid or duplicate page entry: " + p);
                }
                Path file = dir.resolve(text(p, "file", "pages/" + pageName + ".json")).normalize();
                if (!file.startsWith(dir.normalize()) || !Files.isRegularFile(file)) {
                    throw new IllegalStateException("Page file not found for '" + pageName + "': " + file);
                }
                JsonNode def = Json.read(Files.readString(file));
                collectPlugins(def.path("components"), used);
                pages.add(new SiteSnapshot.PageEntry(pageName, path, text(p, "title", pageName), def));
            }
            if (pages.isEmpty()) {
                throw new IllegalStateException("Snapshot has no pages");
            }

            List<String> problems = new ArrayList<>();
            for (String id : used) {
                if (!pinned.containsKey(id)) {
                    problems.add("pages use plugin '" + id + "' but the snapshot does not pin it");
                }
            }
            for (Map.Entry<String, String> e : pinned.entrySet()) {
                Optional<LoadedPlugin> lp = host.get(e.getKey());
                if (lp.isEmpty()) {
                    problems.add("pinned plugin '" + e.getKey() + "' is not installed");
                } else if (!lp.get().version().equals(e.getValue())) {
                    problems.add("plugin '" + e.getKey() + "' is v" + lp.get().version() + " but the site was published with v" + e.getValue());
                }
            }
            if (!problems.isEmpty()) {
                throw new IllegalStateException("Snapshot cannot be served: " + String.join("; ", problems));
            }

            String defaultPage = text(root, "defaultPage", pages.get(0).name());
            return new SiteSnapshot(name, text(root, "title", name), text(root, "snapshotVersion", "1"),
                    defaultPage, Map.copyOf(pinned), List.copyOf(pages), dir.resolve("assets"));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read snapshot in " + dir.toAbsolutePath(), e);
        }
    }

    private static void collectPlugins(JsonNode components, Set<String> out) {
        for (JsonNode c : components) {
            String id = c.path("pluginId").asText(null);
            if (id != null && !id.isBlank()) {
                out.add(id);
            }
            collectPlugins(c.path("children"), out);
        }
    }

    private static String text(JsonNode n, String field, String fallback) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() || v.asText().isBlank() ? fallback : v.asText();
    }
}
