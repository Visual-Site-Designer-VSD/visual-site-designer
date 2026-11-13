package dev.mainul35.vsd.runtime.plugin;

import dev.mainul35.cms.sdk.runtime.RuntimeEventHandler;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Loads plugins for the runtime: their browser bundles and their server-side
 * {@link RuntimeEventHandler}s. Each plugin gets its own class loader whose parent is the
 * runtime (so the SDK is shared and plugins stay isolated from each other).
 *
 * <p>Sources, in this order: JARs in {@code vsd.runtime.plugins-dir}, then plugin module
 * roots in {@code vsd.runtime.dev-plugin-dirs} (resources and classes read from the
 * source tree, for development).
 */
@Component
public class PluginHost {

    private static final Logger log = LoggerFactory.getLogger(PluginHost.class);

    private final Map<String, LoadedPlugin> plugins = new LinkedHashMap<>();

    public PluginHost(RuntimeProperties props) {
        Path dir = props.pluginsPath();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> jars = Files.list(dir)) {
                jars.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().forEach(this::loadJar);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot scan plugins dir " + dir, e);
            }
        } else {
            log.info("No plugins directory at {}", dir.toAbsolutePath());
        }
        for (Path root : props.devPluginPaths()) {
            loadDevDir(root);
        }
        log.info("Plugin host ready with {} plugin(s): {}", plugins.size(), plugins.keySet());
    }

    public Optional<LoadedPlugin> get(String id) {
        return Optional.ofNullable(plugins.get(id));
    }

    public Collection<LoadedPlugin> all() {
        return plugins.values();
    }

    // ---- loading -----------------------------------------------------------------------------

    private void loadJar(Path jar) {
        try {
            String id;
            String version;
            byte[] js;
            byte[] css;
            try (JarFile jf = new JarFile(jar.toFile())) {
                var yml = jf.getEntry("plugin.yml");
                if (yml == null) {
                    log.warn("Skipping {}: no plugin.yml", jar.getFileName());
                    return;
                }
                Map<String, Object> meta;
                try (InputStream in = jf.getInputStream(yml)) {
                    meta = new Yaml().load(in);
                }
                id = String.valueOf(meta.get("plugin-id"));
                version = String.valueOf(meta.get("version"));
                js = readEntry(jf, "frontend/bundle.js");
                css = readEntry(jf, "frontend/bundle.css");
                if (css == null) {
                    // Some plugin builds name the stylesheet after the plugin instead.
                    var other = jf.stream()
                            .filter(e -> e.getName().startsWith("frontend/") && e.getName().endsWith(".css"))
                            .findFirst();
                    if (other.isPresent()) {
                        css = readEntry(jf, other.get().getName());
                    }
                }
            }
            URLClassLoader loader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, getClass().getClassLoader());
            final byte[] jsBytes = js;
            final byte[] cssBytes = css;
            register(new LoadedPlugin(id, version, () -> jsBytes, () -> cssBytes, handlers(id, loader), jar.getFileName().toString()));
        } catch (Exception e) {
            log.error("Failed to load plugin JAR {}", jar, e);
        }
    }

    private void loadDevDir(Path root) {
        try {
            Path resources = root.resolve("src/main/resources");
            Path ymlFile = resources.resolve("plugin.yml");
            if (!Files.isRegularFile(ymlFile)) {
                log.warn("Skipping dev plugin {}: no plugin.yml", root);
                return;
            }
            Map<String, Object> meta;
            try (InputStream in = Files.newInputStream(ymlFile)) {
                meta = new Yaml().load(in);
            }
            String id = String.valueOf(meta.get("plugin-id"));
            String version = String.valueOf(meta.get("version"));
            Path classes = root.resolve("target/classes");
            URL[] urls = Files.isDirectory(classes) ? new URL[]{classes.toUri().toURL()} : new URL[0];
            URLClassLoader loader = new URLClassLoader(urls, getClass().getClassLoader());
            Path js = resources.resolve("frontend/bundle.js");
            Path frontendDir = resources.resolve("frontend");
            register(new LoadedPlugin(id, version, () -> readFile(js), () -> readFile(findCss(frontendDir)), handlers(id, loader),
                    "dev:" + root.getFileName()));
        } catch (Exception e) {
            log.error("Failed to load dev plugin {}", root, e);
        }
    }

    private void register(LoadedPlugin plugin) {
        if (plugins.containsKey(plugin.id())) {
            log.warn("Duplicate plugin id {} ({}); keeping {}", plugin.id(), plugin.source(), plugins.get(plugin.id()).source());
            return;
        }
        plugins.put(plugin.id(), plugin);
        log.info("Loaded plugin {} v{} from {} ({} handler(s))", plugin.id(), plugin.version(), plugin.source(), plugin.handlers().size());
    }

    /** Handlers declared by the plugin itself; a handler claiming another plugin's id is rejected. */
    private List<RuntimeEventHandler> handlers(String pluginId, ClassLoader loader) {
        List<RuntimeEventHandler> out = new ArrayList<>();
        for (RuntimeEventHandler h : ServiceLoader.load(RuntimeEventHandler.class, loader)) {
            if (h.getClass().getClassLoader() != loader) {
                continue; // provider from the runtime's own class path, not this plugin
            }
            if (!pluginId.equals(h.pluginId())) {
                log.error("Plugin {} declares a handler for plugin {}; ignored", pluginId, h.pluginId());
                continue;
            }
            out.add(h);
        }
        return out;
    }

    private static byte[] readEntry(JarFile jf, String name) throws IOException {
        var entry = jf.getEntry(name);
        if (entry == null) {
            return null;
        }
        try (InputStream in = jf.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }

    /** bundle.css if present, otherwise the first stylesheet in the plugin's frontend directory. */
    private static Path findCss(Path frontendDir) {
        Path preferred = frontendDir.resolve("bundle.css");
        if (Files.isRegularFile(preferred)) {
            return preferred;
        }
        if (!Files.isDirectory(frontendDir)) {
            return preferred;
        }
        try (Stream<Path> files = Files.list(frontendDir)) {
            return files.filter(f -> f.getFileName().toString().endsWith(".css")).sorted().findFirst().orElse(preferred);
        } catch (IOException e) {
            return preferred;
        }
    }

    private static byte[] readFile(Path p) {
        try {
            return Files.isRegularFile(p) ? Files.readAllBytes(p) : null;
        } catch (IOException e) {
            return null;
        }
    }
}
