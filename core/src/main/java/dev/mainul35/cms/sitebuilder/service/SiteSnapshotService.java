package dev.mainul35.cms.sitebuilder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.mainul35.cms.plugin.core.PluginAssetService;
import dev.mainul35.cms.plugin.core.PluginManager;
import dev.mainul35.cms.plugin.entity.Plugin;
import dev.mainul35.cms.sitebuilder.dto.PageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates a snapshot of a site for the preview server.
 * Creates page definitions, plugin assets, and plugin JARs
 * as a zip file for deployment to the preview server.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SiteSnapshotService {

    private final PageService pageService;
    private final PageVersionService pageVersionService;
    private final PluginManager pluginManager;
    private final PluginAssetService pluginAssetService;
    private final ObjectMapper objectMapper;

    @Value("${app.plugin.directory:plugins}")
    private String pluginDirectory;

    /**
     * Create a site snapshot and return it as a zip byte array.
     */
    public byte[] createSnapshotZip(Long siteId) throws IOException {
        Path tempDir = Files.createTempDirectory("vsd-snapshot-");
        try {
            createSnapshot(siteId, tempDir);
            return zipDirectory(tempDir);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    /**
     * Create a site snapshot in the given directory.
     *
     * @param siteId  The site to snapshot
     * @param tempDir Target directory for the snapshot
     */
    public void createSnapshot(Long siteId, Path tempDir) throws IOException {
        log.info("Creating site snapshot for site {} in {}", siteId, tempDir);

        // Create directories
        Path pagesDir = tempDir.resolve("pages");
        Path libDir = tempDir.resolve("lib");
        Path staticPluginsDir = tempDir.resolve("static/plugins");
        Files.createDirectories(pagesDir);
        Files.createDirectories(libDir);
        Files.createDirectories(staticPluginsDir);

        // 1. Export page definitions
        List<PageDto> pages = pageService.getPagesBySite(siteId);
        List<Map<String, String>> manifest = new ArrayList<>();
        Set<String> discoveredPluginIds = new HashSet<>();

        for (PageDto page : pages) {
            Optional<String> definitionJson = pageVersionService.getPageDefinition(page.getId());
            if (definitionJson.isEmpty()) {
                log.warn("No page definition found for page: {} ({})", page.getPageName(), page.getId());
                continue;
            }

            // Parse definition to extract component plugin IDs and build the page JSON
            JsonNode definition = objectMapper.readTree(definitionJson.get());
            collectPluginIds(definition, discoveredPluginIds);

            // Build page JSON matching site-runtime's PageDefinition format
            ObjectNode pageJson = objectMapper.createObjectNode();
            pageJson.put("pageName", page.getPageSlug() != null ? page.getPageSlug() : page.getPageName());
            pageJson.put("title", page.getTitle() != null ? page.getTitle() : page.getPageName());
            pageJson.put("description", page.getDescription() != null ? page.getDescription() : "");
            pageJson.put("path", page.getRoutePath() != null ? page.getRoutePath() : "/" + page.getPageSlug());

            // Components from the page definition
            if (definition.has("components")) {
                pageJson.set("components", definition.get("components"));
            } else {
                pageJson.set("components", objectMapper.createArrayNode());
            }

            // Data sources (if any)
            if (definition.has("dataSources")) {
                pageJson.set("dataSources", definition.get("dataSources"));
            } else {
                pageJson.set("dataSources", objectMapper.createObjectNode());
            }

            String pageName = pageJson.get("pageName").asText();
            Path pageFile = pagesDir.resolve(pageName + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(pageFile.toFile(), pageJson);

            manifest.add(Map.of(
                    "pageName", pageName,
                    "title", pageJson.get("title").asText(),
                    "path", pageJson.get("path").asText(),
                    "description", pageJson.has("description") ? pageJson.get("description").asText() : ""
            ));
        }

        // Write manifest
        ObjectNode manifestJson = objectMapper.createObjectNode();
        ArrayNode pagesArray = objectMapper.valueToTree(manifest);
        manifestJson.set("pages", pagesArray);
        ArrayNode pluginsArray = objectMapper.createArrayNode();
        discoveredPluginIds.forEach(pluginsArray::add);
        manifestJson.set("plugins", pluginsArray);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(pagesDir.resolve("_manifest.json").toFile(), manifestJson);

        // 2. Copy plugin frontend assets (JS bundles + CSS)
        List<Plugin> activatedPlugins = pluginManager.getActivatedPlugins();
        for (Plugin plugin : activatedPlugins) {
            String pluginId = plugin.getPluginId();
            Path pluginDir = staticPluginsDir.resolve(pluginId);
            Files.createDirectories(pluginDir);

            // Copy bundle.js
            pluginAssetService.getMainBundle(pluginId).ifPresent(bytes -> {
                try {
                    Files.write(pluginDir.resolve("bundle.js"), bytes);
                } catch (IOException e) {
                    log.warn("Failed to write bundle.js for plugin: {}", pluginId, e);
                }
            });

            // Copy bundle.css
            pluginAssetService.getMainStyles(pluginId).ifPresent(bytes -> {
                try {
                    Files.write(pluginDir.resolve("bundle.css"), bytes);
                } catch (IOException e) {
                    log.warn("Failed to write bundle.css for plugin: {}", pluginId, e);
                }
            });
        }

        // 3. Copy plugin JARs to lib/
        for (Plugin plugin : activatedPlugins) {
            String jarPath = plugin.getJarPath();
            if (jarPath != null) {
                Path sourceJar = Path.of(jarPath);
                if (!sourceJar.isAbsolute()) {
                    sourceJar = Path.of(pluginDirectory).resolve(sourceJar);
                }
                if (Files.exists(sourceJar)) {
                    Path targetJar = libDir.resolve(sourceJar.getFileName());
                    Files.copy(sourceJar, targetJar);
                    log.debug("Copied plugin JAR: {} -> {}", sourceJar, targetJar);
                } else {
                    log.warn("Plugin JAR not found: {} for plugin: {}", sourceJar, plugin.getPluginId());
                }
            }
        }

        log.info("Site snapshot created: {} pages, {} plugins", manifest.size(), activatedPlugins.size());
    }

    /**
     * Recursively collect plugin IDs from the component tree.
     */
    private void collectPluginIds(JsonNode node, Set<String> pluginIds) {
        if (node.has("pluginId")) {
            String pluginId = node.get("pluginId").asText();
            if (pluginId != null && !pluginId.isEmpty()) {
                pluginIds.add(pluginId);
            }
        }
        if (node.has("components") && node.get("components").isArray()) {
            for (JsonNode child : node.get("components")) {
                collectPluginIds(child, pluginIds);
            }
        }
        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                collectPluginIds(child, pluginIds);
            }
        }
    }

    /**
     * Zip the contents of a directory into a byte array.
     */
    private byte[] zipDirectory(Path sourceDir) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String entryName = sourceDir.relativize(file).toString().replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(sourceDir)) {
                        String entryName = sourceDir.relativize(dir).toString().replace('\\', '/') + "/";
                        zos.putNextEntry(new ZipEntry(entryName));
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return baos.toByteArray();
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
