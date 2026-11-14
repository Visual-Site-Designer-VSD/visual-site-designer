package dev.mainul35.cms.sitebuilder.runtimelink;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.cms.sitebuilder.livepreview.LivePreviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * The builder's link to the live runtime. Shared state is declared in the runtime console, so the builder
 * asks where the declarations are: to offer them to designers and to check pages when they are published.
 *
 * <p>The declarations come from, in this order: the live preview runtime if one is running; the runtime named
 * by {@code vsd.runtime-link.url} (with {@code vsd.runtime-link.admin-token}); the declarations file the runtime
 * keeps in its data directory, so the names are still known while no runtime is up.
 */
@Service
public class RuntimeLinkService {

    private static final Logger log = LoggerFactory.getLogger(RuntimeLinkService.class);
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    private static final long CACHE_MS = 10_000;

    /**
     * What the builder UI needs: whether the declarations are known and, if so, what they are. {@code source} says
     * where they came from ({@code runtime}, {@code file} or {@code none}). The declarations are plain maps and lists,
     * because the web layer's JSON library does not know this module's JsonNode.
     */
    public record Catalog(boolean configured, boolean connected, String message, String consoleUrl,
                          boolean strict, List<Map<String, Object>> definitions, String source) {
    }

    private final String url;
    private final String token;
    private final String consoleUrl;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final Duration timeout;
    private final LivePreviewService live;

    private Catalog cached;
    private String cachedKey;
    private long cachedAt;

    @Autowired
    public RuntimeLinkService(ObjectMapper mapper,
                              // Lazy: publishing needs this service, and the live preview needs publishing.
                              @Lazy LivePreviewService live,
                              @Value("${vsd.runtime-link.url:}") String url,
                              @Value("${vsd.runtime-link.admin-token:}") String token,
                              @Value("${vsd.runtime-link.console-url:}") String consoleUrl,
                              @Value("${vsd.runtime-link.timeout-ms:3000}") long timeoutMs) {
        this.mapper = mapper;
        this.live = live;
        this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
        this.token = token == null ? "" : token;
        this.consoleUrl = consoleUrl != null && !consoleUrl.isBlank() ? consoleUrl.trim()
                : (this.url.isEmpty() ? "" : this.url + "/_vsd/console/");
        this.timeout = Duration.ofMillis(Math.max(500, timeoutMs));
        this.http = HttpClient.newBuilder().connectTimeout(this.timeout).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    /** Only the configured runtime, no live preview and no declarations file. */
    RuntimeLinkService(ObjectMapper mapper, String url, String token, String consoleUrl, long timeoutMs) {
        this(mapper, null, url, token, consoleUrl, timeoutMs);
    }

    /** True if some source of declarations exists: a configured runtime, or the live preview with its data directory. */
    public boolean configured() {
        return !url.isEmpty() || live != null;
    }

    /** The declared shared state, cached for a few seconds so the builder panel does not hammer the runtime. */
    public synchronized Catalog sharedState() {
        Optional<LivePreviewService.Target> running = live == null ? Optional.empty() : live.target();
        String key = running.map(t -> "live:" + t.url()).orElse(!url.isEmpty() ? "url:" + url : "file");
        long now = System.currentTimeMillis();
        if (cached != null && key.equals(cachedKey) && now - cachedAt < CACHE_MS) {
            return cached;
        }
        if (running.isPresent()) {
            String base = running.get().url();
            cached = fetch(base, running.get().token(), base + "/_vsd/console/#token=" + running.get().token());
        } else if (!url.isEmpty()) {
            cached = fetch(url, token, consoleUrl);
        } else {
            cached = fromFile();
        }
        cachedKey = key;
        cachedAt = now;
        return cached;
    }

    /** Names of the declared shared state, or empty if they are not known. */
    public Optional<Set<String>> declaredNames() {
        Catalog c = sharedState();
        if (!c.connected()) {
            if (c.configured()) {
                log.warn("Publishing without checking shared state: {}", c.message());
            }
            return Optional.empty();
        }
        Set<String> names = new TreeSet<>();
        c.definitions().forEach(d -> names.add(String.valueOf(d.get("name"))));
        return Optional.of(names);
    }

    // ---- sources ----------------------------------------------------------------------------

    private Catalog fetch(String base, String adminToken, String console) {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(base + "/_vsd/console/api/shared-state"))
                .timeout(timeout).header("Accept", "application/json").GET();
        if (!adminToken.isEmpty()) {
            req.header("X-VSD-Admin-Token", adminToken);
        }
        try {
            HttpResponse<InputStream> res = http.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = res.body()) {
                byte[] body = in.readNBytes(MAX_RESPONSE_BYTES + 1);
                if (res.statusCode() == 401) {
                    return down("The runtime refused the admin token (vsd.runtime-link.admin-token).", console);
                }
                if (res.statusCode() != 200 || body.length > MAX_RESPONSE_BYTES) {
                    return down("The runtime answered " + res.statusCode() + ".", console);
                }
                JsonNode json = mapper.readTree(new String(body, StandardCharsets.UTF_8));
                JsonNode defs = json.path("definitions");
                if (!defs.isArray()) {
                    return down("The runtime answered with an unexpected format.", console);
                }
                List<Map<String, Object>> list = mapper.convertValue(defs, new TypeReference<>() {
                });
                return new Catalog(true, true, null, console, json.path("strict").asBoolean(true), list, "runtime");
            }
        } catch (IOException e) {
            return down("Could not reach the runtime at " + base + ".", console);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return down("Interrupted while contacting the runtime.", console);
        } catch (IllegalArgumentException e) {
            return down("vsd.runtime-link.url is not a valid URL.", console);
        }
    }

    /** The runtime's own declarations file, so the names are known even while the runtime is not running. */
    private Catalog fromFile() {
        if (live == null) {
            return new Catalog(false, false, "The runtime link is not configured (vsd.runtime-link.url).", "", false,
                    List.of(), "none");
        }
        Path file = live.dataDir().resolve("shared-state.json");
        if (!Files.isRegularFile(file)) {
            return new Catalog(true, true, null, "", true, List.of(), "file");
        }
        try {
            List<Map<String, Object>> list = mapper.readValue(file.toFile(), new TypeReference<>() {
            });
            return new Catalog(true, true, null, "", true, list, "file");
        } catch (IOException e) {
            return new Catalog(true, false, "Could not read " + file + ".", "", false, List.of(), "file");
        }
    }

    private Catalog down(String message, String console) {
        return new Catalog(true, false, message, console, false, List.of(), "runtime");
    }
}
