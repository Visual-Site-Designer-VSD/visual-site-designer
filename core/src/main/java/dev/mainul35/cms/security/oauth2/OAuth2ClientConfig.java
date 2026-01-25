package dev.mainul35.cms.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * OAuth2 Client configuration that provides a lazy-loading ClientRegistrationRepository.
 * This allows the CMS to start even when the VSD Auth Server is not available.
 */
@Configuration
@ConditionalOnProperty(name = "app.auth-server.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class OAuth2ClientConfig {

    @Value("${spring.security.oauth2.client.registration.vsd-auth.client-id:}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.vsd-auth.client-secret:}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.vsd-auth.issuer-uri:}")
    private String issuerUri;

    @Value("${spring.security.oauth2.client.provider.vsd-auth.authorization-uri:}")
    private String authorizationUri;

    @Value("${spring.security.oauth2.client.provider.vsd-auth.token-uri:}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.vsd-auth.jwk-set-uri:}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.client.provider.vsd-auth.user-info-uri:}")
    private String userInfoUri;

    @Value("${spring.security.oauth2.client.registration.vsd-auth.redirect-uri:{baseUrl}/login/oauth2/code/vsd-auth}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.provider.vsd-auth.user-name-attribute:sub}")
    private String userNameAttribute;

    @Bean
    @Primary
    public ClientRegistrationRepository clientRegistrationRepository() {
        log.info("Creating lazy-loading ClientRegistrationRepository for VSD Auth Server");

        Map<String, ClientRegistrationConfig> configs = new HashMap<>();

        if (clientId != null && !clientId.isEmpty()) {
            ClientRegistrationConfig vsdAuthConfig = ClientRegistrationConfig.builder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .scopes(Set.of("openid", "profile", "email"))
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(redirectUri)
                    .clientName("VSD Auth Server")
                    .userNameAttributeName(userNameAttribute)
                    .issuerUri(issuerUri)
                    .authorizationUri(authorizationUri)
                    .tokenUri(tokenUri)
                    .jwkSetUri(jwkSetUri)
                    .userInfoUri(userInfoUri)
                    .build();

            configs.put("vsd-auth", vsdAuthConfig);
            log.info("Configured OAuth2 client 'vsd-auth' with issuer: {} (lazy-loaded)", issuerUri);
        }

        return new LazyClientRegistrationRepository(configs);
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository(OAuth2AuthorizedClientService authorizedClientService) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
    }
}
