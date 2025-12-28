package dev.mainul35.siteruntime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Site Runtime.
 * Customer configures these in their application.properties.
 */
@ConfigurationProperties(prefix = "site.runtime")
public class SiteRuntimeProperties {

    private ApiProperties api = new ApiProperties();
    private DatabaseProperties database = new DatabaseProperties();
    private CacheProperties cache = new CacheProperties();
    private AuthProperties auth = new AuthProperties();

    /**
     * API Gateway configuration
     */
    public static class ApiProperties {
        private String gatewayUrl = "http://localhost:8080";
        private int timeoutMs = 30000;
        private int maxRetries = 3;

        public String getGatewayUrl() { return gatewayUrl; }
        public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    }

    /**
     * Database configuration
     */
    public static class DatabaseProperties {
        private String type = "none";  // none, jpa, mongodb
        private String url;
        private String username;
        private String password;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * Cache configuration
     */
    public static class CacheProperties {
        private String type = "memory";  // memory, redis
        private String redisHost = "localhost";
        private int redisPort = 6379;
        private long defaultTtlMs = 60000;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getRedisHost() { return redisHost; }
        public void setRedisHost(String redisHost) { this.redisHost = redisHost; }
        public int getRedisPort() { return redisPort; }
        public void setRedisPort(int redisPort) { this.redisPort = redisPort; }
        public long getDefaultTtlMs() { return defaultTtlMs; }
        public void setDefaultTtlMs(long defaultTtlMs) { this.defaultTtlMs = defaultTtlMs; }
    }

    /**
     * Authentication configuration
     */
    public static class AuthProperties {
        private String type = "none";  // none, basic, oauth2, jwt, social, sso

        // OAuth2/OpenID Connect (generic)
        private String issuerUrl;
        private String clientId;
        private String clientSecret;
        private String[] scopes = {"openid", "profile", "email"};

        // Social Login Configuration
        private SocialProviders social = new SocialProviders();

        // SSO Configuration (Okta, Keycloak, Azure AD, etc.)
        private SsoConfig sso = new SsoConfig();

        // Session configuration
        private int sessionTimeoutMinutes = 30;
        private String loginSuccessUrl = "/";
        private String logoutSuccessUrl = "/";

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getIssuerUrl() { return issuerUrl; }
        public void setIssuerUrl(String issuerUrl) { this.issuerUrl = issuerUrl; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String[] getScopes() { return scopes; }
        public void setScopes(String[] scopes) { this.scopes = scopes; }
        public SocialProviders getSocial() { return social; }
        public void setSocial(SocialProviders social) { this.social = social; }
        public SsoConfig getSso() { return sso; }
        public void setSso(SsoConfig sso) { this.sso = sso; }
        public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
        public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }
        public String getLoginSuccessUrl() { return loginSuccessUrl; }
        public void setLoginSuccessUrl(String loginSuccessUrl) { this.loginSuccessUrl = loginSuccessUrl; }
        public String getLogoutSuccessUrl() { return logoutSuccessUrl; }
        public void setLogoutSuccessUrl(String logoutSuccessUrl) { this.logoutSuccessUrl = logoutSuccessUrl; }
    }

    /**
     * Social login providers configuration (Google, GitHub, Facebook, etc.)
     */
    public static class SocialProviders {
        private GoogleAuth google = new GoogleAuth();
        private GitHubAuth github = new GitHubAuth();
        private FacebookAuth facebook = new FacebookAuth();

        public GoogleAuth getGoogle() { return google; }
        public void setGoogle(GoogleAuth google) { this.google = google; }
        public GitHubAuth getGithub() { return github; }
        public void setGithub(GitHubAuth github) { this.github = github; }
        public FacebookAuth getFacebook() { return facebook; }
        public void setFacebook(FacebookAuth facebook) { this.facebook = facebook; }
    }

    public static class GoogleAuth {
        private boolean enabled = false;
        private String clientId;
        private String clientSecret;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    }

    public static class GitHubAuth {
        private boolean enabled = false;
        private String clientId;
        private String clientSecret;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    }

    public static class FacebookAuth {
        private boolean enabled = false;
        private String clientId;
        private String clientSecret;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    }

    /**
     * SSO/Enterprise IdP configuration (Okta, Keycloak, Azure AD, etc.)
     */
    public static class SsoConfig {
        private String provider = "none";  // none, okta, keycloak, azure, custom

        // Common OIDC settings
        private String issuerUri;
        private String clientId;
        private String clientSecret;
        private String[] scopes = {"openid", "profile", "email"};

        // Keycloak specific
        private String realm;
        private String authServerUrl;

        // Okta specific
        private String oktaDomain;

        // Azure AD specific
        private String tenantId;

        // Custom OIDC endpoints (for non-standard providers)
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String jwkSetUri;
        private String endSessionUri;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getIssuerUri() { return issuerUri; }
        public void setIssuerUri(String issuerUri) { this.issuerUri = issuerUri; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String[] getScopes() { return scopes; }
        public void setScopes(String[] scopes) { this.scopes = scopes; }
        public String getRealm() { return realm; }
        public void setRealm(String realm) { this.realm = realm; }
        public String getAuthServerUrl() { return authServerUrl; }
        public void setAuthServerUrl(String authServerUrl) { this.authServerUrl = authServerUrl; }
        public String getOktaDomain() { return oktaDomain; }
        public void setOktaDomain(String oktaDomain) { this.oktaDomain = oktaDomain; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getAuthorizationUri() { return authorizationUri; }
        public void setAuthorizationUri(String authorizationUri) { this.authorizationUri = authorizationUri; }
        public String getTokenUri() { return tokenUri; }
        public void setTokenUri(String tokenUri) { this.tokenUri = tokenUri; }
        public String getUserInfoUri() { return userInfoUri; }
        public void setUserInfoUri(String userInfoUri) { this.userInfoUri = userInfoUri; }
        public String getJwkSetUri() { return jwkSetUri; }
        public void setJwkSetUri(String jwkSetUri) { this.jwkSetUri = jwkSetUri; }
        public String getEndSessionUri() { return endSessionUri; }
        public void setEndSessionUri(String endSessionUri) { this.endSessionUri = endSessionUri; }
    }

    // Getters for nested properties
    public ApiProperties getApi() { return api; }
    public void setApi(ApiProperties api) { this.api = api; }
    public DatabaseProperties getDatabase() { return database; }
    public void setDatabase(DatabaseProperties database) { this.database = database; }
    public CacheProperties getCache() { return cache; }
    public void setCache(CacheProperties cache) { this.cache = cache; }
    public AuthProperties getAuth() { return auth; }
    public void setAuth(AuthProperties auth) { this.auth = auth; }
}
