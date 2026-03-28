package dev.mainul35.siteruntime.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Auto-configuration for authentication based on properties.
 * Supports: none, social (Google/GitHub/Facebook), sso (Okta/Keycloak/Azure)
 *
 * Designed for SPA architecture:
 * - API routes (/api/**) return JSON 401 instead of redirects
 * - OAuth2 login flows redirect the browser, then return to the SPA
 * - Static assets and SPA routes are always permitted
 */
@Configuration
@EnableWebSecurity
@ConditionalOnClass(SecurityFilterChain.class)
public class SecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    private static final String PROXY_PATH_PATTERN = "/api/proxy/**";

    private static final String[] PUBLIC_PATHS = {
            "/", "/index.html", "/favicon.ico",
            "/css/**", "/js/**", "/images/**", "/plugins/**", "/static/**",
            "/api/pages/**"
    };

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
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /**
     * Social login configuration (Google, GitHub, Facebook)
     * SPA-friendly: API endpoints return JSON 401, OAuth2 redirects for browser auth
     */
    @Bean
    @ConditionalOnProperty(name = "site.runtime.auth.type", havingValue = "social")
    public SecurityFilterChain socialLoginSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security: social login (SPA mode)");
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, PROXY_PATH_PATTERN).permitAll()
                .requestMatchers(HttpMethod.POST, PROXY_PATH_PATTERN).authenticated()
                .requestMatchers(HttpMethod.PUT, PROXY_PATH_PATTERN).authenticated()
                .requestMatchers(HttpMethod.DELETE, PROXY_PATH_PATTERN).authenticated()
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers("/api/ctx/auth/session").permitAll()
                .requestMatchers("/api/ctx/auth/providers").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(spaAuthenticationSuccessHandler())
            )
            .logout(logout -> logout
                .logoutUrl("/api/ctx/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"message\":\"Logged out successfully\"}");
                })
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    if (isApiRequest(request)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"authenticated\":false,\"message\":\"Authentication required\"}");
                    } else {
                        response.sendRedirect("/");
                    }
                })
            )
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /**
     * SSO configuration (Okta, Keycloak, Azure AD, Custom OIDC)
     * SPA-friendly: same pattern as social login
     */
    @Bean
    @ConditionalOnProperty(name = "site.runtime.auth.type", havingValue = "sso")
    public SecurityFilterChain ssoSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security: SSO (SPA mode)");
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, PROXY_PATH_PATTERN).permitAll()
                .requestMatchers(HttpMethod.POST, PROXY_PATH_PATTERN).authenticated()
                .requestMatchers(HttpMethod.PUT, PROXY_PATH_PATTERN).authenticated()
                .requestMatchers(HttpMethod.DELETE, PROXY_PATH_PATTERN).authenticated()
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers("/api/ctx/auth/session").permitAll()
                .requestMatchers("/api/ctx/auth/providers").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(spaAuthenticationSuccessHandler())
            )
            .logout(logout -> logout
                .logoutUrl("/api/ctx/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"message\":\"Logged out successfully\"}");
                })
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    if (isApiRequest(request)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"authenticated\":false,\"message\":\"Authentication required\"}");
                    } else {
                        response.sendRedirect("/");
                    }
                })
            )
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /**
     * After OAuth2 login succeeds, redirect back to the SPA with a query param
     * so the frontend knows authentication completed.
     */
    private AuthenticationSuccessHandler spaAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String successUrl = properties.getAuth().getLoginSuccessUrl();
            String separator = successUrl.contains("?") ? "&" : "?";
            response.sendRedirect(successUrl + separator + "authenticated=true");
        };
    }

    /**
     * CORS configuration for SPA development (allows localhost dev server).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private boolean isApiRequest(jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api/")
                || (accept != null && accept.contains("application/json"));
    }

    // ---- Client Registration Beans ----

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
