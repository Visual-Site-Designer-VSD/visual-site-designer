package dev.mainul35.vsd.runtime.snapshot;

import dev.mainul35.vsd.runtime.RuntimeProperties;
import dev.mainul35.vsd.runtime.plugin.PluginHost;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The snapshot must refuse to start when it cannot be served exactly as it was published. */
class SnapshotServiceTest {

    @TempDir
    Path tmp;

    private PluginHost hostWith(String id, String version) throws IOException {
        Path plugin = tmp.resolve("plugin-" + id);
        Files.createDirectories(plugin.resolve("src/main/resources"));
        Files.writeString(plugin.resolve("src/main/resources/plugin.yml"), "plugin-id: " + id + "\nversion: " + version + "\n");
        RuntimeProperties props = new RuntimeProperties();
        props.setPluginsDir(tmp.resolve("no-jars").toString());
        props.setDevPluginDirs(List.of(plugin.toString()));
        return new PluginHost(props);
    }

    private RuntimeProperties snapshot(String pinnedVersion, String pageComponentPlugin) throws IOException {
        Path dir = tmp.resolve("snap");
        Files.createDirectories(dir.resolve("pages"));
        Files.writeString(dir.resolve("site.json"), """
                {"siteName":"t","pages":[{"name":"home","path":"/"}],"plugins":{"label":"%s"}}
                """.formatted(pinnedVersion));
        Files.writeString(dir.resolve("pages/home.json"), """
                {"pageName":"home","components":[{"instanceId":"c1","pluginId":"%s","componentId":"Label","children":[]}]}
                """.formatted(pageComponentPlugin));
        RuntimeProperties props = new RuntimeProperties();
        props.setSnapshotDir(dir.toString());
        return props;
    }

    @Test
    void servesAConsistentSnapshot() throws IOException {
        SnapshotService svc = new SnapshotService(snapshot("1.0.0", "label"), hostWith("label", "1.0.0"));
        assertEquals("home", svc.byPath("/").orElseThrow().name());
        assertEquals("home", svc.byPath("/").orElseThrow().name());
        assertTrue(svc.byPath("/missing").isEmpty());
    }

    @Test
    void refusesAPluginVersionThatDiffersFromTheOnePublished() throws IOException {
        Exception e = assertThrows(IllegalStateException.class,
                () -> new SnapshotService(snapshot("1.0.0", "label"), hostWith("label", "2.0.0")));
        assertTrue(e.getMessage().contains("published with v1.0.0"), e.getMessage());
    }

    @Test
    void refusesAMissingPlugin() throws IOException {
        Exception e = assertThrows(IllegalStateException.class,
                () -> new SnapshotService(snapshot("1.0.0", "label"), hostWith("other", "1.0.0")));
        assertTrue(e.getMessage().contains("not installed"), e.getMessage());
    }

    @Test
    void refusesPagesUsingAPluginTheSnapshotDoesNotPin() throws IOException {
        Exception e = assertThrows(IllegalStateException.class,
                () -> new SnapshotService(snapshot("1.0.0", "sneaky"), hostWith("label", "1.0.0")));
        assertTrue(e.getMessage().contains("does not pin"), e.getMessage());
    }

    @Test
    void trailingSlashesResolveToTheSamePage() throws IOException {
        Path dir = tmp.resolve("snap2");
        Files.createDirectories(dir.resolve("pages"));
        Files.writeString(dir.resolve("site.json"),
                "{\"pages\":[{\"name\":\"about\",\"path\":\"/about\"}],\"plugins\":{}}");
        Files.writeString(dir.resolve("pages/about.json"), "{\"components\":[]}");
        RuntimeProperties props = new RuntimeProperties();
        props.setSnapshotDir(dir.toString());
        props.setPluginsDir(tmp.resolve("none").toString());
        SnapshotService svc = new SnapshotService(props, new PluginHost(props));
        assertEquals("about", svc.byPath("/about/").orElseThrow().name());
    }
}
