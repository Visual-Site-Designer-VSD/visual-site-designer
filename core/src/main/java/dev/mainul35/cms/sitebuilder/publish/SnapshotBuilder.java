package dev.mainul35.cms.sitebuilder.publish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Writes an immutable site snapshot in the format the live runtime (vsd-runtime) serves:
 *
 * <pre>
 * site.json      siteName, title, snapshotVersion, defaultPage, plugins{id:version}, pages[{name,path,title,file}]
 * pages/*.json   the builder's page definitions, unchanged
 * </pre>
 *
 * Pure file and JSON logic with no persistence, so it can be tested on its own. It pins the version
 * of every plugin a page uses; the runtime refuses to start if any of them differs.
 */
public class SnapshotBuilder {

    public record SiteInfo(String name, String title) {
    }

    /** One page to publish. {@code definitionJson} is the page definition exactly as saved in the builder. */
    public record PageInput(String slug, String name, String title, String routePath, boolean home,
                            String definitionJson) {
    }

    /** What was written. */
    public record Result(String version, Path directory, int pages, Map<String, String> plugins) {
    }

    private final ObjectMapper mapper;

    public SnapshotBuilder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * @param installedPlugins plugin id to version, for plugins that are installed and active
     * @param siteRoot         directory holding this site's snapshots; the new one goes to {@code siteRoot/<version>}
     */
    public Result build(SiteInfo site, List<PageInput> pages, Map<String, String> installedPlugins,
                        Path siteRoot, String version) {
        if (pages.isEmpty()) {
            throw new PublishException("Nothing to publish: the site has no pages with saved content.");
        }
        Path finalDir = siteRoot.resolve(version);
        if (Files.exists(finalDir)) {
            throw new PublishException("Snapshot " + version + " already exists.");
        }

        List<String> problems = new ArrayList<>();
        Map<String, JsonNode> definitions = new TreeMap<>();
        Map<String, Set<String>> pluginUsers = new TreeMap<>();
        Set<String> paths = new HashSet<>();
        Set<String> files = new HashSet<>();
        List<String> pageFiles = new ArrayList<>();
        String homeName = null;

        for (PageInput p : pages) {
            String path = normalizePath(p.routePath(), p.slug(), p.home());
            if (path == null) {
                problems.add("Page '" + p.name() + "' has an unusable route path '" + p.routePath() + "'.");
            } else if (!paths.add(path)) {
                problems.add("Two pages use the route '" + path + "'; page '" + p.name() + "' is one of them.");
            }
            String file = uniqueFile(files, p.slug());
            pageFiles.add(file);
            JsonNode def;
            try {
                def = mapper.readTree(p.definitionJson());
            } catch (IOException e) {
                problems.add("Page '" + p.name() + "' has a definition that is not valid JSON.");
                continue;
            }
            if (def == null || !def.isObject() || !def.path("components").isArray()) {
                problems.add("Page '" + p.name() + "' has no component list.");
                continue;
            }
            definitions.put(file, def);
            collectPlugins(def.path("components"), p.name(), pluginUsers);
            if (p.home() && homeName == null) {
                homeName = p.slug();
            }
        }

        Map<String, String> pinned = new TreeMap<>();
        for (var e : pluginUsers.entrySet()) {
            String installed = installedPlugins.get(e.getKey());
            if (installed == null) {
                problems.add("Plugin '" + e.getKey() + "' is not installed and active, but page(s) "
                        + String.join(", ", e.getValue()) + " use it.");
            } else {
                pinned.put(e.getKey(), installed);
            }
        }
        if (!problems.isEmpty()) {
            throw new PublishException(String.join("\n", problems));
        }

        ObjectNode root = mapper.createObjectNode();
        root.put("siteName", site.name());
        root.put("title", site.title() != null ? site.title() : site.name());
        root.put("snapshotVersion", version);
        ObjectNode pluginsNode = root.putObject("plugins");
        pinned.forEach(pluginsNode::put);
        ArrayNode pagesNode = root.putArray("pages");
        for (int i = 0; i < pages.size(); i++) {
            PageInput p = pages.get(i);
            ObjectNode n = pagesNode.addObject();
            n.put("name", p.slug());
            n.put("path", normalizePath(p.routePath(), p.slug(), p.home()));
            n.put("title", p.title() != null && !p.title().isBlank() ? p.title() : p.name());
            n.put("file", "pages/" + pageFiles.get(i) + ".json");
        }
        root.put("defaultPage", homeName != null ? homeName : pages.get(0).slug());

        Path staging = siteRoot.resolve(".staging-" + version);
        try {
            Files.createDirectories(staging.resolve("pages"));
            for (var e : definitions.entrySet()) {
                write(staging.resolve("pages").resolve(e.getKey() + ".json"), e.getValue());
            }
            write(staging.resolve("site.json"), root);
            Files.move(staging, finalDir, StandardCopyOption.ATOMIC_MOVE);
            // Only now point "latest" at it, so a reader never sees a half-written snapshot.
            Path tmp = siteRoot.resolve(".latest.tmp");
            Files.writeString(tmp, version, StandardCharsets.UTF_8);
            Files.move(tmp, siteRoot.resolve("latest"), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            deleteQuietly(staging);
            throw new PublishException("Could not write the snapshot: " + e.getMessage(), e);
        }
        return new Result(version, finalDir, pages.size(), pinned);
    }

    private void write(Path file, JsonNode node) throws IOException {
        Files.writeString(file, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node),
                StandardCharsets.UTF_8);
    }

    private static void collectPlugins(JsonNode components, String pageName, Map<String, Set<String>> out) {
        for (JsonNode c : components) {
            String id = c.path("pluginId").asText("");
            if (!id.isEmpty()) {
                out.computeIfAbsent(id, k -> new TreeSet<>()).add("'" + pageName + "'");
            }
            if (c.path("children").isArray()) {
                collectPlugins(c.path("children"), pageName, out);
            }
        }
    }

    /** Route path such as {@code /about}; null if it could not be served safely. */
    static String normalizePath(String routePath, String slug, boolean home) {
        String p = routePath;
        if (p == null || p.isBlank()) {
            p = home ? "/" : "/" + slug;
        }
        p = p.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        if (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        if (p.contains("..") || p.contains("\\") || p.contains("//") || p.contains("?") || p.contains("#")) {
            return null;
        }
        return p;
    }

    private static String uniqueFile(Set<String> used, String slug) {
        String base = slug == null ? "page"
                : slug.toLowerCase().replaceAll("[^a-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
        if (base.isEmpty()) {
            base = "page";
        }
        String f = base;
        for (int i = 2; !used.add(f); i++) {
            f = base + "-" + i;
        }
        return f;
    }

    private static void deleteQuietly(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (var s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException | RuntimeException ignored) {
            // best effort
        }
    }
}
