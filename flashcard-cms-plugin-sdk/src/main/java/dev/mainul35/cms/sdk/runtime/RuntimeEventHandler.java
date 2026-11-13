package dev.mainul35.cms.sdk.runtime;

import java.util.Set;

/**
 * Server-side handler for runtime events. Plugin JARs register implementations through
 * {@link java.util.ServiceLoader}
 * ({@code META-INF/services/dev.mainul35.cms.sdk.runtime.RuntimeEventHandler}), and the
 * runtime also picks up Spring beans of this type.
 *
 * <p>Handlers must be stateless: anything that has to survive between events belongs in
 * {@link RuntimeContext#state()}.
 */
public interface RuntimeEventHandler {

    /** Plugin that owns the handler. Events are routed on {@code pluginId + eventType}. */
    String pluginId();

    /** Event types handled, for example {@code "subscribe"}. */
    Set<String> eventTypes();

    RuntimeResult handle(RuntimeContext context, RuntimeEvent event) throws Exception;
}
