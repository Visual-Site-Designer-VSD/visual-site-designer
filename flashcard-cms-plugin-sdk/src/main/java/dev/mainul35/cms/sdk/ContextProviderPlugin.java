package dev.mainul35.cms.sdk;

import dev.mainul35.cms.sdk.context.ApiEndpoint;

import java.util.List;

/**
 * Interface for context provider plugins.
 * Extends the base Plugin interface with context-specific methods.
 *
 * Context provider plugins supply shared state and services for feature domains
 * (e.g., authentication, e-commerce cart). Unlike UI component plugins, they do
 * not render visual components. Instead, they provide:
 * - A React context provider component
 * - Backend API endpoints
 * - Shared state that multiple UI component plugins can consume
 *
 * UI component plugins declare context dependencies via {@code requiredContexts}
 * in their ComponentManifest and use the {@code usePluginContext(contextId)} hook
 * to access context values at runtime.
 */
public interface ContextProviderPlugin extends Plugin {

    /**
     * Get the unique context identifier (e.g., "auth", "cart", "order").
     * This ID is used by UI component plugins to declare dependencies
     * and by the frontend to look up context providers.
     *
     * @return Unique context identifier string
     */
    String getContextId();

    /**
     * Get the path to the React context provider bundle.
     * This bundle should export a React context provider component
     * and any associated hooks (e.g., useAuth, useCart).
     *
     * @return Path to provider bundle (e.g., "AuthProvider.js")
     */
    String getProviderComponentPath();

    /**
     * Get the list of API endpoints this context exposes.
     * These endpoints are registered with the platform and serve as
     * the backend for the context's shared state.
     *
     * @return List of API endpoint descriptors
     */
    List<ApiEndpoint> getApiEndpoints();

    /**
     * Get the list of context IDs that this context depends on.
     * Used for dependency resolution and provider ordering.
     * For example, a cart context might depend on the auth context.
     *
     * @return List of required context IDs (empty if no dependencies)
     */
    default List<String> getRequiredContexts() {
        return List.of();
    }
}
