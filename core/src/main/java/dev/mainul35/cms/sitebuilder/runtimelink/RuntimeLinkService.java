package dev.mainul35.cms.sitebuilder.runtimelink;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * The builder's link to the live runtime. Shared state is declared in the runtime console, so the builder
 * asks the runtime which names exist: to offer them to designers and to check pages when they are published.
 *
 * <p>Configure with {@code vsd.runtime-link.url} (where the runtime is reachable from this server) and
 * {@code vsd.runtime-link.admin-token} (the runtime's {@code vsd.runtime.console.admin-token}). Without a URL
 * the link is off and nothing is checked.
 */
@Service
public class RuntimeLinkService {

    private static final Logger log = LoggerFactory.getLogger(RuntimeLinkService.class);
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    private static final long CACHE_MS = 10_000;

    /**
     * What the builder UI needs: whether the runtime answered and, if so, the declarations. The declarations
     * are plain maps and lists, because the web layer's JSON library does not know this module's JsonNode.
     */
    public record Catalog(boolean configured, boolean connected, String message, String consoleUrl,
                          boolean strict, List<Map<String, Object>> definitions) {
    }

    private final String url;
    private final String token;
    private final String consoleUrl;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final Duration timeout;

    private Catalog cached;
    private long cachedAt;

    public RuntimeLinkService(ObjectMapper mapper,
                              @Value("${vsd.runtime-link.url:}") String url,
                              @Value("${vsd.runtime-link.admin-token:}") String token,
                              @Value("${vsd.runtime-link.console-url:}") String consoleUrl,
                              @Value("${vsd.runtime-link.timeout-ms:3000}") long timeoutMs) {
        this.mapper = mapper;
        this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
        this.token = token == null ? "" : token;
        this.consoleUrl = consoleUrl != null && !consoleUrl.isBlank() ? consoleUrl.trim()
                : (this.url.isEmpty() ? "" : this.url + "/_vsd/console/");
        this.timeout = Duration.ofMillis(Math.max(500, timeoutMs));
        this.http = HttpClient.newBuilder().connectTimeout(this.timeout).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    public boolean configured() {
        return !url.isEmpty();
    }

    /** The declared shared state, cached for a few seconds so the builder panel does not hammer the runtime. */
    public synchronized Catalog sharedState() {
        long now = System.currentTimeMillis();
        if (cached != null && now - cachedAt < CACHE_MS) {
            return cached;
        }
        cached = fetch();
        cachedAt = now;
        return cached;
    }

    /** Names of the declared shared state, or empty if the runtime is not configured or did not answer. */
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

    private Catalog fetch() {
        if (!configured()) {
            return new Catalog(false, false, "The runtime link is not configured (vsd.runtime-link.url).", "", false,
                    List.of());
        }
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url + "/_vsd/console/api/shared-state"))
                .timeout(timeout).header("Accept", "application/json").GET();
        if (!token.isEmpty()) {
            req.header("X-VSD-Admin-Token", token);
        }
        try {
            HttpResponse<InputStream> res = http.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = res.body()) {
                byte[] body = in.readNBytes(MAX_RESPONSE_BYTES + 1);
                if (res.statusCode() == 401) {
                    return down("The runtime refused the admin token (vsd.runtime-link.admin-token).");
                }
                if (res.statusCode() != 200 || body.length > MAX_RESPONSE_BYTES) {
                    return down("The runtime answered " + res.statusCode() + ".");
                }
                JsonNode json = mapper.readTree(new String(body, StandardCharsets.UTF_8));
                JsonNode defs = json.path("definitions");
                if (!defs.isArray()) {
                    return down("The runtime answered with an unexpected format.");
                }
                List<Map<String, Object>> list = mapper.convertValue(defs, new TypeReference<>() {
                });
                return new Catalog(true, true, null, consoleUrl, json.path("strict").asBoolean(true), list);
            }
        } catch (IOException e) {
            return down("Could not reach the runtime at " + url + ".");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return down("Interrupted while contacting the runtime.");
        } catch (IllegalArgumentException e) {
            return down("vsd.runtime-link.url is not a valid URL.");
        }
    }

    private Catalog down(String message) {
        return new Catalog(true, false, message, consoleUrl, false, List.of());
    }
}
