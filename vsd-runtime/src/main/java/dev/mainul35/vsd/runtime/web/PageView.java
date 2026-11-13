package dev.mainul35.vsd.runtime.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.mainul35.vsd.runtime.auth.AuthService;
import dev.mainul35.vsd.runtime.data.PageResolver;
import dev.mainul35.vsd.runtime.session.Session;
import dev.mainul35.vsd.runtime.snapshot.SiteSnapshot;
import dev.mainul35.vsd.runtime.util.Json;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** The page definition as one visitor should see it: data fetched, bindings filled in, authoring data removed. */
@Component
public class PageView {

    private static final int MAX_PARAMS = 20;
    private static final int MAX_VALUE_CHARS = 256;

    private final PageResolver resolver;

    public PageView(PageResolver resolver) {
        this.resolver = resolver;
    }

    public JsonNode definition(SiteSnapshot.PageEntry page, Session session, Map<String, String> query) {
        JsonNode user = session.get(AuthService.USER_KEY).map(Json.MAPPER::<JsonNode>valueToTree).orElse(null);
        ObjectNode meta = Json.MAPPER.createObjectNode();
        meta.put("name", page.name());
        meta.put("path", page.path());
        meta.put("title", page.title());
        return resolver.resolve(page.definition(), user, query, meta);
    }

    /** First value of each URL parameter, with sane names and lengths. */
    public static Map<String, String> query(Map<String, String[]> params) {
        Map<String, String> out = new LinkedHashMap<>();
        for (var e : params.entrySet()) {
            if (out.size() >= MAX_PARAMS) {
                break;
            }
            if (e.getKey().matches("[A-Za-z0-9_-]{1,64}") && e.getValue().length > 0) {
                out.put(e.getKey(), truncate(e.getValue()[0]));
            }
        }
        return out;
    }

    /** Parses a raw query string such as {@code a=1&b=x%20y} (with or without a leading {@code ?}). */
    public static Map<String, String> query(String raw) {
        Map<String, String[]> params = new LinkedHashMap<>();
        if (raw != null) {
            String q = raw.startsWith("?") ? raw.substring(1) : raw;
            for (String pair : q.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                try {
                    String k = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
                    String v = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                    params.putIfAbsent(k, new String[]{v});
                } catch (IllegalArgumentException ignored) {
                    // malformed escape: skip the pair
                }
            }
        }
        return query(params);
    }

    private static String truncate(String v) {
        return v.length() > MAX_VALUE_CHARS ? v.substring(0, MAX_VALUE_CHARS) : v;
    }
}
