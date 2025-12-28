package dev.mainul35.siteruntime.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-configuration for authentication based on properties.
 * Supports: none, basic, social (Google/GitHub/Facebook), sso (Okta/Keycloak/Azure)
 */
@Configuration
@EnableWebSecurity
@ConditionalOnClass(SecurityFilterChain.class)
public class SecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    private final SiteRuntimeProperties properties;

    public SecurityAutoConfiguration(SiteRuntimeProperties properties) {
        this.properties = properties;
    }

    /**
     * No authentication - permit all requests
     */
    @Bean
    @ConditionalOnProperty(name = "site.runtime.auth.type", havingValue = "none", matchIfMissing = true)
    public SecurityFilterChain noAuthSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security: no authentication (permit all)");
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /**
     * Social login configuration (Google, GitHub, Facebook)
     */
    @Bean
    @ConditionalOnProperty(name = "site.runtime.auth.type", havingValue = "social")
    public SecurityFilterChain socialLoginSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security: social login");
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl(properties.getAuth().getLoginSuccessUrl(), true)
            )
            .logout(logout -> logout
                .logoutSuccessUrl(properties.getAuth().getLogoutSuccessUrl())
                .permitAll()
            );
        return http.build();
    }

    /**
     * SSO configuration (Okta, Keycloak, Azure AD, Custom OIDC)
     */
    @Bean
    @ConditionalOnProperty(name = "site.runtime.auth.type", havingValue = "sso")
    public SecurityFilterChain ssoSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security: SSO");
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl(properties.getAuth().getLoginSuccessUrl(), true)
            )
            .logout(logout -> logout
                .logoutSuccessUrl(properties.getAuth().getLogoutSuccessUrl())
                .permitAll()
            );
        return http.build();
    }

    /**
     * Build client registrations for social providers
     */
    @Bean
    @ConditionalOnProperty(name = "site.runtime.auth.type", havingValue = "social")
    public ClientRegistrationRepository socialClientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();
        var social = properties.getAuth().getSocial();

        if (social.getGoogle().isEnabled()) {
            log.info("Registering Google OAuth2 client");
            registrations.add(googleClientRegistration(social.getGoogle()));
        }
        if (social.getGithub().isEnabled()) {
            log.info("Registering GitHub OAuth2 client");
            registrations.add(githubClientRegistration(social.getGithub()));
        }
        if (social.getFacebook().isEnabled()) {
            log.info("Registering Facebook OAuth2 client");
            registrations.add(facebookClientRegistration(social.getFacebook()));
        }

        if (registrations.isEmpty()) {
            throw new IllegalStateException(
                "Social login enabled but no providers configured. " +
                "Enable at least one of: site.runtime.auth.social.google.enabled, " +
                "site.runtime.auth.social.github.enabled, or site.runtime.auth.social.facebook.enabled"
            );
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }

    /**
     * Build client registration for SSO provider
     */
    @Bean
    @ConditionalOnProperty(name = "site.runtime.auth.type", havingValue = "sso")
    public ClientRegistrationRepository ssoClientRegistrationRepository() {
        var sso = properties.getAuth().getSso();
        log.info("Registering SSO provider: {}", sso.getProvider());
        ClientRegistration registration = buildSsoClientRegistration(sso);
        return new InMemoryClientRegistrationRepository(registration);
    }

    private ClientRegistration googleClientRegistration(
            SiteRuntimeProperties.GoogleAuth google) {
        return ClientRegistration.withRegistrationId("google")
                .clientId(google.getClientId())
                .clientSecret(google.getClientSecret())
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();
    }

    private ClientRegistration githubClientRegistration(
            SiteRuntimeProperties.GitHubAuth github) {
        return ClientRegistration.withRegistrationId("github")
                .clientId(github.getClientId())
                .clientSecret(github.getClientSecret())
                .scope("read:user", "user:email")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();
    }

    private ClientRegistration facebookClientRegistration(
            SiteRuntimeProperties.FacebookAuth facebook) {
        return ClientRegistration.withRegistrationId("facebook")
                .clientId(facebook.getClientId())
                .clientSecret(facebook.getClientSecret())
                .scope("public_profile", "email")
                .authorizationUri("https://www.facebook.com/v18.0/dialog/oauth")
                .tokenUri("https://graph.facebook.com/v18.0/oauth/access_token")
                .userInfoUri("https://graph.facebook.com/me?fields=id,name,email")
                .userNameAttributeName("id")
                .clientName("Facebook")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();
    }

    private ClientRegistration buildSsoClientRegistration(
            SiteRuntimeProperties.SsoConfig sso) {

        String registrationId = sso.getProvider();
        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(registrationId)
                .clientId(sso.getClientId())
                .clientSecret(sso.getClientSecret())
                .scope(sso.getScopes())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}");

        switch (sso.getProvider().toLowerCase()) {
            case "okta":
                String oktaIssuer = "https://" + sso.getOktaDomain();
                builder
                    .issuerUri(oktaIssuer)
                    .authorizationUri(oktaIssuer + "/oauth2/v1/authorize")
                    .tokenUri(oktaIssuer + "/oauth2/v1/token")
                    .userInfoUri(oktaIssuer + "/oauth2/v1/userinfo")
                    .jwkSetUri(oktaIssuer + "/oauth2/v1/keys")
                    .userNameAttributeName("sub")
                    .clientName("Okta");
                break;

            case "keycloak":
                String keycloakIssuer = sso.getAuthServerUrl() + "/realms/" + sso.getRealm();
                builder
                    .issuerUri(keycloakIssuer)
                    .authorizationUri(keycloakIssuer + "/protocol/openid-connect/auth")
                    .tokenUri(keycloakIssuer + "/protocol/openid-connect/token")
                    .userInfoUri(keycloakIssuer + "/protocol/openid-connect/userinfo")
                    .jwkSetUri(keycloakIssuer + "/protocol/openid-connect/certs")
                    .userNameAttributeName("preferred_username")
                    .clientName("Keycloak");
                break;

            case "azure":
                String azureIssuer = "https://login.microsoftonline.com/" + sso.getTenantId() + "/v2.0";
                builder
                    .issuerUri(azureIssuer)
                    .authorizationUri(azureIssuer + "/authorize")
                    .tokenUri("https://login.microsoftonline.com/" + sso.getTenantId() + "/oauth2/v2.0/token")
                    .userInfoUri("https://graph.microsoft.com/oidc/userinfo")
                    .jwkSetUri("https://login.microsoftonline.com/" + sso.getTenantId() + "/discovery/v2.0/keys")
                    .userNameAttributeName("sub")
                    .clientName("Azure AD");
                break;

            case "custom":
            default:
                builder
                    .issuerUri(sso.getIssuerUri())
                    .authorizationUri(sso.getAuthorizationUri())
                    .tokenUri(sso.getTokenUri())
                    .userInfoUri(sso.getUserInfoUri())
                    .jwkSetUri(sso.getJwkSetUri())
                    .userNameAttributeName("sub")
                    .clientName("SSO Provider");
                break;
        }

        return builder.build();
    }
}
