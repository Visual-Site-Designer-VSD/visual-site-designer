package dev.mainul35.vsd.runtime.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResolverTest {

    private static final ObjectMapper M = new ObjectMapper();
    private static final JsonNode USER = node("{\"id\":\"u1\",\"displayName\":\"Demo User\",\"roles\":[\"admin\"]}");

    private static JsonNode node(String json) {
        try {
            return M.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static PageResolver resolver(SourceLoader loader) {
        return new PageResolver(loader, Runnable::run, Duration.ofSeconds(2));
    }

    private static JsonNode resolve(PageResolver r, String page, JsonNode user) {
        return r.resolve(node(page), user, Map.of("tab", "news"), node("{\"name\":\"home\",\"path\":\"/\",\"title\":\"Home\"}"));
    }

    private static final SourceLoader NO_API = s -> {
        throw new AssertionError("no API call expected");
    };

    @Test
    void fillsBindingsFromAuthPageAndQuery() {
        JsonNode out = resolve(resolver(NO_API), """
                {"components":[{"instanceId":"a","pluginId":"p","componentId":"L","props":{"text":"x"},
                  "templateBindings":{"text":"Hi {{auth.user.displayName}} on {{page.title}} tab={{query.tab}}"}}]}
                """, USER);
        assertEquals("Hi Demo User on Home tab=news", out.at("/components/0/props/text").asText());
    }

    @Test
    void anonymousVisitorsGetEmptyValuesAndAFalseFlag() {
        PageResolver r = resolver(NO_API);
        JsonNode out = resolve(r, """
                {"components":[{"instanceId":"a","templateBindings":{"t":"[{{auth.user.displayName}}]","f":"{{auth.isAuthenticated}}"}}]}
                """, null);
        assertEquals("[]", out.at("/components/0/props/t").asText());
        assertFalse(out.at("/components/0/props/f").asBoolean(true));
    }

    @Test
    void aWholeValueBindingKeepsItsType() {
        JsonNode out = resolve(resolver(NO_API), """
                {"components":[{"instanceId":"a","templateBindings":{"roles":"{{auth.user.roles}}","who":"{{auth.user}}"}}]}
                """, USER);
        assertTrue(out.at("/components/0/props/roles").isArray());
        assertTrue(out.at("/components/0/props/who").isObject());
    }

    @Test
    void valuesAreNeverRescannedForPlaceholders() {
        JsonNode user = node("{\"displayName\":\"{{auth.user.id}}\",\"id\":\"secret\"}");
        JsonNode out = resolve(resolver(NO_API), """
                {"components":[{"instanceId":"a","templateBindings":{"t":"Hi {{auth.user.displayName}}"}}]}
                """, user);
        assertEquals("Hi {{auth.user.id}}", out.at("/components/0/props/t").asText());
    }

    @Test
    void authoringDataNeverReachesTheOutput() {
        JsonNode out = resolve(resolver(NO_API), """
                {"dataContext":{"dataSources":{"s":{"type":"static","staticData":{"v":1},"headers":{"Authorization":"Bearer SECRET"}}}},
                 "components":[{"instanceId":"a","dataSourceRef":"s","dataSource":{"type":"static","staticData":1},
                   "templateBindings":{"t":"{{s.v}}"},"iteratorConfig":{"dataPath":"x","itemAlias":"i"}}]}
                """, USER);
        String json = out.toString();
        for (String forbidden : new String[]{"dataContext", "dataSource", "templateBindings", "iteratorConfig", "SECRET"}) {
            assertFalse(json.contains(forbidden), forbidden + " leaked: " + json);
        }
    }

    @Test
    void namedStaticSourceWithFieldMappingAndTransform() {
        JsonNode out = resolve(resolver(NO_API), """
                {"dataContext":{"dataSources":{"site":{"type":"STATIC",
                   "staticData":{"meta":{"name":"  vsd "},"n":"42"},
                   "fieldMapping":{"title":{"path":"meta.name","transform":"trim"},"count":{"path":"n","transform":"integer"},
                                   "missing":{"path":"nope","fallback":"dflt"},"short":"meta.name"}}}},
                 "components":[{"instanceId":"a","templateBindings":{"a":"{{site.title}}","b":"{{site.count}}","c":"{{site.missing}}"}}]}
                """, null);
        assertEquals("vsd", out.at("/components/0/props/a").asText());
        assertEquals(42, out.at("/components/0/props/b").asInt());
        assertEquals("dflt", out.at("/components/0/props/c").asText());
    }

    @Test
    void contextSourceReadsFromAuthAndQuery() {
        JsonNode out = resolve(resolver(NO_API), """
                {"components":[{"instanceId":"a","dataSource":{"type":"context","contextKey":"auth.user.id"},
                   "templateBindings":{"t":"{{data}}"}},
                  {"instanceId":"b","dataSource":{"type":"context","contextKey":"query.tab"},"templateBindings":{"t":"{{data}}"}}]}
                """, USER);
        assertEquals("u1", out.at("/components/0/props/t").asText());
        assertEquals("news", out.at("/components/1/props/t").asText());
    }

    @Test
    void apiSourcesAreFetchedOnlyWhenUsedAndOnlyOnce() {
        AtomicInteger calls = new AtomicInteger();
        SourceLoader loader = s -> {
            calls.incrementAndGet();
            return node("{\"items\":[{\"id\":7,\"name\":\"A\"},{\"id\":9,\"name\":\"B\"}]}");
        };
        JsonNode out = resolve(resolver(loader), """
                {"dataContext":{"dataSources":{"api":{"type":"api","endpoint":"https://x.test/a"},
                                               "unused":{"type":"api","endpoint":"https://x.test/b"}}},
                 "components":[{"instanceId":"a","templateBindings":{"t":"{{api.items[1].name}}"}},
                               {"instanceId":"b","templateBindings":{"t":"{{api.items[0].name}}"}}]}
                """, null);
        assertEquals(1, calls.get());
        assertEquals("B", out.at("/components/0/props/t").asText());
        assertEquals("A", out.at("/components/1/props/t").asText());
    }

    @Test
    void aFailingSourceLeavesValuesEmptyAndTheRestOfThePageIntact() {
        SourceLoader loader = s -> {
            throw new java.io.IOException("boom");
        };
        JsonNode out = resolve(resolver(loader), """
                {"dataContext":{"dataSources":{"api":{"type":"api","endpoint":"https://x.test/a"}}},
                 "components":[{"instanceId":"a","props":{"k":"v"},"templateBindings":{"t":"[{{api.x}}]"}},
                               {"instanceId":"b","props":{"k":"ok"}}]}
                """, null);
        assertEquals("[]", out.at("/components/0/props/t").asText());
        assertEquals("v", out.at("/components/0/props/k").asText());
        assertEquals("ok", out.at("/components/1/props/k").asText());
        assertFalse(out.toString().contains("boom"), "error details must not reach the page");
    }

    @Test
    void anInlineSourceScopesToItsComponentAndChildren() {
        JsonNode out = resolve(resolver(NO_API), """
                {"components":[{"instanceId":"box","dataSource":{"type":"static","staticData":{"who":"inner"}},
                   "children":[{"instanceId":"kid","templateBindings":{"t":"{{data.who}}"}}]},
                  {"instanceId":"outside","templateBindings":{"t":"[{{data.who}}]"}}]}
                """, null);
        assertEquals("inner", out.at("/components/0/children/0/props/t").asText());
        assertEquals("[]", out.at("/components/1/props/t").asText());
    }

    @Test
    void iteratorRepeatsTheComponentWithItemAndIndex() {
        JsonNode out = resolve(resolver(NO_API), """
                {"components":[{"instanceId":"card","componentId":"Card",
                   "dataSource":{"type":"static","staticData":{"items":[{"id":"a1","n":"One"},{"id":"b2","n":"Two"}]}},
                   "iteratorConfig":{"dataPath":"items","itemAlias":"p","indexAlias":"i","keyPath":"id"},
                   "templateBindings":{"label":"{{i}}:{{p.n}}"},
                   "children":[{"instanceId":"inner","templateBindings":{"t":"{{p.n}}"}}]}]}
                """, null);
        JsonNode comps = out.get("components");
        assertEquals(2, comps.size());
        assertEquals("card#a1", comps.get(0).get("instanceId").asText());
        assertEquals("0:One", comps.get(0).at("/props/label").asText());
        assertEquals("1:Two", comps.get(1).at("/props/label").asText());
        assertEquals("Two", comps.get(1).at("/children/0/props/t").asText());
        assertEquals("inner", comps.get(1).at("/children/0/instanceId").asText(), "children keep their own ids");
    }

    @Test
    void duplicateIteratorKeysStillGiveUniqueInstanceIds() {
        JsonNode out = resolve(resolver(NO_API), """
                {"components":[{"instanceId":"c","dataSource":{"type":"static","staticData":[{"id":"x"},{"id":"x"},{"id":"bad key!"}]},
                   "iteratorConfig":{"dataPath":"","itemAlias":"it","keyPath":"id"}}]}
                """, null);
        List<String> ids = new ArrayList<>();
        out.get("components").forEach(c -> ids.add(c.get("instanceId").asText()));
        assertEquals(3, ids.size());
        assertEquals(3, ids.stream().distinct().count(), ids.toString());
    }

    @Test
    void emptyIteratorShowsTheEmptyTemplate() {
        JsonNode out = resolve(resolver(NO_API), """
                {"components":[{"instanceId":"c","dataSource":{"type":"static","staticData":{"items":[]}},
                   "iteratorConfig":{"dataPath":"items","itemAlias":"it",
                      "emptyTemplate":[{"instanceId":"none","pluginId":"p","componentId":"Label"}]}}]}
                """, null);
        assertEquals(1, out.get("components").size());
        assertEquals("none", out.at("/components/0/instanceId").asText());
    }

    @Test
    void iterationIsCapped() {
        StringBuilder items = new StringBuilder("[");
        for (int i = 0; i < 500; i++) {
            items.append(i > 0 ? "," : "").append("{\"n\":").append(i).append('}');
        }
        items.append(']');
        JsonNode out = resolve(resolver(NO_API), """
                {"components":[{"instanceId":"c","dataSource":{"type":"static","staticData":%s},
                   "iteratorConfig":{"dataPath":"","itemAlias":"it"}}]}
                """.formatted(items), null);
        assertEquals(200, out.get("components").size());
    }

    @Test
    void reservedAndInvalidSourceNamesAreIgnored() {
        JsonNode out = resolve(resolver(NO_API), """
                {"dataContext":{"dataSources":{"auth":{"type":"static","staticData":{"user":"EVIL"}},"bad name":{"type":"static","staticData":1}}},
                 "components":[{"instanceId":"a","templateBindings":{"t":"{{auth.isAuthenticated}}"}}]}
                """, null);
        assertEquals("false", out.at("/components/0/props/t").asText());
    }

    @Test
    void aSlowSourceDoesNotHoldThePageBeyondTheDeadline() {
        PageResolver r = new PageResolver(s -> {
            Thread.sleep(5_000);
            return node("{\"x\":1}");
        }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor(), Duration.ofMillis(200));
        long t0 = System.nanoTime();
        JsonNode out = resolve(r, """
                {"dataContext":{"dataSources":{"api":{"type":"api","endpoint":"https://x.test/a"}}},
                 "components":[{"instanceId":"a","templateBindings":{"t":"[{{api.x}}]"}}]}
                """, null);
        assertTrue(Duration.ofNanos(System.nanoTime() - t0).toMillis() < 2_000);
        assertEquals("[]", out.at("/components/0/props/t").asText());
    }

    @Test
    void pagesWithoutDataFeaturesPassThroughUnchanged() {
        String page = "{\"pageName\":\"p\",\"globalStyles\":{\"cssVariables\":{\"a\":\"b\"}},"
                + "\"components\":[{\"instanceId\":\"a\",\"props\":{\"x\":\"{{not.a.binding}}\"},\"children\":[{\"instanceId\":\"b\"}]}]}";
        JsonNode out = resolve(resolver(NO_API), page, null);
        assertEquals(node(page), out);
        assertNull(out.get("dataContext"));
    }
}
