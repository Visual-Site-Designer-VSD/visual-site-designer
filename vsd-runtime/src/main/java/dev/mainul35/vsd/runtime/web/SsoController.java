package dev.mainul35.vsd.runtime.web;

import dev.mainul35.vsd.runtime.auth.sso.SsoService;
import dev.mainul35.vsd.runtime.session.Session;
import dev.mainul35.vsd.runtime.session.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.util.Optional;

/**
 * Browser entry points for single sign-on. Both are plain GET navigations: the visitor's browser
 * is redirected to the auth server and back. The one-time state tied to the session protects the callback.
 */
@RestController
@RequestMapping("/_vsd/auth")
public class SsoController {

    private final SsoService sso;
    private final SessionService sessions;

    public SsoController(SsoService sso, SessionService sessions) {
        this.sso = sso;
        this.sessions = sessions;
    }

    @GetMapping("/sso/start")
    public ResponseEntity<String> start(@RequestParam(value = "return", required = false) String returnTo,
                                        HttpServletRequest request, HttpServletResponse response) {
        if (!sso.enabled()) {
            return page(404, "Single sign-on is not enabled on this site.");
        }
        Session s = sessions.getOrCreate(request, response);
        return respond(sso.start(s, returnTo, request.getRemoteAddr()));
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam(value = "code", required = false) String code,
                                           @RequestParam(value = "state", required = false) String state,
                                           @RequestParam(value = "error", required = false) String error,
                                           HttpServletRequest request, HttpServletResponse response) {
        if (!sso.enabled()) {
            return page(404, "Single sign-on is not enabled on this site.");
        }
        Optional<Session> s = sessions.find(request);
        return respond(sso.callback(s.orElse(null), code, state, error, request.getRemoteAddr(), request, response));
    }

    private static ResponseEntity<String> respond(SsoService.Outcome o) {
        if (o.ok()) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, URI.create(o.redirectTo()).toASCIIString())
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build();
        }
        return page(o.status(), o.message());
    }

    private static ResponseEntity<String> page(int status, String message) {
        String html = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Sign-in</title></head>"
                + "<body style=\"font-family:system-ui;padding:3rem\"><h1>Sign-in</h1><p>"
                + HtmlUtils.htmlEscape(message) + "</p><p><a href=\"/\">Back to the site</a></p></body></html>";
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(html);
    }
}
