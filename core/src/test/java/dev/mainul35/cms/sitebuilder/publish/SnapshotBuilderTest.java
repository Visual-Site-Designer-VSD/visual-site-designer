package dev.mainul35.cms.sitebuilder.publish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.cms.sitebuilder.publish.SnapshotBuilder.PageInput;
import dev.mainul35.cms.sitebuilder.publish.SnapshotBuilder.SiteInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotBuilderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SiteInfo SITE = new SiteInfo("Demo", "Demo Site");
    private static final Map<String, String> INSTALLED = Map.of("label-plugin", "1.2.0", "box-plugin", "3.0.0");

    @TempDir
    Path root;

    private final SnapshotBuilder builder = new SnapshotBuilder(MAPPER);

    private static String page(String... pluginIds) {
        StringBuilder sb = new StringBuilder("{\"pageName\":\"p\",\"components\":[");
        for (int i = 0; i < pluginIds.length; i++) {
            sb.append(i > 0 ? "," : "").append("{\"instanceId\":\"c").append(i)
                    .append("\",\"pluginId\":\"").append(pluginIds[i]).append("\",\"componentId\":\"X\"}");
        }
        return sb.append("]}").toString();
    }

    private static PageInput home(String def) {
        return new PageInput("home", "Home", "Welcome", null, true, def);
    }

    private static PageInput about(String def) {
        return new PageInput("about", "About", null, "/about/", false, def);
    }

    @Test
    void writesSiteJsonAndPagesAndPinsInstalledVersions() throws IOException {
        var result = builder.build(SITE, List.of(home(page("label-plugin")), about(page("box-plugin"))),
                INSTALLED, root, "v1");

        assertEquals(2, result.pages());
        assertEquals(Map.of("box-plugin", "3.0.0", "label-plugin", "1.2.0"), result.plugins());

        JsonNode site = MAPPER.readTree(root.resolve("v1/site.json").toFile());
        assertEquals("Demo", site.get("siteName").asText());
        assertEquals("v1", site.get("snapshotVersion").asText());
        assertEquals("home", site.get("defaultPage").asText());
        assertEquals("1.2.0", site.at("/plugins/label-plugin").asText());
        assertEquals("/", site.at("/pages/0/path").asText());
        assertEquals("/about", site.at("/pages/1/path").asText(), "trailing slash removed");
        assertEquals("About", site.at("/pages/1/title").asText(), "falls back to the page name");
        assertEquals("pages/about.json", site.at("/pages/1/file").asText());
        assertTrue(Files.exists(root.resolve("v1/pages/home.json")));
        assertEquals("v1", Files.readString(root.resolve("latest")));
    }

    @Test
    void findsPluginsInsideNestedChildren() {
        String nested = "{\"components\":[{\"pluginId\":\"box-plugin\",\"children\":"
                + "[{\"pluginId\":\"label-plugin\",\"children\":[]}]}]}";
        var result = builder.build(SITE, List.of(home(nested)), INSTALLED, root, "v1");
        assertEquals(Map.of("box-plugin", "3.0.0", "label-plugin", "1.2.0"), result.plugins());
    }

    @Test
    void refusesAPluginThatIsNotInstalledAndWritesNothing() {
        PublishException e = assertThrows(PublishException.class,
                () -> builder.build(SITE, List.of(home(page("label-plugin", "ghost-plugin"))), INSTALLED, root, "v1"));
        assertTrue(e.getMessage().contains("ghost-plugin"), e.getMessage());
        assertTrue(e.getMessage().contains("'Home'"), "names the page that uses it");
        assertFalse(Files.exists(root.resolve("v1")));
        assertFalse(Files.exists(root.resolve("latest")), "latest untouched on failure");
    }

    @Test
    void aFailedPublishLeavesThePreviousSnapshotAsLatest() throws IOException {
        builder.build(SITE, List.of(home(page("label-plugin"))), INSTALLED, root, "v1");
        assertThrows(PublishException.class,
                () -> builder.build(SITE, List.of(home(page("ghost"))), INSTALLED, root, "v2"));
        assertEquals("v1", Files.readString(root.resolve("latest")));
        assertFalse(Files.exists(root.resolve("v2")));
    }

    @Test
    void refusesDuplicateRoutes() {
        PageInput a = new PageInput("a", "A", null, "/x", false, page());
        PageInput b = new PageInput("b", "B", null, "/x/", false, page());
        PublishException e = assertThrows(PublishException.class,
                () -> builder.build(SITE, List.of(a, b), INSTALLED, root, "v1"));
        assertTrue(e.getMessage().contains("/x"), e.getMessage());
    }

    @Test
    void refusesUnsafeRoutes() {
        for (String bad : new String[]{"/../etc", "/a//b", "/a?b=1", "/a#x", "/a\\b"}) {
            PageInput p = new PageInput("p", "P", null, bad, false, page());
            assertThrows(PublishException.class,
                    () -> builder.build(SITE, List.of(p), INSTALLED, root, "v-" + Math.abs(bad.hashCode())), bad);
        }
    }

    @Test
    void refusesBrokenDefinitions() {
        for (String bad : new String[]{"not json", "{}", "[]", "{\"components\":{}}"}) {
            assertThrows(PublishException.class,
                    () -> builder.build(SITE, List.of(home(bad)), INSTALLED, root, "v-" + Math.abs(bad.hashCode())), bad);
        }
    }

    @Test
    void refusesAnEmptySiteAndAReusedVersion() {
        assertThrows(PublishException.class, () -> builder.build(SITE, List.of(), INSTALLED, root, "v1"));
        builder.build(SITE, List.of(home(page())), INSTALLED, root, "v1");
        assertThrows(PublishException.class, () -> builder.build(SITE, List.of(home(page())), INSTALLED, root, "v1"));
    }

    @Test
    void unsafeSlugsNeverEscapeThePagesDirectory() throws IOException {
        PageInput p = new PageInput("../../evil name", "Evil", null, "/evil", true, page());
        builder.build(SITE, List.of(p), INSTALLED, root, "v1");
        try (var files = Files.list(root.resolve("v1/pages"))) {
            List<String> names = files.map(f -> f.getFileName().toString()).toList();
            assertEquals(List.of("evil-name.json"), names);
        }
    }

    @Test
    void defaultsToTheFirstPageWhenNoneIsTheHomepage() throws IOException {
        builder.build(SITE, List.of(about(page())), INSTALLED, root, "v1");
        JsonNode site = MAPPER.readTree(root.resolve("v1/site.json").toFile());
        assertEquals("about", site.get("defaultPage").asText());
    }
}
