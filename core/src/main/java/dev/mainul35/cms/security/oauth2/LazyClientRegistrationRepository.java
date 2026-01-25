package dev.mainul35.cms.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A lazy-loading ClientRegistrationRepository that defers OIDC discovery
 * until a client registration is actually requested.
 *
 * This allows the CMS to start even when the VSD Auth Server is not available,
 * and only fails when OAuth2 login is actually attempted.
 */
@Slf4j
public class LazyClientRegistrationRepository implements ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final Map<String, ClientRegistrationConfig> configs;
    private final Map<String, ClientRegistration> cache = new ConcurrentHashMap<>();

    public LazyClientRegistrationRepository(Map<String, ClientRegistrationConfig> configs) {
        this.configs = configs;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (registrationId == null) {
            return null;
        }

        // Check cache first
        ClientRegistration cached = cache.get(registrationId);
        if (cached != null) {
            return cached;
        }

        // Get config and build registration lazily
        ClientRegistrationConfig config = configs.get(registrationId);
        if (config == null) {
            log.debug("No configuration found for registration ID: {}", registrationId);
            return null;
        }

        try {
            ClientRegistration registration = buildClientRegistration(registrationId, config);
            cache.put(registrationId, registration);
            log.info("Successfully loaded OAuth2 client registration: {}", registrationId);
            return registration;
        } catch (Exception e) {
            log.error("Failed to load OAuth2 client registration '{}': {}", registrationId, e.getMessage());
            throw new IllegalStateException("Unable to resolve OAuth2 configuration for '" + registrationId +
                    "'. Is the auth server running at " + config.getIssuerUri() + "?", e);
        }
    }

    private ClientRegistration buildClientRegistration(String registrationId, ClientRegistrationConfig config) {
        ClientRegistration.Builder builder;

        if (config.getIssuerUri() != null && !config.getIssuerUri().isEmpty()) {
            // Use OIDC discovery
            builder = ClientRegistrations.fromIssuerLocation(config.getIssuerUri())
                    .registrationId(registrationId);
        } else {
            // Manual configuration without OIDC discovery
            builder = ClientRegistration.withRegistrationId(registrationId)
                    .authorizationUri(config.getAuthorizationUri())
                    .tokenUri(config.getTokenUri())
                    .jwkSetUri(config.getJwkSetUri())
                    .userInfoUri(config.getUserInfoUri());
        }

        return builder
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret())
                .scope(config.getScopes())
                .authorizationGrantType(config.getAuthorizationGrantType())
                .redirectUri(config.getRedirectUri())
                .clientName(config.getClientName() != null ? config.getClientName() : registrationId)
                .userNameAttributeName(config.getUserNameAttributeName())
                .build();
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        // Load all registrations lazily
        return configs.keySet().stream()
                .map(this::findByRegistrationId)
                .filter(reg -> reg != null)
                .iterator();
    }

    /**
     * Clear the cache (useful for testing or when auth server restarts)
     */
    public void clearCache() {
        cache.clear();
        log.info("OAuth2 client registration cache cleared");
    }
}
