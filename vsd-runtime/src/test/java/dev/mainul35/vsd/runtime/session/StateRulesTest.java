package dev.mainul35.vsd.runtime.session;

import dev.mainul35.cms.sdk.runtime.StateKey;
import dev.mainul35.cms.sdk.runtime.StateScope;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StateRulesTest {

    @Test
    void parsesScopeCaseInsensitively() {
        StateKey k = StateRules.parse("plugin", "my-plugin", "subscription", false);
        assertEquals(StateScope.PLUGIN, k.scope());
    }

    @Test
    void clientsCannotUseReservedOwners() {
        assertThrows(IllegalArgumentException.class, () -> StateRules.parse("SESSION", "$auth", "user", false));
        assertDoesNotThrow(() -> StateRules.parse("SESSION", "$auth", "user", true), "server code may");
    }

    @Test
    void rejectsSeparatorsAndOddNamesThatWouldMakeWireIdsAmbiguous() {
        for (String bad : new String[]{"a/b", "a|b", "", "a b", "x".repeat(129), "a\nb"}) {
            assertThrows(IllegalArgumentException.class, () -> StateRules.parse("SESSION", bad, "k", false), bad);
            assertThrows(IllegalArgumentException.class, () -> StateRules.parse("SESSION", "o", bad, false), bad);
        }
    }

    @Test
    void rejectsUnknownScope() {
        assertThrows(IllegalArgumentException.class, () -> StateRules.parse("GLOBAL", "o", "k", false));
    }

    @Test
    void normalizesToPlainJsonAndEnforcesSize() {
        record Pojo(String name, int n) {
        }
        Object tree = StateRules.normalize(new Pojo("x", 2), 1024);
        assertEquals(Map.of("name", "x", "n", 2), tree);
        assertThrows(IllegalArgumentException.class, () -> StateRules.normalize("x".repeat(2000), 1024));
    }
}
