package dev.mainul35.vsd.runtime.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateEngineTest {

    private static final ObjectMapper M = new ObjectMapper();

    private static Scope scope(String json) throws Exception {
        JsonNode root = M.readTree(json);
        Map<String, JsonNode> vars = new java.util.HashMap<>();
        root.fields().forEachRemaining(e -> vars.put(e.getKey(), e.getValue()));
        return new Scope(vars);
    }

    @Test
    void pathsSupportDotsAndIndexes() throws Exception {
        Scope s = scope("{\"a\":{\"b\":[{\"c\":\"deep\"},{\"c\":\"two\"}]}}");
        assertEquals("deep", TemplateEngine.render("{{a.b[0].c}}", s).asText());
        assertEquals("two", TemplateEngine.render("{{ a.b[1].c }}", s).asText());
        assertEquals("x deep y", TemplateEngine.render("x {{a.b[0].c}} y", s).asText());
    }

    @Test
    void missingValuesAreEmptyNeverTheLiteralPlaceholder() throws Exception {
        Scope s = scope("{\"a\":{}}");
        assertEquals("[]", TemplateEngine.render("[{{a.nope}}{{zzz.q}}]", s).asText());
        assertTrue(TemplateEngine.render("{{a.nope}}", s).isNull());
    }

    @Test
    void notPlaceholdersAreLeftAlone() throws Exception {
        Scope s = scope("{\"a\":\"v\"}");
        for (String t : new String[]{"{a}", "{{ }}", "{{a b}}", "{{a.}}", "{{1a}}", "{{a['x']}}", "{{a + 1}}"}) {
            assertEquals(t, TemplateEngine.render(t, s).asText(), t);
        }
    }

    @Test
    void containersAreSplicedAsJson() throws Exception {
        Scope s = scope("{\"a\":{\"k\":[1,2]}}");
        assertEquals("v=[1,2]", TemplateEngine.render("v={{a.k}}", s).asText());
    }

    @Test
    void innerScopesShadowOuterOnes() throws Exception {
        Scope outer = scope("{\"item\":\"outer\",\"x\":\"1\"}");
        Scope inner = outer.with("item", M.getNodeFactory().textNode("inner"));
        assertEquals("inner", TemplateEngine.render("{{item}}", inner).asText());
        assertEquals("1", TemplateEngine.render("{{x}}", inner).asText());
        assertEquals("outer", TemplateEngine.render("{{item}}", outer).asText());
    }

    @Test
    void rootsAreListed() {
        assertEquals(Set.of("auth", "products"), TemplateEngine.roots("{{auth.user.id}} {{products[0].n}} {{auth.isAuthenticated}}"));
    }

    @Test
    void jsonPathsNeverThrow() throws Exception {
        JsonNode n = M.readTree("{\"a\":[1]}");
        assertNull(JsonPaths.get(n, "a[5]"));
        assertNull(JsonPaths.get(n, "a[x]"));
        assertNull(JsonPaths.get(n, "a[1"));
        assertNull(JsonPaths.get(n, "a.b"));
        assertEquals(n, JsonPaths.get(n, ""));
    }
}
