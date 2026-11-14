package dev.mainul35.cms.sitebuilder.livepreview;

import dev.mainul35.cms.sitebuilder.publish.PublishException;
import dev.mainul35.cms.sitebuilder.publish.SitePublishService;
import dev.mainul35.cms.sitebuilder.publish.SnapshotBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Runs the site being designed on the live runtime, on demand.
 *
 * <p>"Run on live server" saves nothing by itself: it builds a throw-away snapshot of the site's saved pages and
 * starts one runtime process for it, bound to this machine only, on a free port, with a random console token.
 * The runtime stops again as soon as the designer closes the preview window (the builder tells us), when the
 * builder stops sending heartbeats (tab closed, browser crashed), or when this server shuts down. Only one live
 * preview runs at a time; starting another replaces it.
 */
@Service
public class LivePreviewService {

    private static final Logger log = LoggerFactory.getLogger(LivePreviewService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    /** What the builder needs to open and manage the preview. */
    public record Info(boolean running, Long siteId, String url, String consoleUrl, int idleTimeoutSeconds) {
        static Info stopped(int idle) {
            return new Info(false, null, null, null, idle);
        }
    }

    /** Where the running preview can be reached, for the shared-state link. */
    public record Target(String url, String token) {
    }

    /** Starts the runtime process. Replaceable so the lifecycle can be tested without the real runtime. */
    @FunctionalInterface
    interface Launcher {
        Process launch(List<String> command, Map<String, String> env, Path logFile) throws IOException;
    }

    private static final class Running {
        final Process process;
        final Long siteId;
        final Long userId;
        final int port;
        final String token;
        final Path snapshotRoot;
        volatile long lastBeat = System.currentTimeMillis();

        Running(Process process, Long siteId, Long userId, int port, String token, Path snapshotRoot) {
            this.process = process;
            this.siteId = siteId;
            this.userId = userId;
            this.port = port;
            this.token = token;
            this.snapshotRoot = snapshotRoot;
        }
    }

    private final SitePublishService publisher;
    private final boolean enabled;
    private final String runtimeJarSetting;
    private final Path workDir;
    private final Path dataDir;
    private final Path pluginDir;
    private final List<String> devPluginDirs;
    private final long idleMillis;
    private final long startupMillis;
    private final Launcher launcher;
    private Running running;

    @Autowired
    public LivePreviewService(SitePublishService publisher,
                              @Value("${vsd.live-preview.enabled:true}") boolean enabled,
                              @Value("${vsd.live-preview.runtime-jar:}") String runtimeJar,
                              @Value("${vsd.live-preview.work-dir:}") String workDir,
                              @Value("${vsd.live-preview.data-dir:data/runtime-data}") String dataDir,
                              @Value("${vsd.live-preview.dev-plugin-dirs:}") String devPluginDirs,
                              @Value("${vsd.live-preview.idle-timeout-seconds:45}") int idleSeconds,
                              @Value("${vsd.live-preview.startup-timeout-seconds:45}") int startupSeconds,
                              @Value("${app.plugin.directory:plugins}") String pluginDir) {
        this(publisher, enabled, runtimeJar,
                workDir == null || workDir.isBlank()
                        ? Path.of(System.getProperty("java.io.tmpdir"), "vsd-live-preview") : Path.of(workDir),
                Path.of(dataDir), Path.of(pluginDir), splitList(devPluginDirs), idleSeconds, startupSeconds,
                LivePreviewService::startProcess);
    }

    LivePreviewService(SitePublishService publisher, boolean enabled, String runtimeJar, Path workDir, Path dataDir,
                       Path pluginDir, List<String> devPluginDirs, int idleSeconds, int startupSeconds, Launcher launcher) {
        this.publisher = publisher;
        this.enabled = enabled;
        this.runtimeJarSetting = runtimeJar == null ? "" : runtimeJar.trim();
        this.workDir = workDir.toAbsolutePath().normalize();
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.pluginDir = pluginDir.toAbsolutePath().normalize();
        this.devPluginDirs = devPluginDirs;
        this.idleMillis = Math.max(1, idleSeconds) * 1000L;
        this.startupMillis = Math.max(5, startupSeconds) * 1000L;
        this.launcher = launcher;
    }

    /** Where the runtime keeps the shared state declarations (also read by the builder when no runtime is up). */
    public Path dataDir() {
        return dataDir;
    }

    public boolean enabled() {
        return enabled;
    }

    // ---- lifecycle --------------------------------------------------------------------------

    /**
     * Builds a snapshot of the site and starts the runtime for it, replacing any preview that is running.
     *
     * @throws LivePreviewException if it could not be started (the message says why)
     */
    public synchronized Info start(Long siteId, Long userId) {
        if (!enabled) {
            throw new LivePreviewException("The live preview is turned off (vsd.live-preview.enabled=false).");
        }
        Path jar = findRuntimeJar();
        stopLocked("replaced by a new preview");

        SnapshotBuilder.Result snapshot;
        try {
            Files.createDirectories(workDir);
            snapshot = publisher.previewSnapshot(siteId, workDir);
        } catch (PublishException e) {
            throw new LivePreviewException(e.getMessage(), e);
        } catch (IOException e) {
            throw new LivePreviewException("Could not prepare " + workDir + ": " + e.getMessage(), e);
        }
        Path snapshotRoot = snapshot.directory().getParent();

        int port = freePort();
        String token = newToken();
        List<String> command = command(jar, port, snapshot.directory());
        Path logFile = workDir.resolve("runtime.log");
        Process process;
        try {
            Files.createDirectories(dataDir);
            process = launcher.launch(command, Map.of("VSD_RUNTIME_CONSOLE_ADMINTOKEN", token), logFile);
        } catch (IOException e) {
            deleteQuietly(snapshotRoot);
            throw new LivePreviewException("Could not start the runtime: " + e.getMessage(), e);
        }

        if (!waitUntilServing(process, port)) {
            boolean exited = !process.isAlive();
            String tail = tail(logFile);
            kill(process);
            deleteQuietly(snapshotRoot);
            throw new LivePreviewException((exited ? "The runtime stopped while starting." : "The runtime did not answer in time.")
                    + (tail.isEmpty() ? "" : " Last log lines:\n" + tail));
        }
        running = new Running(process, siteId, userId, port, token, snapshotRoot);
        log.info("Live preview of site {} is running on port {}", siteId, port);
        return info(running);
    }

    /** The preview as this user may see it: how to reach it if it is theirs, otherwise "stopped". */
    public synchronized Info statusFor(Long userId) {
        dropIfDead();
        return running == null || !running.userId.equals(userId) ? Info.stopped(idleSeconds()) : info(running);
    }

    /** The builder says it still has the preview open. Returns false if nothing of this user's is running. */
    public synchronized boolean heartbeat(Long userId) {
        dropIfDead();
        if (running == null || !running.userId.equals(userId)) {
            return false;
        }
        running.lastBeat = System.currentTimeMillis();
        return true;
    }

    /** Stops the preview if it belongs to this user. Returns true if something was stopped. */
    public synchronized boolean stop(Long userId) {
        if (running == null || !running.userId.equals(userId)) {
            return false;
        }
        stopLocked("closed by the designer");
        return true;
    }

    /** Stops dead or abandoned previews. */
    @Scheduled(fixedDelay = 5000)
    public synchronized void reap() {
        dropIfDead();
        if (running != null && System.currentTimeMillis() - running.lastBeat > idleMillis) {
            stopLocked("the builder stopped sending heartbeats");
        }
    }

    @PreDestroy
    public synchronized void shutdown() {
        stopLocked("the builder server is shutting down");
    }

    /** Address and token of the running preview, for the shared-state link. */
    public synchronized Optional<Target> target() {
        dropIfDead();
        return running == null ? Optional.empty()
                : Optional.of(new Target("http://127.0.0.1:" + running.port, running.token));
    }

    // ---- helpers ----------------------------------------------------------------------------

    private void stopLocked(String why) {
        if (running == null) {
            return;
        }
        Running r = running;
        running = null;
        kill(r.process);
        deleteQuietly(r.snapshotRoot);
        log.info("Live preview of site {} stopped ({})", r.siteId, why);
    }

    private void dropIfDead() {
        if (running != null && !running.process.isAlive()) {
            Running r = running;
            running = null;
            deleteQuietly(r.snapshotRoot);
            log.warn("Live preview of site {} ended on its own", r.siteId);
        }
    }

    private Info info(Running r) {
        String base = "http://127.0.0.1:" + r.port;
        return new Info(true, r.siteId, base + "/", base + "/_vsd/console/#token=" + r.token, idleSeconds());
    }

    private int idleSeconds() {
        return (int) (idleMillis / 1000);
    }

    private List<String> command(Path jar, int port, Path snapshotDir) {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<String> cmd = new ArrayList<>(List.of(java, "-jar", jar.toString(),
                "--server.port=" + port,
                "--server.address=127.0.0.1",
                "--vsd.runtime.snapshot-dir=" + snapshotDir,
                "--vsd.runtime.plugins-dir=" + pluginDir,
                "--vsd.runtime.data-dir=" + dataDir,
                "--vsd.runtime.cookie-secure=false"));
        if (!devPluginDirs.isEmpty()) {
            cmd.add("--vsd.runtime.dev-plugin-dirs=" + String.join(",", devPluginDirs));
        }
        return cmd;
    }

    private Path findRuntimeJar() {
        if (!runtimeJarSetting.isEmpty()) {
            Path p = Path.of(runtimeJarSetting).toAbsolutePath().normalize();
            if (!Files.isRegularFile(p)) {
                throw new LivePreviewException("vsd.live-preview.runtime-jar points to " + p + ", which does not exist.");
            }
            return p;
        }
        for (String base : new String[]{"vsd-runtime/target", "../vsd-runtime/target"}) {
            Path dir = Path.of(base).toAbsolutePath().normalize();
            if (Files.isDirectory(dir)) {
                try (Stream<Path> files = Files.list(dir)) {
                    Optional<Path> jar = files.filter(f -> f.getFileName().toString().matches("vsd-runtime-.*\\.jar"))
                            .filter(f -> !f.getFileName().toString().endsWith(".original"))
                            .max(Comparator.comparing(f -> f.toFile().lastModified()));
                    if (jar.isPresent()) {
                        return jar.get();
                    }
                } catch (IOException ignored) {
                    // try the next place
                }
            }
        }
        throw new LivePreviewException("The live runtime is not built. Run: mvn -pl vsd-runtime -am package -DskipTests"
                + " (or set vsd.live-preview.runtime-jar).");
    }

    private boolean waitUntilServing(Process process, int port) {
        long deadline = System.currentTimeMillis() + startupMillis;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                return false;
            }
            try {
                HttpURLConnection c = (HttpURLConnection) URI.create("http://127.0.0.1:" + port + "/").toURL().openConnection();
                c.setConnectTimeout(500);
                c.setReadTimeout(2000);
                int code = c.getResponseCode();
                c.disconnect();
                if (code > 0) {
                    return true;
                }
            } catch (IOException notYet) {
                // still starting
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static Process startProcess(List<String> command, Map<String, String> env, Path logFile) throws IOException {
        Files.createDirectories(logFile.getParent());
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().putAll(env);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(logFile.toFile()));
        return pb.start();
    }

    private static void kill(Process p) {
        p.destroy();
        try {
            if (!p.waitFor(5, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                p.waitFor(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            p.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    private static int freePort() {
        try (ServerSocket s = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            return s.getLocalPort();
        } catch (IOException e) {
            throw new LivePreviewException("No free port to start the runtime on.", e);
        }
    }

    private static String newToken() {
        byte[] b = new byte[24];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static List<String> splitList(String value) {
        List<String> out = new ArrayList<>();
        if (value != null) {
            for (String v : value.split(",")) {
                if (!v.isBlank()) {
                    out.add(v.trim());
                }
            }
        }
        return out;
    }

    private static String tail(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return String.join("\n", lines.subList(Math.max(0, lines.size() - 15), lines.size()));
        } catch (IOException | RuntimeException e) {
            return "";
        }
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            log.debug("Could not remove {}", dir);
        }
    }
}
