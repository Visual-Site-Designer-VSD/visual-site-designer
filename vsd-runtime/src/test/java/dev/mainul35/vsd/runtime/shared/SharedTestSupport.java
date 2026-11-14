package dev.mainul35.vsd.runtime.shared;

import dev.mainul35.vsd.runtime.RuntimeProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Builds registries and services that keep their files in a throw-away directory. */
public final class SharedTestSupport {

    private SharedTestSupport() {
    }

    public static RuntimeProperties props() {
        try {
            RuntimeProperties p = new RuntimeProperties();
            Path dir = Files.createTempDirectory("vsd-shared-state");
            p.setDataDir(dir.toString());
            dir.toFile().deleteOnExit();
            return p;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static SharedStateService service(RuntimeProperties props) {
        return new SharedStateService(new SharedStateRegistry(props), props);
    }

    /** A service with nothing declared and strict mode off: behaves like the runtime did before shared state. */
    public static SharedStateService lenient() {
        RuntimeProperties p = props();
        p.setStrictSharedState(false);
        return service(p);
    }
}
