package dev.mainul35.cms.security.authserver;

import dev.mainul35.cms.security.filter.JwtAuthenticationFilter.JwtUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * The admin console's view of the VSD Auth Server settings. Administrators only. The client secret goes in, and
 * only "is one set" comes out.
 */
@RestController
@RequestMapping("/api/admin/auth-server")
@PreAuthorize("hasRole('ADMIN')")
public class AuthServerAdminController {

    /** What the browser sees: everything except the secret. */
    public record View(boolean enabled, boolean usable, String source, String issuerUri, String authorizationUri,
                       String tokenUri, String jwkSetUri, String userInfoUri, String clientId, boolean clientSecretSet,
                       String scopes, String redirectUri) {
    }

    private final AuthServerSettingsService settings;

    public AuthServerAdminController(AuthServerSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    public ResponseEntity<View> get(HttpServletRequest request) {
        return ResponseEntity.ok(view(settings.effective(), request));
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestBody AuthServerSettingsService.Update body,
                                    @AuthenticationPrincipal JwtUserPrincipal principal, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(view(settings.update(body, principal == null ? null : principal.userId()), request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Forget the saved settings so the application configuration applies again. */
    @DeleteMapping
    public ResponseEntity<View> reset(@AuthenticationPrincipal JwtUserPrincipal principal, HttpServletRequest request) {
        return ResponseEntity.ok(view(settings.resetToEnvironment(principal == null ? null : principal.userId()), request));
    }

    /** Checks the addresses in the form without saving them. */
    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestBody AuthServerSettingsService.Update body) {
        try {
            List<AuthServerSettingsService.Check> checks = settings.test(body);
            return ResponseEntity.ok(Map.of("checks", checks, "ok", checks.stream().allMatch(AuthServerSettingsService.Check::ok)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private static View view(AuthServerSettingsService.Effective e, HttpServletRequest request) {
        // The address the auth server must be told to send people back to.
        String redirect = ServletUriComponentsBuilder.fromContextPath(request).replacePath("/login/oauth2/code/vsd-auth").build().toUriString();
        return new View(e.enabled(), e.usable(), e.fromDatabase() ? "database" : "environment", e.issuerUri(),
                e.authorizationUri(), e.tokenUri(), e.jwkSetUri(), e.userInfoUri(), e.clientId(),
                e.clientSecret() != null && !e.clientSecret().isBlank(), e.scopes(), redirect);
    }
}
