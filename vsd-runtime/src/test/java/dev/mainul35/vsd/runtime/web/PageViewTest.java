package dev.mainul35.vsd.runtime.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.cms.sdk.runtime.StatePatch;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import dev.mainul35.vsd.runtime.data.PageResolver;
import dev.mainul35.vsd.runtime.session.InMemorySessionStore;
import dev.mainul35.vsd.runtime.session.Session;
import dev.mainul35.vsd.runtime.shared.SharedStateRegistry;
import dev.mainul35.vsd.runtime.shared.SharedStateService;
import dev.mainul35.vsd.runtime.shared.SharedTestSupport;
import dev.mainul35.vsd.runtime.snapshot.SiteSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageViewTest {

    private static final ObjectMapper M = new ObjectMapper();

    private static JsonNode node(String json) {
        try {
            return M.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static final String PAGE = """
            {"components":[{"instanceId":"a","templateBindings":{"text":"Cart: {{shared.cart.total}}"}},
                           {"instanceId":"b","templateBindings":{"text":"{{ shared.theme.dark }} {{auth.isAuthenticated}}"}}]}
            """;

    private final RuntimeProperties props = SharedTestSupport.props();
    private final SharedStateRegistry registry = new SharedStateRegistry(props);
    private final SharedStateService shared = new SharedStateService(registry, props);
    private final PageView view = new PageView(new PageResolver(s -> {
        throw new AssertionError("no API call expected");
    }, Runnable::run, Duration.ofSeconds(2)), shared);
    private final SiteSnapshot.PageEntry page = new SiteSnapshot.PageEntry("home", "/", "Home", node(PAGE));

    @Test
    void reportsWhichSharedStateThePageReads() {
        assertEquals(List.of("cart", "theme"), view.usedShared(page));
        SiteSnapshot.PageEntry plain = new SiteSnapshot.PageEntry("p", "/plain", "Plain", node("{\"components\":[]}"));
        assertEquals(List.of(), view.usedShared(plain));
    }

    @Test
    void bindingsSeeTheVisitorsOwnValuesOrTheStartingValues() {
        registry.put(SharedStateRegistry.parse(
                "{\"name\":\"cart\",\"fields\":{\"total\":{\"type\":\"number\"},\"items\":{\"type\":\"array\"}}}"));
        Session mine = new InMemorySessionStore().create();
        Session other = new InMemorySessionStore().create();

        // before any value exists, the starting value shows
        assertEquals("Cart: 0", view.definition(page, mine, Map.of()).at("/components/0/props/text").asText());

        mine.apply(List.of(StatePatch.set(SharedStateService.key("cart"), Map.of("total", 7, "items", List.of("x")))));
        JsonNode mineOut = view.definition(page, mine, Map.of());
        JsonNode otherOut = view.definition(page, other, Map.of());
        assertEquals("Cart: 7", mineOut.at("/components/0/props/text").asText());
        assertEquals("Cart: 0", otherOut.at("/components/0/props/text").asText(), "another visitor's state is never shown");
        // state that is not declared (theme) is simply empty, and other roots still work next to it
        assertEquals("false", mineOut.at("/components/1/props/text").asText().trim());
    }
}
