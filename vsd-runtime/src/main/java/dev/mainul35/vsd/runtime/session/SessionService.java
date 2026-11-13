package dev.mainul35.vsd.runtime.session;

import dev.mainul35.vsd.runtime.RuntimeProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;

/** Cookie handling, CSRF/origin checks and idle expiry for visitor sessions. */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    public static final String CSRF_HEADER = "X-VSD-CSRF";

    private final SessionStore store;
    private final RuntimeProperties props;

    public SessionService(SessionStore store, RuntimeProperties props) {
        this.store = store;
        this.props = props;
    }

    /** The visitor's live session, if the cookie points at one that has not expired. */
    public Optional<Session> find(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie c : cookies) {
            if (props.getCookieName().equals(c.getName())) {
                Optional<Session> s = store.find(c.getValue());
                s.ifPresent(Session::touch);
                return s;
            }
        }
        return Optional.empty();
    }

    public Session getOrCreate(HttpServletRequest request, HttpServletResponse response) {
        Optional<Session> existing = find(request);
        if (existing.isPresent()) {
            return existing.get();
        }
        Session s = store.create();
        writeCookie(request, response, s);
        return s;
    }

    /** New session id and CSRF token for the same session: call on every privilege change. */
    public void rotate(Session session, HttpServletRequest request, HttpServletResponse response) {
        session.lock().lock();
        try {
            store.rekey(session);
            session.setCsrf(InMemorySessionStore.newToken());
        } finally {
            session.lock().unlock();
        }
        writeCookie(request, response, session);
    }

    /**
     * Guards state-changing requests: the CSRF header must match and, when the browser sends
     * an Origin header, it must be this host.
     */
    public boolean passesCsrf(Session session, HttpServletRequest request) {
        String header = request.getHeader(CSRF_HEADER);
        if (header == null || !MessageDigest.isEqual(
                header.getBytes(StandardCharsets.UTF_8), session.csrf().getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            return true;
        }
        try {
            URI o = URI.create(origin);
            String host = o.getPort() == -1 ? o.getHost() : o.getHost() + ":" + o.getPort();
            return host != null && host.equalsIgnoreCase(request.getHeader(HttpHeaders.HOST));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void writeCookie(HttpServletRequest request, HttpServletResponse response, Session session) {
        ResponseCookie cookie = ResponseCookie.from(props.getCookieName(), session.id())
                .httpOnly(true)
                .secure(props.isCookieSecure() || request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(1))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Scheduled(fixedDelay = 60_000)
    void expireIdle() {
        long cutoff = System.currentTimeMillis() - props.getSessionIdleTimeout().toMillis();
        int removed = 0;
        for (Session s : store.all()) {
            if (s.lastAccess() < cutoff) {
                store.remove(s.id());
                s.closeStreams();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Expired {} idle sessions", removed);
        }
    }

    @Scheduled(fixedDelay = 20_000)
    void heartbeat() {
        for (Session s : store.all()) {
            s.heartbeat();
        }
    }
}
