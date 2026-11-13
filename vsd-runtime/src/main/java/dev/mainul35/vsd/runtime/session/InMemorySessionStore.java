package dev.mainul35.vsd.runtime.session;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySessionStore implements SessionStore {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public Session create() {
        Session s = new Session(newToken(), newToken());
        sessions.put(s.id(), s);
        return s;
    }

    @Override
    public Optional<Session> find(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(sessions.get(id));
    }

    @Override
    public void rekey(Session session) {
        sessions.remove(session.id());
        session.setId(newToken());
        sessions.put(session.id(), session);
    }

    @Override
    public void remove(String id) {
        sessions.remove(id);
    }

    @Override
    public Collection<Session> all() {
        return sessions.values();
    }
}
