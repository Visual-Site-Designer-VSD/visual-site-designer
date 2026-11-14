package dev.mainul35.vsd.runtime.shared;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The declared shared state of this runtime. Kept in memory for fast checks and saved to
 * {@code <data-dir>/shared-state.json}, so it survives restarts and republishing a site.
 */
@Component
public class SharedStateRegistry {

    private static final Logger log = LoggerFactory.getLogger(SharedStateRegistry.class);
    private static final int MAX_DEFINITIONS = 100;

    /** Strict on purpose: a misspelled option in the console must be an error, not silently ignored. */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final Path file;
    private final Object writeLock = new Object();
    private volatile Map<String, SharedStateDef> defs = Map.of();

    public SharedStateRegistry(RuntimeProperties props) {
        this.file = props.dataPath().resolve("shared-state.json");
        load();
    }

    public List<SharedStateDef> all() {
        return new ArrayList<>(defs.values());
    }

    public Optional<SharedStateDef> get(String name) {
        return Optional.ofNullable(defs.get(name));
    }

    /** Parses a definition from console JSON. Unknown fields are an error. */
    public static SharedStateDef parse(String json) {
        try {
            return MAPPER.readValue(json, SharedStateDef.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid definition: " + firstLine(e.getMessage()));
        }
    }

    /** Creates or replaces a definition and saves it. */
    public SharedStateDef put(SharedStateDef def) {
        SharedStateValidator.validateDefinition(def);
        synchronized (writeLock) {
            Map<String, SharedStateDef> next = new LinkedHashMap<>(defs);
            if (!next.containsKey(def.getName()) && next.size() >= MAX_DEFINITIONS) {
                throw new IllegalArgumentException("At most " + MAX_DEFINITIONS + " shared states can be declared");
            }
            next.put(def.getName(), def);
            save(next);
            defs = next;
        }
        return def;
    }

    public boolean delete(String name) {
        synchronized (writeLock) {
            if (!defs.containsKey(name)) {
                return false;
            }
            Map<String, SharedStateDef> next = new LinkedHashMap<>(defs);
            next.remove(name);
            save(next);
            defs = next;
            return true;
        }
    }

    // ---- storage ----------------------------------------------------------------------------

    private void load() {
        if (!Files.isRegularFile(file)) {
            log.info("No shared state declared yet ({} does not exist)", file.toAbsolutePath());
            return;
        }
        try {
            List<SharedStateDef> list = MAPPER.readValue(file.toFile(), new TypeReference<>() {
            });
            Map<String, SharedStateDef> loaded = new LinkedHashMap<>();
            for (SharedStateDef d : list) {
                SharedStateValidator.validateDefinition(d);
                if (loaded.put(d.getName(), d) != null) {
                    throw new IllegalArgumentException("duplicate name '" + d.getName() + "'");
                }
            }
            defs = loaded;
            log.info("Loaded {} shared state declaration(s) from {}", loaded.size(), file.toAbsolutePath());
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot read shared state declarations from " + file.toAbsolutePath()
                    + ": " + firstLine(e.getMessage()), e);
        }
    }

    private void save(Map<String, SharedStateDef> map) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), new ArrayList<>(map.values()));
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save shared state declarations: " + firstLine(e.getMessage()), e);
        }
    }

    private static String firstLine(String s) {
        if (s == null) {
            return "unknown error";
        }
        int nl = s.indexOf('\n');
        return nl < 0 ? s : s.substring(0, nl);
    }
}
