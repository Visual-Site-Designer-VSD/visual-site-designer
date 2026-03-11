package dev.mainul35.cms.sdk.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Metadata descriptor for a context provider plugin.
 * Stored in the context registry and served to the frontend for discovery.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextDescriptor {

    /**
     * Unique context identifier (e.g., "auth", "cart")
     */
    private String contextId;

    /**
     * Path to the React context provider component bundle
     * (e.g., "AuthProvider.js")
     */
    private String providerComponentPath;

    /**
     * Backend REST API endpoints this context exposes
     */
    private List<ApiEndpoint> apiEndpoints;

    /**
     * Context IDs that this context depends on
     * (e.g., cart depends on auth)
     */
    private List<String> requiredContexts;

    /**
     * Plugin ID that provides this context
     */
    private String pluginId;

    /**
     * Plugin version
     */
    private String pluginVersion;
}
