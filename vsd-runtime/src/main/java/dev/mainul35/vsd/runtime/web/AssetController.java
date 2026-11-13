package dev.mainul35.vsd.runtime.web;

import dev.mainul35.vsd.runtime.plugin.LoadedPlugin;
import dev.mainul35.vsd.runtime.plugin.PluginHost;
import dev.mainul35.vsd.runtime.snapshot.SnapshotService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Serves the browser client, the pinned plugins' bundles and the site's static assets. */
@RestController
public class AssetController {

    private static final Set<String> CLIENT_FILES = Set.of("runtime.js", "runtime.css", "runtime.js.map");

    private final PluginHost plugins;
    private final SnapshotService snapshots;

    public AssetController(PluginHost plugins, SnapshotService snapshots) {
        this.plugins = plugins;
        this.snapshots = snapshots;
    }

    @GetMapping("/_vsd/client/{name:.+}")
    public ResponseEntity<byte[]> client(@PathVariable String name, HttpServletRequest request) throws IOException {
        if (!CLIENT_FILES.contains(name)) {
            return ResponseEntity.notFound().build();
        }
        ClassPathResource res = new ClassPathResource("vsd-client/" + name);
        if (!res.exists()) {
            return ResponseEntity.notFound().build();
        }
        try (InputStream in = res.getInputStream()) {
            return respond(in.readAllBytes(), type(name), CacheControl.noCache(), request);
        }
    }

    @GetMapping("/_vsd/plugins/{id}/{file:bundle\\.(?:js|css)}")
    public ResponseEntity<byte[]> pluginAsset(@PathVariable String id, @PathVariable String file,
                                              @RequestParam(name = "v", required = false) String version,
                                              HttpServletRequest request) {
        // Only plugins the published site pins are reachable.
        if (!snapshots.site().plugins().containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        Optional<LoadedPlugin> lp = plugins.get(id);
        if (lp.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = file.endsWith(".js") ? lp.get().bundleJs().get() : lp.get().bundleCss().get();
        if (bytes == null) {
            return ResponseEntity.notFound().build();
        }
        boolean versioned = lp.get().version().equals(version) && !lp.get().source().startsWith("dev:");
        CacheControl cache = versioned
                ? CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable()
                : CacheControl.noCache();
        return respond(bytes, type(file), cache, request);
    }

    @GetMapping("/assets/**")
    public ResponseEntity<byte[]> siteAsset(HttpServletRequest request) throws IOException {
        Path root = snapshots.site().assetsDir().toAbsolutePath().normalize();
        String rel = request.getRequestURI().substring("/assets/".length());
        Path file = root.resolve(rel).normalize();
        if (rel.isEmpty() || !file.startsWith(root) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        MediaType mt = MediaTypeFactory.getMediaType(file.getFileName().toString()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return respond(Files.readAllBytes(file), mt, CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic(), request, true);
    }

    private static MediaType type(String name) {
        if (name.endsWith(".js")) return MediaType.parseMediaType("text/javascript;charset=UTF-8");
        if (name.endsWith(".css")) return MediaType.parseMediaType("text/css;charset=UTF-8");
        return MediaType.APPLICATION_JSON;
    }

    private static ResponseEntity<byte[]> respond(byte[] bytes, MediaType type, CacheControl cache, HttpServletRequest request) {
        return respond(bytes, type, cache, request, false);
    }

    /** @param sandbox true for authored files: a CSP sandbox keeps any script in them out of the site's origin. */
    private static ResponseEntity<byte[]> respond(byte[] bytes, MediaType type, CacheControl cache,
                                                  HttpServletRequest request, boolean sandbox) {
        String etag = "\"" + Integer.toHexString(java.util.Arrays.hashCode(bytes)) + "-" + bytes.length + "\"";
        if (etag.equals(request.getHeader("If-None-Match"))) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).cacheControl(cache).build();
        }
        ResponseEntity.BodyBuilder b = ResponseEntity.ok().contentType(type).eTag(etag).cacheControl(cache);
        if (sandbox) {
            b.header("Content-Security-Policy", "sandbox");
        }
        return b.body(bytes);
    }
}
