package dev.mainul35.cms.security.oauth2;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.Set;

/**
 * Configuration holder for OAuth2 client registration.
 * Used by LazyClientRegistrationRepository to defer OIDC discovery.
 */
@Data
@Builder
public class ClientRegistrationConfig {
    private String clientId;
    private String clientSecret;
    private Set<String> scopes;
    private AuthorizationGrantType authorizationGrantType;
    private String redirectUri;
    private String clientName;
    private String userNameAttributeName;

    // OIDC Discovery (preferred)
    private String issuerUri;

    // Manual configuration (fallback when issuer-uri is not set)
    private String authorizationUri;
    private String tokenUri;
    private String jwkSetUri;
    private String userInfoUri;
}
