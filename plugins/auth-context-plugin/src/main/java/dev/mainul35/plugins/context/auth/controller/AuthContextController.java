package dev.mainul35.plugins.context.auth.controller;

import dev.mainul35.cms.sdk.annotation.PluginController;
import dev.mainul35.plugins.context.auth.model.AuthUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication context controller integrated with Spring Security OAuth2.
 *
 * Reads authentication state from the SecurityContext (populated by Spring Security
 * after OAuth2/SSO login). The SPA frontend calls these endpoints to get the
 * current auth state and available login providers.
 *
 * Endpoints are mounted under: /api/ctx/auth/
 */
@Slf4j
@PluginController(pluginId = "auth-context-plugin", basePath = "")
@RequestMapping("/api/ctx/auth")
public class AuthContextController {

    private final ClientRegistrationRepository clientRegistrationRepository;

    public AuthContextController(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    /**
     * Get current authentication session from Spring Security context.
     * Returns the authenticated user if OAuth2/SSO login has completed, 401 otherwise.
     */
    @GetMapping("/session")
    public ResponseEntity<?> getSession() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401)
                    .body(Map.of("authenticated", false, "message", "Not authenticated"));
        }

        AuthUser user = extractUser(authentication);
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "user", user
        ));
    }

    /**
     * List available OAuth2/SSO login providers.
     * The SPA uses this to render login buttons (e.g., "Sign in with Google").
     */
    @GetMapping("/providers")
    public ResponseEntity<?> getProviders() {
        List<Map<String, String>> providers = new ArrayList<>();

        if (clientRegistrationRepository instanceof Iterable<?> registrations) {
            for (Object reg : registrations) {
                if (reg instanceof ClientRegistration clientReg) {
                    providers.add(Map.of(
                            "id", clientReg.getRegistrationId(),
                            "name", clientReg.getClientName(),
                            "authorizationUrl", "/oauth2/authorization/" + clientReg.getRegistrationId()
                    ));
                }
            }
        }

        return ResponseEntity.ok(Map.of("providers", providers));
    }

    /**
     * Get detailed user profile from the current OAuth2/OIDC principal.
     */
    @GetMapping("/user")
    public ResponseEntity<?> getUserProfile(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("authenticated", false, "message", "Not authenticated"));
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("attributes", principal.getAttributes());
        profile.put("authorities", principal.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList());

        if (principal instanceof OidcUser oidcUser) {
            profile.put("idToken", oidcUser.getIdToken().getTokenValue());
            profile.put("claims", oidcUser.getClaims());
        }

        return ResponseEntity.ok(profile);
    }

    /**
     * Extract a normalized AuthUser from the Spring Security Authentication object.
     * Works with OAuth2 (Google, GitHub, Facebook) and OIDC (Okta, Keycloak, Azure) principals.
     */
    private AuthUser extractUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof OidcUser oidcUser) {
            return AuthUser.builder()
                    .id(oidcUser.getSubject())
                    .email(oidcUser.getEmail())
                    .name(oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getPreferredUsername())
                    .avatarUrl(oidcUser.getPicture())
                    .provider(getRegistrationId(authentication))
                    .build();
        }

        if (principal instanceof OAuth2User oauth2User) {
            Map<String, Object> attrs = oauth2User.getAttributes();
            return AuthUser.builder()
                    .id(getStringAttr(attrs, "id", "sub"))
                    .email(getStringAttr(attrs, "email"))
                    .name(getStringAttr(attrs, "name", "login", "preferred_username"))
                    .avatarUrl(getStringAttr(attrs, "avatar_url", "picture"))
                    .provider(getRegistrationId(authentication))
                    .build();
        }

        // Fallback for non-OAuth2 auth (e.g., form login)
        return AuthUser.builder()
                .id(authentication.getName())
                .name(authentication.getName())
                .build();
    }

    private String getRegistrationId(Authentication authentication) {
        if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken token) {
            return token.getAuthorizedClientRegistrationId();
        }
        return null;
    }

    private String getStringAttr(Map<String, Object> attrs, String... keys) {
        for (String key : keys) {
            Object val = attrs.get(key);
            if (val != null) {
                return val.toString();
            }
        }
        return null;
    }
}
