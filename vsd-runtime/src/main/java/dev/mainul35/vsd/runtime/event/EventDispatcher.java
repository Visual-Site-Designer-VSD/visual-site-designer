package dev.mainul35.vsd.runtime.event;

import dev.mainul35.cms.sdk.runtime.RuntimeContext;
import dev.mainul35.cms.sdk.runtime.RuntimeEvent;
import dev.mainul35.cms.sdk.runtime.RuntimeEventHandler;
import dev.mainul35.cms.sdk.runtime.RuntimeResult;
import dev.mainul35.cms.sdk.runtime.RuntimeState;
import dev.mainul35.cms.sdk.runtime.RuntimeUser;
import dev.mainul35.cms.sdk.runtime.StatePatch;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import dev.mainul35.vsd.runtime.plugin.LoadedPlugin;
import dev.mainul35.vsd.runtime.plugin.PluginHost;
import dev.mainul35.vsd.runtime.session.Session;
import dev.mainul35.vsd.runtime.shared.SharedStateService;
import dev.mainul35.vsd.runtime.snapshot.SnapshotService;
import dev.mainul35.vsd.runtime.util.Json;
import dev.mainul35.vsd.runtime.util.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Routes client events to server-side handlers.
 *
 * <p>Guarantees: events of one session run one at a time; a retried {@code eventId} gets the
 * original answer instead of running twice; handler state writes are all-or-nothing; only
 * plugins pinned by the published site can receive events.
 */
@Service
public class EventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EventDispatcher.class);

    /** HTTP status plus the JSON body to send. */
    public record Response(int status, String json) {
    }

    private final Map<String, RuntimeEventHandler> handlers = new HashMap<>();
    private final SnapshotService snapshots;
    private final RuntimeProperties props;
    private final RateLimiter limiter;
    private final SharedStateService shared;

    public EventDispatcher(PluginHost host, List<RuntimeEventHandler> beans, SnapshotService snapshots, RuntimeProperties props,
                           SharedStateService shared) {
        this.shared = shared;
        this.snapshots = snapshots;
        this.props = props;
        this.limiter = new RateLimiter(props.getMaxEventsPerSecond(), 1000);
        for (LoadedPlugin p : host.all()) {
            p.handlers().forEach(this::register);
        }
        beans.forEach(this::register);
        log.info("Event dispatcher routes {} handler key(s)", handlers.size());
    }

    private void register(RuntimeEventHandler h) {
        for (String type : h.eventTypes()) {
            String key = key(h.pluginId(), type);
            if (handlers.putIfAbsent(key, h) != null) {
                throw new IllegalStateException("Two handlers registered for " + h.pluginId() + ":" + type);
            }
        }
    }

    private static String key(String pluginId, String type) {
        return pluginId + "\u0000" + type;
    }

    @SuppressWarnings("unchecked")
    public Response dispatch(Session session, Map<String, Object> body, String clientIp) {
        String eventId = str(body, "eventId");
        String type = str(body, "type");
        String pluginId = str(body, "pluginId");
        String page = str(body, "page");
        if (eventId == null || eventId.length() > 64 || type == null || type.length() > 64 || pluginId == null) {
            return error(400, "Invalid event");
        }
        if (!snapshots.site().plugins().containsKey(pluginId)) {
            return error(404, "Unknown plugin");
        }
        if (page != null && snapshots.byName(page).isEmpty()) {
            return error(400, "Unknown page");
        }
        RuntimeEventHandler handler = handlers.get(key(pluginId, type));
        if (handler == null) {
            return error(404, "No handler for " + pluginId + ":" + type);
        }
        if (!limiter.tryAcquire(session.id())) {
            return error(429, "Too many events");
        }
        Object payload = body.get("payload");
        RuntimeEvent event = new RuntimeEvent(eventId, type, pluginId, str(body, "componentId"), str(body, "instanceId"),
                page, payload instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of());

        session.lock().lock();
        try {
            String cached = session.cachedResponse(eventId);
            if (cached != null) {
                return new Response(200, cached);
            }
            EventState state = new EventState(session, props.getMaxValueBytes(), props.getMaxStateKeys(), shared, pluginId);
            RuntimeResult result;
            try {
                result = handler.handle(new Ctx(session, snapshots.site().name(), page, state, clientIp), event);
            } catch (IllegalArgumentException e) {
                result = RuntimeResult.failure(e.getMessage());
            } catch (Exception e) {
                log.error("Handler {}:{} failed", pluginId, type, e);
                result = RuntimeResult.failure("Event handler error");
            }
            boolean success = result.status() == RuntimeResult.Status.SUCCESS;
            List<StatePatch> applied = success ? session.apply(state.pending()) : List.of();

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("status", success ? "success" : "failure");
            out.put("message", result.message());
            out.put("data", result.data());
            out.put("errors", result.errors());
            out.put("patches", applied.stream().map(Session::toWire).toList());
            out.put("rev", session.rev());
            String json = Json.write(out);
            if (success) {
                session.cacheResponse(eventId, json);
            }
            return new Response(200, json);
        } finally {
            session.lock().unlock();
        }
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof String s && !s.isBlank() ? s : null;
    }

    public static Response error(int status, String message) {
        return new Response(status, Json.write(Map.of("status", "failure", "message", message)));
    }

    private record Ctx(Session session, String site, String page, RuntimeState state, String ip) implements RuntimeContext {
        @Override public String sessionId() { return session.id(); }
        @Override public String siteName() { return site; }
        @Override public String pageName() { return page == null ? "" : page; }
        @Override public Optional<RuntimeUser> user() { return session.user(); }
        @Override public RuntimeState state() { return state; }
        @Override public String clientIp() { return ip; }
    }
}
