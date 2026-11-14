package dev.mainul35.vsd.runtime.auth.sso;

import com.nimbusds.jwt.JWTClaimsSet;
import dev.mainul35.cms.sdk.runtime.RuntimeUser;
import dev.mainul35.vsd.runtime.auth.AuthService;
import dev.mainul35.vsd.runtime.session.Session;
import dev.mainul35.vsd.runtime.util.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the sign-in handshake with the VSD auth server. Each attempt is a one-time "flow"
 * tied to the visitor's session, so a callback from another browser (or a replay) is refused.
 */
@Service
public class SsoService {

    private static final Logger log = LoggerFactory.getLogger(SsoService.class);
    private static final int MAX_FLOWS = 10_000;
    private static final int MAX_RETURN_CHARS = 512;

    /** Where the handler sends the browser next, or why it could not. */
    public record Outcome(boolean ok, String redirectTo, int status, String message) {
        static Outcome redirect(String to) {
            return new Outcome(true, to, 302, null);
        }

        static Outcome fail(int status, String message) {
            return new Outcome(false, null, status, message);
        }
    }

    private record Flow(String sessionId, String verifier, String nonce, String returnTo, long expiresAt) {
    }

    private final OidcClient oidc;
    private final AuthService auth;
    private final ConcurrentHashMap<String, Flow> flows = new ConcurrentHashMap<>();
    private final RateLimiter limiter = new RateLimiter(20, 60_000);

    public SsoService(OidcClient oidc, AuthService auth) {
        this.oidc = oidc;
        this.auth = auth;
    }

    public boolean enabled() {
        return oidc.enabled();
    }

    /** Starts a sign-in: returns the auth server URL to send the visitor to. */
    public Outcome start(Session session, String returnTo, String ip) {
        if (!limiter.tryAcquire(ip)) {
            return Outcome.fail(429, "Too many attempts. Try again in a minute.");
        }
        if (flows.size() >= MAX_FLOWS) {
            long now = System.currentTimeMillis();
            flows.values().removeIf(f -> f.expiresAt() < now);
            if (flows.size() >= MAX_FLOWS) {
                return Outcome.fail(503, "Sign-in is busy. Try again shortly.");
            }
        }
        String state = OidcClient.randomToken();
        String nonce = OidcClient.randomToken();
        String verifier = OidcClient.randomToken();
        long ttl = oidc.config().getFlowTtl().toMillis();
        flows.put(state, new Flow(session.id(), verifier, nonce, safeReturn(returnTo), System.currentTimeMillis() + ttl));
        return Outcome.redirect(oidc.authorizeUrl(state, nonce, verifier));
    }

    /** Finishes a sign-in: checks the flow, swaps the code, verifies the token and signs the visitor in. */
    public Outcome callback(Session session, String code, String state, String error, String ip,
                            HttpServletRequest request, HttpServletResponse response) {
        if (!limiter.tryAcquire(ip)) {
            return Outcome.fail(429, "Too many attempts. Try again in a minute.");
        }
        // The state is single-use: take it out before anything else, whatever happens next.
        Flow flow = state == null || state.length() > 256 ? null : flows.remove(state);
        if (flow == null || flow.expiresAt() < System.currentTimeMillis()) {
            return Outcome.fail(400, "This sign-in link has expired. Please start again.");
        }
        if (session == null || !flow.sessionId().equals(session.id())) {
            log.warn("SSO callback arrived from a different session than the one that started it");
            return Outcome.fail(400, "This sign-in was started in another browser. Please start again.");
        }
        if (error != null) {
            log.info("Auth server returned error '{}'", error.length() > 64 ? error.substring(0, 64) : error);
            return Outcome.fail(403, "Sign-in was cancelled or refused.");
        }
        if (code == null || code.isBlank() || code.length() > 2048) {
            return Outcome.fail(400, "Sign-in could not be completed.");
        }
        RuntimeUser user;
        try {
            OidcClient.Token token = oidc.exchange(code, flow.verifier());
            user = toUser(oidc.verify(token, flow.nonce()));
        } catch (SsoException e) {
            log.warn("SSO sign-in failed: {}", e.getMessage());
            return Outcome.fail(401, "Sign-in could not be completed.");
        }
        List<String> required = oidc.config().getRequiredRoles();
        if (!required.isEmpty() && required.stream().map(SsoService::normalizeRole).noneMatch(user::hasRole)) {
            log.info("SSO user {} lacks a required role", user.id());
            return Outcome.fail(403, "Your account is not allowed to sign in to this site.");
        }
        auth.signIn(session, user, AuthService.PROVIDER_SSO, request, response);
        return Outcome.redirect(flow.returnTo());
    }

    // ---- claims -> user ---------------------------------------------------------------------

    static RuntimeUser toUser(JWTClaimsSet c) {
        String sub = c.getSubject();
        Object userId = c.getClaim("user_id");
        String id = userId != null && !userId.toString().isBlank() ? userId.toString() : sub;
        String email = text(c, "email");
        if (email == null && sub.contains("@")) {
            email = sub;
        }
        String name = text(c, "display_name");
        if (name == null) {
            name = text(c, "name");
        }
        if (name == null) {
            name = email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : sub;
        }
        String avatar = text(c, "avatar_url");
        if (avatar != null && (avatar.length() > 512 || !(avatar.startsWith("https://") || avatar.startsWith("http://")))) {
            avatar = null;
        }
        return new RuntimeUser(id, sub, name, email, roles(c.getClaim("roles")), avatar);
    }

    private static Set<String> roles(Object claim) {
        List<String> raw = new ArrayList<>();
        if (claim instanceof List<?> list) {
            list.forEach(o -> raw.add(String.valueOf(o)));
        } else if (claim instanceof String s) {
            raw.addAll(List.of(s.split("[\\s,]+")));
        }
        Set<String> out = new LinkedHashSet<>();
        for (String r : raw) {
            if (!r.isBlank() && out.size() < 50) {
                out.add(normalizeRole(r));
            }
        }
        return out;
    }

    /** {@code ROLE_Admin} and {@code admin} both become {@code ADMIN}. */
    static String normalizeRole(String role) {
        String r = role.trim().toUpperCase(Locale.ROOT);
        return r.startsWith("ROLE_") ? r.substring(5) : r;
    }

    private static String text(JWTClaimsSet c, String name) {
        try {
            String v = c.getStringClaim(name);
            return v == null || v.isBlank() ? null : (v.length() > 256 ? v.substring(0, 256) : v);
        } catch (ParseException e) {
            return null;
        }
    }

    // ---- return path ------------------------------------------------------------------------

    /** Only a path on this site may be used after sign-in; anything else becomes the home page. */
    static String safeReturn(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_RETURN_CHARS) {
            return "/";
        }
        if (value.charAt(0) != '/' || value.startsWith("//") || value.contains("\\") || value.startsWith("/_vsd/")) {
            return "/";
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < 0x20 || value.charAt(i) == 0x7f) {
                return "/";
            }
        }
        try {
            URI u = new URI(value);
            if (u.getScheme() != null || u.getRawAuthority() != null) {
                return "/";
            }
        } catch (java.net.URISyntaxException e) {
            return "/";
        }
        return value;
    }
}
