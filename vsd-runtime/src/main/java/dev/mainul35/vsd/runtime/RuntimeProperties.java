package dev.mainul35.vsd.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Configuration for the live runtime ({@code vsd.runtime.*}). */
@ConfigurationProperties(prefix = "vsd.runtime")
public class RuntimeProperties {

    /** Directory holding site.json and pages/*.json (a published snapshot). */
    private String snapshotDir = "site-snapshot";

    /** Directory scanned for plugin JARs. */
    private String pluginsDir = "plugins";

    /** Plugin module roots loaded straight from source/target dirs (development only). */
    private List<String> devPluginDirs = new ArrayList<>();

    /** Idle time after which a visitor session is dropped. */
    private Duration sessionIdleTimeout = Duration.ofMinutes(30);

    private String cookieName = "VSDSESSID";

    /** Force the Secure flag on the session cookie (it is also set for HTTPS requests). */
    private boolean cookieSecure = false;

    /** Maximum number of state entries per session. */
    private int maxStateKeys = 500;

    /** Maximum serialized size of one state value. */
    private int maxValueBytes = 16 * 1024;

    /** Events per second a session may send. */
    private int maxEventsPerSecond = 20;

    /** Users for the built-in config identity provider (development / small sites). */
    private List<User> users = new ArrayList<>();

    /** Data sources used by page bindings ({@code vsd.runtime.data.*}). */
    private Data data = new Data();

    /** Sign-in through the VSD auth server ({@code vsd.runtime.sso.*}). */
    private Sso sso = new Sso();

    /**
     * Limits for API data sources. A page author controls the endpoint, so the operator controls which
     * hosts may be called: with no allowed hosts, API sources are refused and only STATIC and CONTEXT work.
     */
    public static class Data {
        /** Hosts API data sources may call (exact, case-insensitive). */
        private List<String> allowedHosts = new ArrayList<>();
        /** Allow allowed hosts that resolve to loopback / private addresses (local development only). */
        private boolean allowPrivateAddresses = false;
        private Duration timeout = Duration.ofSeconds(5);
        /** Longest a page waits for all of its data sources together. */
        private Duration pageDeadline = Duration.ofSeconds(8);
        private int maxResponseBytes = 1024 * 1024;
        /** Calls per second to any one upstream (same URL, method, headers); visitors cannot multiply load past this. */
        private int maxCallsPerSecond = 20;
        private int cacheMaxEntries = 200;
        private Duration defaultCacheTtl = Duration.ofSeconds(60);

        public List<String> getAllowedHosts() { return allowedHosts; }
        public void setAllowedHosts(List<String> allowedHosts) { this.allowedHosts = allowedHosts; }
        public boolean isAllowPrivateAddresses() { return allowPrivateAddresses; }
        public void setAllowPrivateAddresses(boolean allowPrivateAddresses) { this.allowPrivateAddresses = allowPrivateAddresses; }
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
        public Duration getPageDeadline() { return pageDeadline; }
        public void setPageDeadline(Duration pageDeadline) { this.pageDeadline = pageDeadline; }
        public int getMaxResponseBytes() { return maxResponseBytes; }
        public void setMaxResponseBytes(int maxResponseBytes) { this.maxResponseBytes = maxResponseBytes; }
        public int getMaxCallsPerSecond() { return maxCallsPerSecond; }
        public void setMaxCallsPerSecond(int maxCallsPerSecond) { this.maxCallsPerSecond = maxCallsPerSecond; }
        public int getCacheMaxEntries() { return cacheMaxEntries; }
        public void setCacheMaxEntries(int cacheMaxEntries) { this.cacheMaxEntries = cacheMaxEntries; }
        public Duration getDefaultCacheTtl() { return defaultCacheTtl; }
        public void setDefaultCacheTtl(Duration defaultCacheTtl) { this.defaultCacheTtl = defaultCacheTtl; }
    }

