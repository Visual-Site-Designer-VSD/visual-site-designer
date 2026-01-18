package dev.mainul35.cms.security.oauth2;

import dev.mainul35.cms.security.entity.CmsUser;
import dev.mainul35.cms.security.entity.CmsRole;
import dev.mainul35.cms.security.entity.RoleName;
import dev.mainul35.cms.security.entity.UserStatus;
import dev.mainul35.cms.security.repository.CmsUserRepository;
import dev.mainul35.cms.security.repository.CmsRoleRepository;
import dev.mainul35.cms.security.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles successful OAuth2 login from VSD Auth Server.
 * Creates or updates the local CMS user and issues a local JWT token,
 * then redirects to the frontend with the token.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final CmsUserRepository userRepository;
    private final CmsRoleRepository roleRepository;
    private final JwtService jwtService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User oauth2User = oauth2Token.getPrincipal();
            Map<String, Object> attributes = oauth2User.getAttributes();

            String authServerId = (String) attributes.get("sub");
            String email = (String) attributes.get("email");
            String displayName = (String) attributes.get("display_name");
            if (displayName == null) {
                displayName = (String) attributes.get("name");
            }

            log.info("OAuth2 login success for user: {} ({})", email, authServerId);

            // Find or create local CMS user
            CmsUser cmsUser = userRepository.findByAuthServerId(authServerId)
                    .orElseGet(() -> userRepository.findByEmail(email)
                            .orElse(null));

            if (cmsUser == null) {
                // Create new user from auth server
                cmsUser = createUserFromOAuth2(authServerId, email, displayName, attributes);
                log.info("Created new CMS user from auth server: {}", email);
            } else {
                // Update auth server ID if not set
                if (cmsUser.getAuthServerId() == null) {
                    cmsUser.setAuthServerId(authServerId);
                    cmsUser = userRepository.save(cmsUser);
                    log.info("Linked existing CMS user to auth server: {}", email);
                }
            }

            // Generate local JWT token
            String accessToken = jwtService.generateAccessToken(cmsUser);

            // Redirect to frontend with token
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/oauth2/callback")
                    .queryParam("token", accessToken)
                    .build()
                    .toUriString();

            log.debug("Redirecting to frontend with token: {}", redirectUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    private CmsUser createUserFromOAuth2(String authServerId, String email, String displayName,
                                          Map<String, Object> attributes) {
        CmsUser user = new CmsUser();
        user.setAuthServerId(authServerId);
        user.setEmail(email);
        user.setUsername(email.split("@")[0]); // Use email prefix as username
        user.setFullName(displayName);
        user.setIsActive(true);
        user.setStatus(UserStatus.APPROVED); // Auto-approve SSO users

        // Assign default USER role
        Set<CmsRole> roles = new HashSet<>();
        roleRepository.findByRoleName(RoleName.USER).ifPresent(roles::add);

        // Check if user has CMS roles from auth server
        Object authServerRoles = attributes.get("roles");
        if (authServerRoles instanceof Iterable<?> roleList) {
            for (Object role : roleList) {
                String roleName = role.toString();
                if (roleName.startsWith("CMS_")) {
                    // Map CMS roles from auth server using enum
                    String mappedRoleName = roleName.replace("CMS_", "");
                    for (RoleName enumRole : RoleName.values()) {
                        if (enumRole.name().equals(mappedRoleName)) {
                            roleRepository.findByRoleName(enumRole).ifPresent(roles::add);
                            break;
                        }
                    }
                }
            }
        }

        user.setRoles(roles);
        return userRepository.save(user);
    }
}
