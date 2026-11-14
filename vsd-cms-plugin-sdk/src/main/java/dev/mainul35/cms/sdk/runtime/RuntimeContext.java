package dev.mainul35.cms.sdk.runtime;

import java.util.Optional;

/**
 * What a server-side handler sees while processing one event. This replaces the old
 * "client sends a snapshot of props" model: handlers read and write session-owned state.
 */
public interface RuntimeContext {

    String sessionId();

    String siteName();

    /** The page the event came from. */
    String pageName();

    Optional<RuntimeUser> user();

    default boolean isAuthenticated() {
        return user().isPresent();
    }

    RuntimeState state();

    String clientIp();
}
