package dev.mainul35.vsd.runtime.shared;

import dev.mainul35.cms.sdk.runtime.RuntimeUser;
import dev.mainul35.cms.sdk.runtime.StateKey;
import dev.mainul35.cms.sdk.runtime.StatePatch;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import dev.mainul35.vsd.runtime.session.InMemorySessionStore;
import dev.mainul35.vsd.runtime.session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedStateTest {

    private static final String CART = """
            {"name":"cart","description":"The shopping cart","kind":"OBJECT","lifetime":"VISITOR",
             "fields":{"items":{"type":"array"},"total":{"type":"number","required":true},"note":{"type":"string","maxLength":10}},
             "writers":["cart-plugin","checkout-plugin"]}""";
    private static final String DRAFT = """
            {"name":"draft","kind":"OBJECT","lifetime":"LOGIN","fields":{"step":{"type":"integer"}}}""";

    private RuntimeProperties props;
    private SharedStateRegistry registry;
    private SharedStateService service;
    private InMemorySessionStore store;

    @BeforeEach
    void setUp() {
        props = SharedTestSupport.props();
        registry = new SharedStateRegistry(props);
        service = new SharedStateService(registry, props);
        store = new InMemorySessionStore();
    }

    private SharedStateDef declare(String json) {
        return registry.put(SharedStateRegistry.parse(json));
    }

    private static RuntimeUser user(String id) {
        return new RuntimeUser(id, id, id, null, Set.of());
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private void write(Session s, String name, Object value, String writer, boolean client) {
        service.checkWrite(s, SharedStateService.key(name), value, false, writer, client);
    }

    // ---- definitions and values ---------------------------------------------------------------

    @Test
    void aDefinitionGetsAStartingValueBuiltFromItsFields() {
        SharedStateDef d = declare(CART);
        assertEquals(map("items", List.of(), "total", 0, "note", ""), d.getDefaultValue());
    }

    @Test
    void badDefinitionsAreRefusedWithAClearReason() {
        for (String bad : new String[]{
                "{\"name\":\"Cart\",\"fields\":{\"a\":{\"type\":\"string\"}}}",            // uppercase name
                "{\"name\":\"x\",\"kind\":\"OBJECT\",\"fields\":{}}",                        // no fields
                "{\"name\":\"x\",\"fields\":{\"a\":{\"type\":\"date\"}}}",                   // unknown type
                "{\"name\":\"x\",\"fields\":{\"a b\":{\"type\":\"string\"}}}",               // bad field name
                "{\"name\":\"x\",\"fields\":{\"a\":{\"type\":\"string\"}},\"writers\":[\"a b\"]}",
                "{\"name\":\"x\",\"kind\":\"MAP\",\"valueType\":\"string\",\"maxEntries\":5000}",
                "{\"name\":\"x\",\"fields\":{\"a\":{\"type\":\"string\"}},\"defaultValue\":{\"a\":5}}"}) {
            assertThrows(IllegalArgumentException.class, () -> registry.put(SharedStateRegistry.parse(bad)), bad);
        }
        // a misspelled option must not be silently ignored
        assertThrows(IllegalArgumentException.class,
                () -> SharedStateRegistry.parse("{\"name\":\"x\",\"fields\":{\"a\":{\"type\":\"string\"}},\"writter\":[]}"));
    }

    @Test
    void objectValuesMustMatchTheFields() {
        SharedStateDef d = declare(CART);
        SharedStateValidator.validateValue(d, map("items", List.of(1, 2), "total", 9.5, "note", "hi"));
        assertThrows(IllegalArgumentException.class, () -> SharedStateValidator.validateValue(d, map("items", List.of())), "required total");
        assertThrows(IllegalArgumentException.class, () -> SharedStateValidator.validateValue(d, map("total", "9")), "wrong type");
        assertThrows(IllegalArgumentException.class, () -> SharedStateValidator.validateValue(d, map("total", 1, "extra", 1)), "unknown field");
        assertThrows(IllegalArgumentException.class, () -> SharedStateValidator.validateValue(d, map("total", 1, "note", "x".repeat(11))), "too long");
        assertThrows(IllegalArgumentException.class, () -> SharedStateValidator.validateValue(d, "not an object"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> SharedStateValidator.validateValue(d, map("total", "9")));
        assertTrue(e.getMessage().contains("'cart'") && e.getMessage().contains("'total'"), e.getMessage());
    }

    @Test
    void integersAcceptWholeNumbersOnly() {
        SharedStateDef d = declare(DRAFT);
        SharedStateValidator.validateValue(d, map("step", 3));
        SharedStateValidator.validateValue(d, map("step", 3.0));
        assertThrows(IllegalArgumentException.class, () -> SharedStateValidator.validateValue(d, map("step", 3.5)));
    }

    @Test
    void mapValuesAreCheckedByKeyCountKeyNameAndType() {
        SharedStateDef d = declare("{\"name\":\"flags\",\"kind\":\"MAP\",\"valueType\":\"boolean\",\"maxEntries\":2}");
        SharedStateValidator.validateValue(d, map("a", true, "b", false));
        assertThrows(IllegalArgumentException.class, () -> SharedStateValidator.validateValue(d, map("a", true, "b", true, "c", true)));
        assertThrows(IllegalArgumentException.class, () -> SharedStateValidator.validateValue(d, map("a", "yes")));
        assertThrows(IllegalArgumentException.class, () -> SharedStateValidator.validateValue(d, map("bad key", true)));
    }

    // ---- storage ----------------------------------------------------------------------------

    @Test
    void declarationsSurviveARestartAndCanBeDeleted() {
        declare(CART);
        declare(DRAFT);
        SharedStateRegistry reloaded = new SharedStateRegistry(props);
        assertEquals(2, reloaded.all().size());
        assertEquals(List.of("cart-plugin", "checkout-plugin"), reloaded.get("cart").orElseThrow().getWriters());
        assertTrue(reloaded.delete("draft"));
        assertFalse(reloaded.delete("draft"));
        assertEquals(1, new SharedStateRegistry(props).all().size());
    }

    // ---- who may write what -----------------------------------------------------------------

    @Test
    void undeclaredSharedStateIsRefused() {
        Session s = store.create();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> write(s, "nope", map(), "p", true));
        assertTrue(e.getMessage().contains("not declared"), e.getMessage());
    }

    @Test
    void onlyListedPluginsMayWrite() {
        declare(CART);
        Session s = store.create();
        write(s, "cart", map("total", 1), "cart-plugin", true);
        write(s, "cart", map("total", 1), "checkout-plugin", false);
        assertThrows(IllegalArgumentException.class, () -> write(s, "cart", map("total", 1), "other-plugin", true));
        assertThrows(IllegalArgumentException.class, () -> write(s, "cart", map("total", 1), null, true), "browser did not say who it is");
    }

    @Test
    void stateCanBeLimitedToServerSideHandlers() {
        declare("{\"name\":\"locked\",\"fields\":{\"a\":{\"type\":\"string\"}},\"clientWritable\":false}");
        Session s = store.create();
        assertThrows(IllegalArgumentException.class, () -> write(s, "locked", map("a", "x"), "p", true));
        write(s, "locked", map("a", "x"), "p", false);
    }

    @Test
    void loginStateCannotBeWrittenWhileSignedOut() {
        declare(DRAFT);
        Session s = store.create();
        assertThrows(IllegalArgumentException.class, () -> write(s, "draft", map("step", 1), "p", true));
        s.setUser(user("u1"));
        write(s, "draft", map("step", 1), "p", true);
    }

    @Test
    void sharedStateCannotBeRemovedOrPutInOtherScopes() {
        declare(CART);
        Session s = store.create();
        assertThrows(IllegalArgumentException.class,
                () -> service.checkWrite(s, SharedStateService.key("cart"), null, true, "cart-plugin", true));
        assertThrows(IllegalArgumentException.class,
                () -> service.checkWrite(s, new StateKey(dev.mainul35.cms.sdk.runtime.StateScope.PAGE, "shared", "cart"),
                        map("total", 1), false, "cart-plugin", true));
    }

    @Test
    void undeclaredSessionStateIsRefusedInStrictModeAndAllowedWhenLenient() {
        Session s = store.create();
        StateKey free = StateKey.session("myplugin", "thing");
        assertThrows(IllegalArgumentException.class, () -> service.checkWrite(s, free, 1, false, "p", true));
        props.setStrictSharedState(false);
        service.checkWrite(s, free, 1, false, "p", true);
        // plugin-private state is never affected
        props.setStrictSharedState(true);
        service.checkWrite(s, StateKey.plugin("p", "mine"), 1, false, "p", true);
    }

    // ---- lifetimes --------------------------------------------------------------------------

    private Object value(Session s, String name) {
        return s.get(SharedStateService.key(name)).orElse(null);
    }

    @Test
    void newSessionsStartWithVisitorStateOnly() {
        declare(CART);
        declare(DRAFT);
        Session s = store.create();
        service.seed(s);
        assertEquals(map("items", List.of(), "total", 0, "note", ""), value(s, "cart"));
        assertNull(value(s, "draft"), "login state does not exist while signed out");
    }

    @Test
    void signInKeepsVisitorStateAndStartsLoginStateFresh() {
        declare(CART);
        declare(DRAFT);
        Session s = store.create();
        service.seed(s);
        s.apply(List.of(StatePatch.set(SharedStateService.key("cart"), map("items", List.of("a"), "total", 5, "note", ""))));
        s.setUser(user("u1"));
        service.onSignIn(s, false);
        assertEquals(5, ((Map<?, ?>) value(s, "cart")).get("total"), "anonymous cart survives sign-in");
        assertEquals(map("step", 0), value(s, "draft"));
    }

    @Test
    void visitorStateCanAskToResetAtSignIn() {
        declare("{\"name\":\"temp\",\"fields\":{\"n\":{\"type\":\"integer\"}},\"onSignIn\":\"RESET\"}");
        Session s = store.create();
        service.seed(s);
        s.apply(List.of(StatePatch.set(SharedStateService.key("temp"), map("n", 9))));
        s.setUser(user("u1"));
        service.onSignIn(s, false);
        assertEquals(map("n", 0), value(s, "temp"));
    }

    @Test
    void signOutClearsEverythingExceptStateThatSaysKeep() {
        declare(CART);
        declare(DRAFT);
        declare("{\"name\":\"theme\",\"fields\":{\"dark\":{\"type\":\"boolean\"}},\"onSignOut\":\"KEEP\"}");
        Session s = store.create();
        service.seed(s);
        s.setUser(user("u1"));
        service.onSignIn(s, false);
        s.apply(List.of(
                StatePatch.set(SharedStateService.key("cart"), map("items", List.of(), "total", 7, "note", "")),
                StatePatch.set(SharedStateService.key("theme"), map("dark", true)),
                StatePatch.set(SharedStateService.key("draft"), map("step", 2)),
                StatePatch.set(StateKey.plugin("p", "other"), "x")));
        s.setUser(null);
        service.onSignOut(s);
        assertEquals(0, ((Map<?, ?>) value(s, "cart")).get("total"), "cleared back to the starting value");
        assertEquals(map("dark", true), value(s, "theme"), "kept");
        assertNull(value(s, "draft"), "login state is gone");
        assertTrue(s.get(StateKey.plugin("p", "other")).isEmpty(), "everything else is cleared as before");
    }

    @Test
    void anotherAccountOnTheSameSessionStartsClean() {
        declare(CART);
        Session s = store.create();
        service.seed(s);
        s.apply(List.of(StatePatch.set(SharedStateService.key("cart"), map("items", List.of(), "total", 3, "note", ""))));
        service.onSignIn(s, true);
        assertEquals(0, ((Map<?, ?>) value(s, "cart")).get("total"));
    }

    @Test
    void theBrowserOnlySeesWhatItNeeds() {
        declare(CART);
        List<Map<String, Object>> view = service.publicView();
        assertEquals(1, view.size());
        assertEquals(List.of("name", "kind", "lifetime", "default", "clientWritable", "writers"), new ArrayList<>(view.get(0).keySet()));
    }
}
