package dev.mainul35.vsd.runtime.snapshot;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * An immutable published site: pages (builder PageDefinition JSON, untouched) plus the exact
 * plugin versions the site was published with.
 */
public record SiteSnapshot(
        String name,
        String title,
        String version,
        String defaultPage,
        Map<String, String> plugins,
        List<PageEntry> pages,
        Path assetsDir) {

    public record PageEntry(String name, String path, String title, JsonNode definition) {
    }
}
