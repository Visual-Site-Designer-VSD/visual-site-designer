package dev.mainul35.cms.sitebuilder.livepreview;

import dev.mainul35.cms.sitebuilder.publish.SitePublishService;
import dev.mainul35.cms.sitebuilder.publish.SnapshotBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LivePreviewServiceTest {

    private static final Long OWNER = 7L;
    private static final Long OTHER = 8L;

    @TempDir
    Path tmp;

    private final List<Process> started = new CopyOnWriteArrayList<>();
    private String mode = "";
    private Path fakeJar;
    private Path snapshotDir;
    private SitePublishService publisher;

    @BeforeEach
    void setUp() throws Exception {
        fakeJar = Files.writeString(tmp.resolve("runtime.jar"), "not a real jar");
        publisher = mock(SitePublishService.class);
        snapshotDir = tmp.resolve("work/site/v1");
        when(publisher.previewSnapshot(eq(1L), any())).thenAnswer(inv -> {
            Files.createDirectories(snapshotDir);
            Files.writeString(snapshotDir.resolve("site.json"), "{}");
            return new SnapshotBuilder.Result("v1", snapshotDir, 1, Map.of());
        });
    }

    @AfterEach
    void cleanUp() {
        started.forEach(Process::destroyForcibly);
    }

    /** Starts FakeRuntime instead of the real runtime, on the port the service asked for. */
    private LivePreviewService.Launcher fake() {
        return (command, env, log) -> {
            String port = command.stream().filter(c -> c.startsWith("--server.port=")).findFirst().orElseThrow().substring(14);
            String classes;
            try {
                classes = Path.of(FakeRuntime.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
            } catch (java.net.URISyntaxException e) {
                throw new IOException(e);
            }
            String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            ProcessBuilder pb = new ProcessBuilder(java, "-cp", classes, FakeRuntime.class.getName(), port, mode);
            pb.environment().putAll(env);
            pb.redirectErrorStream(true);
            Files.createDirectories(log.getParent());
            pb.redirectOutput(log.toFile());
            Process p = pb.start();
            started.add(p);
            return p;
        };
    }

    private LivePreviewService service(int idleSeconds) {
        return new LivePreviewService(publisher, true, fakeJar.toString(), tmp.resolve("work"), tmp.resolve("data"),
                tmp.resolve("plugins"), List.of(), idleSeconds, 20, fake());
    }

    private static String get(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
        c.setConnectTimeout(1000);
        c.setReadTimeout(2000);
        try (var in = c.getInputStream()) {
            return new String(in.readAllBytes());
        }
    }

    private static boolean answers(String url) {
        try {
            get(url);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Test
    void startsTheRuntimeForTheSiteAndStopsItWhenAsked() throws Exception {
        LivePreviewService live = service(45);
        LivePreviewService.Info info = live.start(1L, OWNER);

        assertTrue(info.running());
        assertEquals(1L, info.siteId());
        assertTrue(info.url().startsWith("http://127.0.0.1:"), info.url());
        assertEquals("ok", get(info.url()));
        // the console token travels to the runtime through its environment, not its command line
        String token = info.consoleUrl().substring(info.consoleUrl().indexOf("#token=") + 7);
        assertEquals(token, get(info.url() + "token"));
        assertEquals(info.url().substring(0, info.url().length() - 1), live.target().orElseThrow().url());
        assertEquals(token, live.target().orElseThrow().token());

        assertTrue(live.heartbeat(OWNER));
        assertFalse(live.heartbeat(OTHER), "another designer cannot keep it alive");
        assertFalse(live.statusFor(OTHER).running(), "or see how to reach it");
        assertTrue(live.statusFor(OWNER).running());
        assertFalse(live.stop(OTHER));

        assertTrue(live.stop(OWNER));
        assertFalse(live.statusFor(OWNER).running());
        assertTrue(live.target().isEmpty());
        assertFalse(started.get(0).isAlive(), "the process is gone");
        assertFalse(answers(info.url()));
        assertFalse(Files.exists(snapshotDir.getParent()), "the throw-away snapshot is removed");
    }

    @Test
    void anAbandonedPreviewIsStoppedWhenTheBuilderStopsSendingHeartbeats() throws Exception {
        LivePreviewService live = service(1);
        LivePreviewService.Info info = live.start(1L, OWNER);
        live.reap();
        assertTrue(live.statusFor(OWNER).running(), "not yet idle");

        Thread.sleep(1300);
        live.reap();
        assertFalse(live.statusFor(OWNER).running());
        assertFalse(answers(info.url()));
    }

    @Test
    void aHeartbeatKeepsItAlive() throws Exception {
        LivePreviewService live = service(2);
        live.start(1L, OWNER);
        for (int i = 0; i < 3; i++) {
            Thread.sleep(900);
            assertTrue(live.heartbeat(OWNER));
            live.reap();
        }
        assertTrue(live.statusFor(OWNER).running());
        live.stop(OWNER);
    }

    @Test
    void startingAgainReplacesThePreviousPreview() throws Exception {
        LivePreviewService live = service(45);
        LivePreviewService.Info first = live.start(1L, OWNER);
        LivePreviewService.Info second = live.start(1L, OWNER);
        assertNotEquals(first.url(), second.url());
        assertFalse(answers(first.url()), "the old one is stopped");
        assertEquals("ok", get(second.url()));
        live.stop(OWNER);
    }

    @Test
    void reportsWhyTheRuntimeCouldNotStart() {
        mode = "die";
        LivePreviewService live = service(45);
        LivePreviewException e = assertThrows(LivePreviewException.class, () -> live.start(1L, OWNER));
        assertTrue(e.getMessage().contains("stopped while starting"), e.getMessage());
        assertTrue(e.getMessage().contains("boom: cannot start"), "the log of the runtime is shown: " + e.getMessage());
        assertFalse(live.statusFor(OWNER).running());
        assertFalse(Files.exists(snapshotDir.getParent()));
    }

    @Test
    void noticesWhenTheRuntimeEndsOnItsOwn() throws Exception {
        mode = "short";
        LivePreviewService live = service(45);
        live.start(1L, OWNER);
        Thread.sleep(2500);
        assertFalse(live.statusFor(OWNER).running());
        assertTrue(live.target().isEmpty());
    }

    @Test
    void refusesClearlyWhenItCannotRun() {
        LivePreviewService off = new LivePreviewService(publisher, false, fakeJar.toString(), tmp.resolve("work"),
                tmp.resolve("data"), tmp.resolve("plugins"), List.of(), 45, 20, fake());
        assertTrue(assertThrows(LivePreviewException.class, () -> off.start(1L, OWNER)).getMessage().contains("turned off"));

        LivePreviewService noJar = new LivePreviewService(publisher, true, tmp.resolve("missing.jar").toString(),
                tmp.resolve("work"), tmp.resolve("data"), tmp.resolve("plugins"), List.of(), 45, 20, fake());
        assertTrue(assertThrows(LivePreviewException.class, () -> noJar.start(1L, OWNER)).getMessage().contains("missing.jar"));
    }

    @Test
    void theRuntimeIsOnlyReachableFromThisMachineAndGetsTheRightSettings() throws Exception {
        List<String> seen = new ArrayList<>();
        LivePreviewService live = new LivePreviewService(publisher, true, fakeJar.toString(), tmp.resolve("work"),
                tmp.resolve("data"), tmp.resolve("plugins"), List.of("plugins/a", "plugins/b"), 45, 20, (cmd, env, log) -> {
            seen.addAll(cmd);
            return fake().launch(cmd, env, log);
        });
        live.start(1L, OWNER);
        assertTrue(seen.contains("--server.address=127.0.0.1"));
        assertTrue(seen.stream().anyMatch(a -> a.equals("--vsd.runtime.snapshot-dir=" + snapshotDir)), seen.toString());
        assertTrue(seen.stream().anyMatch(a -> a.startsWith("--vsd.runtime.plugins-dir=")));
        assertTrue(seen.stream().anyMatch(a -> a.startsWith("--vsd.runtime.data-dir=")));
        assertTrue(seen.contains("--vsd.runtime.dev-plugin-dirs=plugins/a,plugins/b"));
        assertFalse(seen.stream().anyMatch(a -> a.toLowerCase().contains("token")), "no secret on the command line");
        live.stop(OWNER);
    }
}
