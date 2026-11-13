package dev.mainul35.vsd.runtime.session;

import java.util.Collection;
import java.util.Optional;

/**
 * Where sessions live. Only an in-memory implementation exists today. A shared store (Redis)
 * would also need cross-node fan-out of patches to open streams; that is not built.
 */
public interface SessionStore {

    Session create();

    Optional<Session> find(String id);

    /** Gives an existing session a new id (session fixation defence on login/logout). */
    void rekey(Session session);

    void remove(String id);

    Collection<Session> all();
}
