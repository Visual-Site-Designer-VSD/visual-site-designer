package dev.mainul35.cms.security.oauth2;

import dev.mainul35.cms.security.authserver.AuthServerSettingsService;
import dev.mainul35.cms.security.authserver.DynamicClientRegistrationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

/**
 * OAuth2 client for the VSD Auth Server. The registration is built from the settings in use at the moment of each
 * sign-in (see {@link DynamicClientRegistrationRepository}), so the auth server can be configured, switched on and
 * off in the admin console without restarting, and the CMS starts even when the auth server is down.
 */
@Configuration
@Slf4j
public class OAuth2ClientConfig {

    @Value("${spring.security.oauth2.client.registration.vsd-auth.redirect-uri:{baseUrl}/login/oauth2/code/vsd-auth}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.provider.vsd-auth.user-name-attribute:sub}")
    private String userNameAttribute;

    @Bean
    @Primary
    public ClientRegistrationRepository clientRegistrationRepository(AuthServerSettingsService settings) {
        return new DynamicClientRegistrationRepository(settings, redirectUri, userNameAttribute);
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
