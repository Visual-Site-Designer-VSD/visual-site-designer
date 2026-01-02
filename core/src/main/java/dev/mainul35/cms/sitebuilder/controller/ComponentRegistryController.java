package dev.mainul35.cms.sitebuilder.controller;

import dev.mainul35.cms.sdk.component.ComponentManifest;
import dev.mainul35.cms.plugin.core.PluginAssetService;
import dev.mainul35.cms.plugin.core.PluginManager;
import dev.mainul35.cms.sitebuilder.entity.ComponentRegistryEntry;
import dev.mainul35.cms.sitebuilder.service.ComponentRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for component registry operations.
 * Provides endpoints for browsing, searching, and accessing UI components.
 */
@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ComponentRegistryController {

    private final ComponentRegistryService componentRegistryService;
    private final PluginManager pluginManager;
    private final PluginAssetService pluginAssetService;

    @org.springframework.beans.factory.annotation.Value("${app.plugin.directory:plugins}")
    private String pluginDirectory;

    /**
     * Get all registered components
     *
     * @return List of all active components
     */
    @GetMapping
    public ResponseEntity<List<ComponentRegistryEntry>> getAllComponents() {
        try {
            log.debug("Fetching all components");
            List<ComponentRegistryEntry> components = componentRegistryService.getAllComponents();
            return ResponseEntity.ok(components);
        } catch (Exception e) {
            log.error("Error fetching components", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get components by category
     *
     * @param category Component category (ui, layout, form, widget)
     * @return List of components in the category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ComponentRegistryEntry>> getComponentsByCategory(@PathVariable String category) {
        try {
            log.debug("Fetching components for category: {}", category);
            List<ComponentRegistryEntry> components = componentRegistryService.getComponentsByCategory(category);
            return ResponseEntity.ok(components);
        } catch (Exception e) {
            log.error("Error fetching components by category: {}", category, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all available categories
     *
     * @return List of category names
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        try {
            List<String> categories = componentRegistryService.getCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error fetching categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific component
     *
     * @param pluginId Plugin identifier
     * @param componentId Component identifier
     * @return Component registry entry
     */
    @GetMapping("/{pluginId}/{componentId}")
    public ResponseEntity<ComponentRegistryEntry> getComponent(
            @PathVariable String pluginId,
            @PathVariable String componentId) {
        try {
            log.debug("Fetching component: {} from plugin: {}", componentId, pluginId);
            return componentRegistryService.getComponent(pluginId, componentId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching component: {} from plugin: {}", componentId, pluginId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get component manifest
     *
     * @param pluginId Plugin identifier
     * @param componentId Component identifier
     * @return Component manifest with full configuration
     */
    @GetMapping("/{pluginId}/{componentId}/manifest")
    public ResponseEntity<ComponentManifest> getComponentManifest(
            @PathVariable String pluginId,
            @PathVariable String componentId) {
        try {
            log.debug("Fetching manifest for component: {} from plugin: {}", componentId, pluginId);
            return componentRegistryService.getComponentManifest(pluginId, componentId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching manifest for component: {} from plugin: {}", componentId, pluginId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get components from a specific plugin
     *
     * @param pluginId Plugin identifier
     * @return List of components from the plugin
     */
    @GetMapping("/plugin/{pluginId}")
    public ResponseEntity<List<ComponentRegistryEntry>> getPluginComponents(@PathVariable String pluginId) {
        try {
            log.debug("Fetching components from plugin: {}", pluginId);
            List<ComponentRegistryEntry> components = componentRegistryService.getPluginComponents(pluginId);
            return ResponseEntity.ok(components);
        } catch (Exception e) {
            log.error("Error fetching components from plugin: {}", pluginId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search components by name
     *
     * @param query Search query
     * @return List of matching components
     */
    @GetMapping("/search")
    public ResponseEntity<List<ComponentRegistryEntry>> searchComponents(@RequestParam String query) {
        try {
            log.debug("Searching components with query: {}", query);
            List<ComponentRegistryEntry> components = componentRegistryService.getAllComponents();

            // Simple filter by component name (case-insensitive)
            List<ComponentRegistryEntry> filtered = components.stream()
                    .filter(c -> c.getComponentName().toLowerCase().contains(query.toLowerCase()))
                    .toList();

            return ResponseEntity.ok(filtered);
        } catch (Exception e) {
            log.error("Error searching components with query: {}", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Component bundle endpoint - serves the actual React component JavaScript bundle
     * from plugin resources.
     *
     * The bundle is loaded from the plugin JAR's frontend directory.
     * Supported bundle naming conventions:
     * - frontend/{componentId}.js
     * - frontend/components/{componentId}.js
     * - frontend/bundle.js (for single-component plugins)
     *
     * @param pluginId Plugin identifier
     * @param componentId Component identifier
     * @return Component bundle as JavaScript
     */
    @GetMapping("/{pluginId}/{componentId}/bundle.js")
    public ResponseEntity<byte[]> getComponentBundle(
            @PathVariable String pluginId,
            @PathVariable String componentId) {
        try {
            log.debug("Fetching bundle for component: {} from plugin: {}", componentId, pluginId);

            // Try different bundle naming conventions
            String[] bundlePaths = {
                    componentId + ".js",                    // {componentId}.js
                    "components/" + componentId + ".js",    // components/{componentId}.js
                    componentId + "/index.js",              // {componentId}/index.js
                    "bundle.js",                            // bundle.js (single-component plugins)
                    "index.js"                              // index.js
            };

            for (String bundlePath : bundlePaths) {
                var bundle = pluginAssetService.getAssetBytes(pluginId, bundlePath);
                if (bundle.isPresent() && bundle.get().length > 0) {
                    log.debug("Found bundle at: {} for plugin: {}", bundlePath, pluginId);
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, "application/javascript; charset=utf-8")
                            .header(HttpHeaders.CACHE_CONTROL, "no-cache, must-revalidate")
                            .body(bundle.get());
                }
            }

            // If no bundle found, return a fallback that logs a warning
            log.warn("No bundle found for component: {} in plugin: {}", componentId, pluginId);
            String fallbackScript = """
                    // Component bundle not found for %s/%s
                    // Ensure the plugin JAR contains frontend/%s.js or frontend/bundle.js
                    console.warn('[Plugin] Component bundle not found: %s/%s');
                    export default function %s(props) {
                        return React.createElement('div', {
                            style: { padding: '10px', border: '1px dashed #ccc', color: '#999' }
                        }, 'Component not loaded: %s');
                    }
                    """.formatted(pluginId, componentId, componentId, pluginId, componentId,
                    toPascalCase(componentId), componentId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/javascript; charset=utf-8")
                    .body(fallbackScript.getBytes());
        } catch (Exception e) {
            log.error("Error fetching bundle for component: {} from plugin: {}", componentId, pluginId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Convert a kebab-case or snake_case string to PascalCase
     */
    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : input.toCharArray()) {
            if (c == '-' || c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Delete a specific component from the registry.
     * Used to clean up old/obsolete component entries.
     *
     * @param pluginId Plugin identifier
     * @param componentId Component identifier
     * @return Success or not found response
     */
    @DeleteMapping("/{pluginId}/{componentId}")
    public ResponseEntity<Void> deleteComponent(
            @PathVariable String pluginId,
            @PathVariable String componentId) {
        try {
            log.info("Deleting component: {} from plugin: {}", componentId, pluginId);

            // Check if component exists
            if (!componentRegistryService.isComponentRegistered(pluginId, componentId)) {
                log.warn("Component not found: {} from plugin: {}", componentId, pluginId);
                return ResponseEntity.notFound().build();
            }

            componentRegistryService.unregisterComponent(pluginId, componentId);
            log.info("Successfully deleted component: {} from plugin: {}", componentId, pluginId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting component: {} from plugin: {}", componentId, pluginId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Debug endpoint to check plugin loading status
     */
    @GetMapping("/debug/status")
    public ResponseEntity<Map<String, Object>> getDebugStatus() {
        Map<String, Object> status = new HashMap<>();

        // Check plugin directory
        File pluginDir = new File(pluginDirectory);
        status.put("pluginDirPath", pluginDir.getAbsolutePath());
        status.put("pluginDirExists", pluginDir.exists());
        status.put("pluginDirIsDirectory", pluginDir.isDirectory());

        // List JAR files
        if (pluginDir.exists() && pluginDir.isDirectory()) {
            File[] jarFiles = pluginDir.listFiles((dir, name) -> name.endsWith(".jar"));
            status.put("jarFilesCount", jarFiles != null ? jarFiles.length : 0);
            if (jarFiles != null) {
                status.put("jarFiles", java.util.Arrays.stream(jarFiles).map(File::getName).toList());
            }
        }

        // Check loaded plugins
        status.put("loadedPluginsCount", pluginManager.getAllPlugins().size());
        status.put("activatedPluginsCount", pluginManager.getActivatedPlugins().size());
        status.put("loadedPlugins", pluginManager.getAllPlugins().stream()
                .map(p -> Map.of("id", p.getPluginId(), "name", p.getPluginName(), "status", p.getStatus()))
                .toList());

        // Check registered components
        status.put("registeredComponentsCount", componentRegistryService.getAllComponents().size());

        return ResponseEntity.ok(status);
    }
}