    /**
     * Sign-in through the VSD auth server (OpenID Connect authorization-code flow with PKCE,
     * {@code vsd.runtime.sso.*}). Endpoints that the visitor's browser opens (authorization, end-session)
     * use public URLs; endpoints the runtime calls itself (token, JWKS) may use internal ones.
     */
    public static class Sso {
        private boolean enabled = false;
        /** Expected {@code iss} claim, exactly as the auth server issues it. */
        private String issuer;
        private String authorizationEndpoint;
        private String tokenEndpoint;
        private String jwksUri;
        /** Optional: where to send the visitor after sign-out so the auth server session ends too. */
        private String endSessionEndpoint;
        private String clientId;
        /** Empty for a public client (PKCE only). */
        private String clientSecret;
        /** Public URL of this site's {@code /_vsd/auth/callback}; must be registered with the auth server. */
        private String redirectUri;
        private String scopes = "openid profile email";
        /** Expected {@code aud}; defaults to the client id. */
        private String audience;
        /** If not empty, the user needs at least one of these roles (without the ROLE_ prefix) to sign in. */
        private List<String> requiredRoles = new ArrayList<>();
        /** Keep the username/password sign-in next to SSO. */
        private boolean passwordLogin = true;
        /** Allow http:// endpoints (local development only). */
        private boolean allowInsecureHttp = false;
        /** How long a visitor has to finish signing in at the auth server. */
        private Duration flowTtl = Duration.ofMinutes(10);
        private Duration clockSkew = Duration.ofSeconds(60);
        private Duration timeout = Duration.ofSeconds(5);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getAuthorizationEndpoint() { return authorizationEndpoint; }
        public void setAuthorizationEndpoint(String authorizationEndpoint) { this.authorizationEndpoint = authorizationEndpoint; }
        public String getTokenEndpoint() { return tokenEndpoint; }
        public void setTokenEndpoint(String tokenEndpoint) { this.tokenEndpoint = tokenEndpoint; }
        public String getJwksUri() { return jwksUri; }
        public void setJwksUri(String jwksUri) { this.jwksUri = jwksUri; }
        public String getEndSessionEndpoint() { return endSessionEndpoint; }
        public void setEndSessionEndpoint(String endSessionEndpoint) { this.endSessionEndpoint = endSessionEndpoint; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
        public String getScopes() { return scopes; }
        public void setScopes(String scopes) { this.scopes = scopes; }
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
        public List<String> getRequiredRoles() { return requiredRoles; }
        public void setRequiredRoles(List<String> requiredRoles) { this.requiredRoles = requiredRoles; }
        public boolean isPasswordLogin() { return passwordLogin; }
        public void setPasswordLogin(boolean passwordLogin) { this.passwordLogin = passwordLogin; }
        public boolean isAllowInsecureHttp() { return allowInsecureHttp; }
        public void setAllowInsecureHttp(boolean allowInsecureHttp) { this.allowInsecureHttp = allowInsecureHttp; }
        public Duration getFlowTtl() { return flowTtl; }
        public void setFlowTtl(Duration flowTtl) { this.flowTtl = flowTtl; }
        public Duration getClockSkew() { return clockSkew; }
        public void setClockSkew(Duration clockSkew) { this.clockSkew = clockSkew; }
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
    }

    public static class User {
        private String id;
        private String username;
        private String password;
        private String displayName;
        private String email;
        private List<String> roles = new ArrayList<>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }

    public String getSnapshotDir() { return snapshotDir; }
    public Path snapshotPath() { return Path.of(snapshotDir); }
    public void setSnapshotDir(String snapshotDir) { this.snapshotDir = snapshotDir; }
    public String getPluginsDir() { return pluginsDir; }
    public Path pluginsPath() { return Path.of(pluginsDir); }
    public void setPluginsDir(String pluginsDir) { this.pluginsDir = pluginsDir; }
    public List<String> getDevPluginDirs() { return devPluginDirs; }
    public List<Path> devPluginPaths() { return devPluginDirs.stream().map(Path::of).toList(); }
    public void setDevPluginDirs(List<String> devPluginDirs) { this.devPluginDirs = devPluginDirs; }
    public Duration getSessionIdleTimeout() { return sessionIdleTimeout; }
    public void setSessionIdleTimeout(Duration sessionIdleTimeout) { this.sessionIdleTimeout = sessionIdleTimeout; }
    public String getCookieName() { return cookieName; }
    public void setCookieName(String cookieName) { this.cookieName = cookieName; }
    public boolean isCookieSecure() { return cookieSecure; }
    public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }
    public int getMaxStateKeys() { return maxStateKeys; }
    public void setMaxStateKeys(int maxStateKeys) { this.maxStateKeys = maxStateKeys; }
    public int getMaxValueBytes() { return maxValueBytes; }
    public void setMaxValueBytes(int maxValueBytes) { this.maxValueBytes = maxValueBytes; }
    public int getMaxEventsPerSecond() { return maxEventsPerSecond; }
    public void setMaxEventsPerSecond(int maxEventsPerSecond) { this.maxEventsPerSecond = maxEventsPerSecond; }
    public Sso getSso() { return sso; }
    public void setSso(Sso sso) { this.sso = sso; }
    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }
    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }
}
