package dev.mainul35.cms.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.Collections;
import java.util.Iterator;

/**
 * Fallback OAuth2 configuration when auth server is disabled.
 * Provides an empty ClientRegistrationRepository to satisfy bean dependencies.
 */
@Configuration
@ConditionalOnProperty(name = "app.auth-server.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class OAuth2ClientDisabledConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        log.info("VSD Auth Server integration is disabled - OAuth2 login not available");
        return new EmptyClientRegistrationRepository();
    }

    /**
     * An empty ClientRegistrationRepository for when OAuth2 is disabled.
     */
    private static class EmptyClientRegistrationRepository implements ClientRegistrationRepository, Iterable<ClientRegistration> {
        @Override
        public ClientRegistration findByRegistrationId(String registrationId) {
            return null;
        }

        @Override
        public Iterator<ClientRegistration> iterator() {
            return Collections.emptyIterator();
        }
    }
}
