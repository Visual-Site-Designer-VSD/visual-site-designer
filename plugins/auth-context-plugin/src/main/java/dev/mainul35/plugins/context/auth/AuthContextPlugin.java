package dev.mainul35.plugins.context.auth;

import dev.mainul35.cms.sdk.ContextProviderPlugin;
import dev.mainul35.cms.sdk.PluginContext;
import dev.mainul35.cms.sdk.context.ApiEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Authentication Context Provider Plugin.
 *
 * Provides shared authentication state that UI component plugins can consume
 * via the usePluginContext("auth") hook. This enables components like LoginForm,
 * LogoutButton, and UserProfile to share auth state without tight coupling.
 *
 * Exposed state includes:
 * - isAuthenticated: whether the user is logged in
 * - user: current user info (name, email, avatar)
 * - login/logout actions
 */
@Slf4j
public class AuthContextPlugin implements ContextProviderPlugin {

    private PluginContext context;

    @Override
    public void onLoad(PluginContext context) throws Exception {
        this.context = context;
        log.info("Auth Context Provider Plugin loaded successfully");
    }

    @Override
    public String getPluginId() {
        return "auth-context-plugin";
    }

    @Override
    public String getName() {
        return "Auth Context Provider";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Provides shared authentication state for UI components";
    }

    @Override
    public String getContextId() {
        return "auth";
    }

    @Override
    public String getProviderComponentPath() {
        return "AuthProvider.js";
    }

    @Override
    public List<ApiEndpoint> getApiEndpoints() {
        return List.of(
                ApiEndpoint.builder()
                        .path("/api/ctx/auth/session")
                        .method("GET")
                        .description("Get current authentication session")
                        .build(),
                ApiEndpoint.builder()
                        .path("/api/ctx/auth/login")
                        .method("POST")
                        .description("Authenticate user with credentials")
                        .build(),
                ApiEndpoint.builder()
                        .path("/api/ctx/auth/logout")
                        .method("POST")
                        .description("End current authentication session")
                        .build()
        );
    }

    @Override
    public List<String> getRequiredContexts() {
        // Auth context has no dependencies on other contexts
        return List.of();
    }
}
