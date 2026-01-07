package dev.mainul35.cms.sitebuilder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.cms.sdk.component.ComponentManifest;
import dev.mainul35.cms.sitebuilder.entity.ComponentRegistryEntry;
import dev.mainul35.cms.sitebuilder.entity.PageVersion;
import dev.mainul35.cms.sitebuilder.repository.ComponentRegistryRepository;
import dev.mainul35.cms.sitebuilder.repository.PageVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing the component registry.
 * Handles registration, lookup, and lifecycle of UI component plugins.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComponentRegistryService {

    private final ComponentRegistryRepository componentRegistryRepository;
    private final PageVersionRepository pageVersionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Register a UI component from a plugin
     */
    @Transactional
    public ComponentRegistryEntry registerComponent(ComponentManifest manifest) {
        log.info("Registering component: {} from plugin: {}", manifest.getComponentId(), manifest.getPluginId());

        // Check if component already exists
        Optional<ComponentRegistryEntry> existing = componentRegistryRepository
                .findByPluginIdAndComponentId(manifest.getPluginId(), manifest.getComponentId());

        ComponentRegistryEntry entry;
        if (existing.isPresent()) {
            log.info("Component already exists, updating: {}", manifest.getComponentId());
            entry = existing.get();
        } else {
            entry = new ComponentRegistryEntry();
            entry.setPluginId(manifest.getPluginId());
            entry.setComponentId(manifest.getComponentId());
        }

        // Update component data
        entry.setComponentName(manifest.getDisplayName());
        entry.setCategory(manifest.getCategory());
        entry.setIcon(manifest.getIcon());
        entry.setReactBundlePath(manifest.getReactComponentPath());
        entry.setIsActive(true);

        // Serialize manifest to JSON
        try {
            String manifestJson = objectMapper.writeValueAsString(manifest);
            entry.setComponentManifest(manifestJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize component manifest", e);
            throw new RuntimeException("Failed to serialize component manifest", e);
        }

        ComponentRegistryEntry saved = componentRegistryRepository.save(entry);
        log.info("Component registered successfully: {}", manifest.getComponentId());
        return saved;
    }

    /**
     * Get all registered components
     */
    public List<ComponentRegistryEntry> getAllComponents() {
        return componentRegistryRepository.findByIsActiveTrue();
    }

    /**
     * Get components by category
     */
    public List<ComponentRegistryEntry> getComponentsByCategory(String category) {
        return componentRegistryRepository.findByCategoryAndIsActiveTrue(category);
    }

    /**
     * Get a specific component
     */
    public Optional<ComponentRegistryEntry> getComponent(String pluginId, String componentId) {
        return componentRegistryRepository.findByPluginIdAndComponentId(pluginId, componentId);
    }

    /**
     * Get component manifest
     */
    public Optional<ComponentManifest> getComponentManifest(String pluginId, String componentId) {
        return getComponent(pluginId, componentId)
                .map(entry -> {
                    try {
                        return objectMapper.readValue(entry.getComponentManifest(), ComponentManifest.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize component manifest", e);
                        return null;
                    }
                });
    }

    /**
     * Get all components from a specific plugin
     */
    public List<ComponentRegistryEntry> getPluginComponents(String pluginId) {
        return componentRegistryRepository.findByPluginId(pluginId);
    }

    /**
     * Deactivate a component
     */
    @Transactional
    public void deactivateComponent(String pluginId, String componentId) {
        log.info("Deactivating component: {} from plugin: {}", componentId, pluginId);
        componentRegistryRepository.findByPluginIdAndComponentId(pluginId, componentId)
                .ifPresent(entry -> {
                    entry.setIsActive(false);
                    componentRegistryRepository.save(entry);
                });
    }

    /**
     * Activate a component
     */
    @Transactional
    public void activateComponent(String pluginId, String componentId) {
        log.info("Activating component: {} from plugin: {}", componentId, pluginId);
        componentRegistryRepository.findByPluginIdAndComponentId(pluginId, componentId)
                .ifPresent(entry -> {
                    entry.setIsActive(true);
                    componentRegistryRepository.save(entry);
                });
    }

    /**
     * Unregister all components from a plugin
     */
    @Transactional
    public void unregisterPluginComponents(String pluginId) {
        log.info("Unregistering all components from plugin: {}", pluginId);
        componentRegistryRepository.deleteByPluginId(pluginId);
    }

    /**
     * Unregister a specific component
     */
    @Transactional
    public void unregisterComponent(String pluginId, String componentId) {
        log.info("Unregistering component: {} from plugin: {}", componentId, pluginId);
        componentRegistryRepository.deleteByPluginIdAndComponentId(pluginId, componentId);
    }

    /**
     * Check if a component is registered
     */
    public boolean isComponentRegistered(String pluginId, String componentId) {
        return componentRegistryRepository.existsByPluginIdAndComponentId(pluginId, componentId);
    }

    /**
     * Get component categories
     */
    public List<String> getCategories() {
        return List.of("ui", "layout", "form", "widget", "navbar", "data");
    }

    /**
     * Register multiple components from a single plugin
     * Useful for plugins that provide multiple component variants
     */
    @Transactional
    public List<ComponentRegistryEntry> registerComponents(List<ComponentManifest> manifests) {
        log.info("Registering {} components", manifests.size());
        return manifests.stream()
                .map(this::registerComponent)
                .toList();
    }

    /**
     * Get all components including inactive ones (admin only)
     */
    public List<ComponentRegistryEntry> getAllComponentsIncludingInactive() {
        return componentRegistryRepository.findAll();
    }

    /**
     * Virtual plugin ID mappings - maps actual plugin IDs to their virtual aliases.
     * Components can be stored in pages with either the actual or virtual pluginId.
     */
    private static final Map<String, List<String>> PLUGIN_ALIASES = Map.of(
            "label-component-plugin", List.of("core-ui"),
            "button-component-plugin", List.of("core-ui"),
            "container-layout-plugin", List.of("core-ui", "core-layout"),
            "textbox-component-plugin", List.of("core-ui"),
            "image-component-plugin", List.of("core-ui"),
            "navbar-component-plugin", List.of("core-navbar")
    );

    /**
     * Find pages that use a specific component.
     * Scans through active page versions to find component usage.
     * Also checks for virtual plugin aliases (e.g., 'core-ui' -> 'label-component-plugin').
     *
     * @param pluginId the plugin ID
     * @param componentId the component ID
     * @return list of maps containing page info: {pageId, pageName, siteName, siteId}
     */
    public List<Map<String, Object>> findPagesUsingComponent(String pluginId, String componentId) {
        log.info("Searching for pages using component: {} from plugin: {}", componentId, pluginId);

        List<Map<String, Object>> result = new ArrayList<>();

        // Build list of plugin IDs to search for (actual + aliases)
        Set<String> pluginIdsToSearch = new HashSet<>();
        pluginIdsToSearch.add(pluginId);

        // Add virtual plugin aliases
        List<String> aliases = PLUGIN_ALIASES.get(pluginId);
        if (aliases != null) {
            pluginIdsToSearch.addAll(aliases);
        }

        // Also add reverse lookup - if searching for alias, include actual plugin IDs
        for (Map.Entry<String, List<String>> entry : PLUGIN_ALIASES.entrySet()) {
            if (entry.getValue().contains(pluginId)) {
                pluginIdsToSearch.add(entry.getKey());
            }
        }

        log.info("Searching for component {} with pluginIds: {}", componentId, pluginIdsToSearch);

        // Get ALL page versions - we want to find usage regardless of active status
        // This ensures we find components even in draft/inactive versions
        List<PageVersion> allVersions = pageVersionRepository.findAll();
        log.info("Total page versions in database: {}", allVersions.size());

        // Deduplicate by pageId - only keep the latest version per page to avoid duplicates
        Map<Long, PageVersion> latestVersionByPage = new HashMap<>();
        for (PageVersion version : allVersions) {
            Long pageId = version.getPage().getId();
            PageVersion existing = latestVersionByPage.get(pageId);
            if (existing == null || version.getVersionNumber() > existing.getVersionNumber()) {
                latestVersionByPage.put(pageId, version);
            }
        }

        List<PageVersion> versionsToSearch = new ArrayList<>(latestVersionByPage.values());
        log.info("Searching {} unique pages (latest version per page)", versionsToSearch.size());

        for (PageVersion version : versionsToSearch) {
            try {
                String pageDefinition = version.getPageDefinition();
                if (pageDefinition != null) {
                    // Log first 200 chars of page definition for debugging
                    if (log.isDebugEnabled()) {
                        log.debug("Checking page {} definition: {}...",
                                version.getPage().getId(),
                                pageDefinition.substring(0, Math.min(200, pageDefinition.length())));
                    }

                    if (containsComponentWithAnyPluginId(pageDefinition, pluginIdsToSearch, componentId)) {
                        Map<String, Object> pageInfo = new HashMap<>();
                        pageInfo.put("pageId", version.getPage().getId());
                        pageInfo.put("pageName", version.getPage().getPageName());
                        pageInfo.put("pageSlug", version.getPage().getPageSlug());
                        pageInfo.put("siteId", version.getPage().getSite().getId());
                        pageInfo.put("siteName", version.getPage().getSite().getSiteName());
                        result.add(pageInfo);
                        log.info("Found component {} in page: {} ({})",
                                componentId, version.getPage().getPageName(), version.getPage().getId());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse page definition for page: {}", version.getPage().getId(), e);
            }
        }

        log.info("Found {} pages using component: {}", result.size(), componentId);
        return result;
    }

    /**
     * Check if a page definition JSON contains a specific component with any of the given plugin IDs.
     */
    private boolean containsComponentWithAnyPluginId(String pageDefinitionJson, Set<String> pluginIds, String componentId) {
        try {
            JsonNode rootNode = objectMapper.readTree(pageDefinitionJson);
            return searchForComponentWithAnyPluginId(rootNode, pluginIds, componentId);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse page definition JSON", e);
            return false;
        }
    }

    /**
     * Recursively search for a component in the JSON tree, matching any of the given plugin IDs.
     */
    private boolean searchForComponentWithAnyPluginId(JsonNode node, Set<String> pluginIds, String componentId) {
        if (node == null) {
            return false;
        }

        // Check if this node is a component with matching pluginId and componentId
        if (node.isObject()) {
            JsonNode pluginIdNode = node.get("pluginId");
            JsonNode componentIdNode = node.get("componentId");

            if (pluginIdNode != null && componentIdNode != null) {
                String nodePluginId = pluginIdNode.asText();
                String nodeComponentId = componentIdNode.asText();

                log.debug("Found component in JSON: pluginId={}, componentId={}", nodePluginId, nodeComponentId);

                // Match if componentId matches (case-insensitive) AND pluginId is in the set of IDs to search
                if (componentId.equalsIgnoreCase(nodeComponentId) && pluginIds.contains(nodePluginId)) {
                    log.info("MATCH! Component {} found with pluginId {} (searching for: {})",
                            componentId, nodePluginId, pluginIds);
                    return true;
                }

                // Log near-misses for debugging
                if (componentId.equalsIgnoreCase(nodeComponentId)) {
                    log.debug("Component ID matches but pluginId {} not in search set {}", nodePluginId, pluginIds);
                }
            }

            // Search in all fields of this object
            for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
                if (searchForComponentWithAnyPluginId(it.next(), pluginIds, componentId)) {
                    return true;
                }
            }
        } else if (node.isArray()) {
            // Search in array elements
            for (JsonNode element : node) {
                if (searchForComponentWithAnyPluginId(element, pluginIds, componentId)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if a page definition JSON contains a specific component.
     */
    private boolean containsComponent(String pageDefinitionJson, String pluginId, String componentId) {
        try {
            JsonNode rootNode = objectMapper.readTree(pageDefinitionJson);
            return searchForComponent(rootNode, pluginId, componentId);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse page definition JSON", e);
            return false;
        }
    }

    /**
     * Recursively search for a component in the JSON tree.
     */
    private boolean searchForComponent(JsonNode node, String pluginId, String componentId) {
        if (node == null) {
            return false;
        }

        // Check if this node is a component with matching pluginId and componentId
        if (node.isObject()) {
            JsonNode pluginIdNode = node.get("pluginId");
            JsonNode componentIdNode = node.get("componentId");

            if (pluginIdNode != null && componentIdNode != null) {
                String nodePluginId = pluginIdNode.asText();
                String nodeComponentId = componentIdNode.asText();

                if (pluginId.equals(nodePluginId) && componentId.equals(nodeComponentId)) {
                    return true;
                }
            }

            // Search in all fields of this object
            for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
                if (searchForComponent(it.next(), pluginId, componentId)) {
                    return true;
                }
            }
        } else if (node.isArray()) {
            // Search in array elements
            for (JsonNode element : node) {
                if (searchForComponent(element, pluginId, componentId)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Validate page components - check if any components are deactivated.
     *
     * @param pageDefinitionJson the page definition JSON
     * @return list of deactivated components found in the page
     */
    public List<Map<String, Object>> validatePageComponents(String pageDefinitionJson) {
        List<Map<String, Object>> deactivatedComponents = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(pageDefinitionJson);
            findDeactivatedComponents(rootNode, deactivatedComponents);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse page definition JSON for validation", e);
        }

        return deactivatedComponents;
    }

    /**
     * Recursively find deactivated components in the JSON tree.
     */
    private void findDeactivatedComponents(JsonNode node, List<Map<String, Object>> result) {
        if (node == null) {
            return;
        }

        if (node.isObject()) {
            JsonNode pluginIdNode = node.get("pluginId");
            JsonNode componentIdNode = node.get("componentId");

            if (pluginIdNode != null && componentIdNode != null) {
                String pluginId = pluginIdNode.asText();
                String componentId = componentIdNode.asText();

                // Check if this component is inactive
                Optional<ComponentRegistryEntry> entry = getComponent(pluginId, componentId);
                if (entry.isPresent() && !entry.get().getIsActive()) {
                    Map<String, Object> componentInfo = new HashMap<>();
                    componentInfo.put("pluginId", pluginId);
                    componentInfo.put("componentId", componentId);
                    componentInfo.put("componentName", entry.get().getComponentName());
                    componentInfo.put("instanceId", node.has("instanceId") ? node.get("instanceId").asText() : null);
                    result.add(componentInfo);
                }
            }

            // Search in all fields of this object
            for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
                findDeactivatedComponents(it.next(), result);
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                findDeactivatedComponents(element, result);
            }
        }
    }
}
