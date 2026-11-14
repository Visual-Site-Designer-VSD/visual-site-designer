package dev.mainul35.cms.sitebuilder.runtimelink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.mainul35.cms.sitebuilder.livepreview.LivePreviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuntimeLinkServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private HttpServer runtime;
    private final AtomicReference<String> seenToken = new AtomicReference<>();
    private volatile int status = 200;

    @BeforeEach
    void start() throws Exception {
        runtime = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        runtime.createContext("/_vsd/console/api/shared-state", ex -> {
            seenToken.set(ex.getRequestHeaders().getFirst("X-VSD-Admin-Token"));
            byte[] body = "{\"strict\":true,\"definitions\":[{\"name\":\"cart\",\"kind\":\"OBJECT\"},{\"name\":\"theme\",\"kind\":\"OBJECT\"}]}"
                    .getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, status == 200 ? body.length : -1);
            if (status == 200) {
                ex.getResponseBody().write(body);
            }
            ex.close();
        });
        runtime.start();
    }

    @AfterEach
    void stop() {
        runtime.stop(0);
    }

    private String url() {
        return "http://127.0.0.1:" + runtime.getAddress().getPort();
    }

    @Test
    void readsTheDeclaredStateAndSendsTheAdminToken() {
        RuntimeLinkService s = new RuntimeLinkService(MAPPER, url() + "/", "secret", "", 2000);
        var c = s.sharedState();
        assertTrue(c.connected());
        assertEquals(2, c.definitions().size());
        assertTrue(c.consoleUrl().endsWith("/_vsd/console/"));
        assertEquals("secret", seenToken.get());
        assertEquals(Set.of("cart", "theme"), s.declaredNames().orElseThrow());
    }

    @Test
    void isOffWithoutAUrlAndNeverGuessesDeclarations() {
        RuntimeLinkService s = new RuntimeLinkService(MAPPER, "", "", "", 2000);
        assertFalse(s.configured());
        assertFalse(s.sharedState().connected());
        assertTrue(s.declaredNames().isEmpty());
    }

    @Test
    void reportsWhyItCouldNotConnect() {
        status = 401;
        var refused = new RuntimeLinkService(MAPPER, url(), "wrong", "", 2000).sharedState();
        assertFalse(refused.connected());
        assertTrue(refused.message().contains("token"), refused.message());

        var unreachable = new RuntimeLinkService(MAPPER, "http://127.0.0.1:1", "", "", 800).sharedState();
        assertFalse(unreachable.connected());
        assertTrue(unreachable.configured());
        assertTrue(unreachable.message().contains("reach"), unreachable.message());
    }

    @TempDir
    Path tmp;

    private LivePreviewService live(Optional<LivePreviewService.Target> target) {
        LivePreviewService live = mock(LivePreviewService.class);
        when(live.target()).thenReturn(target);
        when(live.dataDir()).thenReturn(tmp);
        return live;
    }

    @Test
    void readsTheDeclarationsFileWhileNoRuntimeIsUp() throws Exception {
        RuntimeLinkService none = new RuntimeLinkService(MAPPER, live(Optional.empty()), "", "", "", 2000);
        var empty = none.sharedState();
        assertTrue(empty.connected(), "no file yet means nothing declared, which is a known answer");
        assertEquals("file", empty.source());
        assertTrue(empty.definitions().isEmpty());

        Files.writeString(tmp.resolve("shared-state.json"), "[{\"name\":\"cart\",\"kind\":\"OBJECT\"}]");
        RuntimeLinkService service = new RuntimeLinkService(MAPPER, live(Optional.empty()), "", "", "", 2000);
        assertEquals(Set.of("cart"), service.declaredNames().orElseThrow());

        Files.writeString(tmp.resolve("shared-state.json"), "not json");
        RuntimeLinkService broken = new RuntimeLinkService(MAPPER, live(Optional.empty()), "", "", "", 2000);
        assertFalse(broken.sharedState().connected());
        assertTrue(broken.declaredNames().isEmpty(), "unreadable declarations are not guessed");
    }

    @Test
    void theRunningLivePreviewIsAskedFirst() {
        var target = new LivePreviewService.Target(url(), "live-token");
        RuntimeLinkService service = new RuntimeLinkService(MAPPER, live(Optional.of(target)), "http://127.0.0.1:1", "other", "", 2000);
        var c = service.sharedState();
        assertTrue(c.connected());
        assertEquals("runtime", c.source());
        assertEquals("live-token", seenToken.get());
        assertTrue(c.consoleUrl().endsWith("#token=live-token"), c.consoleUrl());
        assertEquals(Set.of("cart", "theme"), service.declaredNames().orElseThrow());
    }
}
