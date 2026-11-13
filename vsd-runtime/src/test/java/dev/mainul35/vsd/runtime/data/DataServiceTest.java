package dev.mainul35.vsd.runtime.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataServiceTest {

    private static final ObjectMapper M = new ObjectMapper();

    private static SourceSpec spec(String url, String extra) throws Exception {
        return SourceSpec.parse(M.readTree("{\"type\":\"api\",\"endpoint\":\"" + url + "\"" + extra + "}"));
    }

    @Test
    void cachesOnlyWhenTheAuthorAskedForIt() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        DataService svc = new DataService(s -> M.getNodeFactory().numberNode(calls.incrementAndGet()), new RuntimeProperties.Data());

        svc.load(spec("https://a.test/1", ""));
        svc.load(spec("https://a.test/1", ""));
        assertEquals(2, calls.get(), "uncached sources are fetched every time");

        svc.load(spec("https://a.test/2", ",\"cacheTtlMs\":60000"));
        JsonNode again = svc.load(spec("https://a.test/2", ",\"cacheTtlMs\":60000"));
        assertEquals(3, calls.get(), "cached sources are fetched once");
        assertEquals(3, again.asInt());
    }

    @Test
    void cacheEntriesExpire() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        DataService svc = new DataService(s -> M.getNodeFactory().numberNode(calls.incrementAndGet()), new RuntimeProperties.Data());
        SourceSpec s = spec("https://a.test/x", ",\"cacheTtlMs\":1");
        svc.load(s);
        Thread.sleep(20);
        svc.load(s);
        assertEquals(2, calls.get());
    }

    @Test
    void differentUrlsOrHeadersNeverShareACacheEntry() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        DataService svc = new DataService(s -> M.getNodeFactory().numberNode(calls.incrementAndGet()), new RuntimeProperties.Data());
        svc.load(spec("https://a.test/x", ",\"cacheTtlMs\":60000"));
        svc.load(spec("https://a.test/y", ",\"cacheTtlMs\":60000"));
        svc.load(spec("https://a.test/x", ",\"cacheTtlMs\":60000,\"headers\":{\"X-Key\":\"other\"}"));
        assertEquals(3, calls.get());
    }

    @Test
    void visitorsCannotMultiplyLoadOnAnUpstream() throws Exception {
        RuntimeProperties.Data cfg = new RuntimeProperties.Data();
        cfg.setMaxCallsPerSecond(3);
        AtomicInteger calls = new AtomicInteger();
        DataService svc = new DataService(s -> M.getNodeFactory().numberNode(calls.incrementAndGet()), cfg);
        SourceSpec s = spec("https://a.test/hot", "");
        for (int i = 0; i < 3; i++) {
            svc.load(s);
        }
        assertThrows(IllegalStateException.class, () -> svc.load(s));
        assertEquals(3, calls.get());
    }
}
