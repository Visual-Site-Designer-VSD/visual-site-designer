package dev.mainul35.cms.sitebuilder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.cms.sdk.context.ContextDescriptor;
import dev.mainul35.cms.sitebuilder.entity.ContextRegistryEntry;
import dev.mainul35.cms.sitebuilder.repository.ContextRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing the context provider registry.
 * Handles registration, lookup, and lifecycle of context provider plugins.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContextRegistryService {

    private final ContextRegistryRepository contextRegistryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Register a context provider from a plugin.
     */
    @Transactional
    public ContextRegistryEntry registerContext(ContextDescriptor descriptor) {
        log.info("Registering context: {} from plugin: {}", descriptor.getContextId(), descriptor.getPluginId());

        Optional<ContextRegistryEntry> existing = contextRegistryRepository
                .findByPluginIdAndContextId(descriptor.getPluginId(), descriptor.getContextId());

        ContextRegistryEntry entry;
        if (existing.isPresent()) {
            log.info("Context already exists, updating: {}", descriptor.getContextId());
            entry = existing.get();
        } else {
            entry = new ContextRegistryEntry();
            entry.setPluginId(descriptor.getPluginId());
            entry.setContextId(descriptor.getContextId());
        }

        entry.setProviderBundlePath(descriptor.getProviderComponentPath());
        entry.setIsActive(true);

        try {
            String descriptorJson = objectMapper.writeValueAsString(descriptor);
            entry.setContextDescriptor(descriptorJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize context descriptor", e);
            throw new RuntimeException("Failed to serialize context descriptor", e);
        }

        ContextRegistryEntry saved = contextRegistryRepository.save(entry);
        log.info("Context registered successfully: {}", descriptor.getContextId());
        return saved;
    }

    /**
     * Get all active contexts.
     */
    public List<ContextRegistryEntry> getActiveContexts() {
        return contextRegistryRepository.findByIsActiveTrue();
    }

    /**
     * Get all active context descriptors (deserialized).
     */
    public List<ContextDescriptor> getActiveContextDescriptors() {
        return getActiveContexts().stream()
                .map(entry -> {
                    try {
                        return objectMapper.readValue(entry.getContextDescriptor(), ContextDescriptor.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize context descriptor for: {}", entry.getContextId(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Get a specific context by context ID.
     */
    public Optional<ContextRegistryEntry> getContext(String contextId) {
        return contextRegistryRepository.findByContextId(contextId);
    }

    /**
     * Get context descriptor by context ID.
     */
    public Optional<ContextDescriptor> getContextDescriptor(String contextId) {
        return getContext(contextId)
                .map(entry -> {
                    try {
                        return objectMapper.readValue(entry.getContextDescriptor(), ContextDescriptor.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize context descriptor", e);
                        return null;
                    }
                });
    }

    /**
     * Get all contexts from a specific plugin.
     */
    public List<ContextRegistryEntry> getPluginContexts(String pluginId) {
        return contextRegistryRepository.findByPluginId(pluginId);
    }

    /**
     * Check if a context is registered.
     */
    public boolean isContextRegistered(String contextId) {
        return contextRegistryRepository.existsByContextId(contextId);
    }

    /**
     * Deactivate a context.
     */
    @Transactional
    public void deactivateContext(String pluginId, String contextId) {
        log.info("Deactivating context: {} from plugin: {}", contextId, pluginId);
        contextRegistryRepository.findByPluginIdAndContextId(pluginId, contextId)
                .ifPresent(entry -> {
                    entry.setIsActive(false);
                    contextRegistryRepository.save(entry);
                });
    }

    /**
     * Unregister all contexts from a plugin.
     */
    @Transactional
    public void unregisterPluginContexts(String pluginId) {
        log.info("Unregistering all contexts from plugin: {}", pluginId);
        contextRegistryRepository.deleteByPluginId(pluginId);
    }

    /**
     * Unregister a specific context.
     */
    @Transactional
    public void unregisterContext(String pluginId, String contextId) {
        log.info("Unregistering context: {} from plugin: {}", contextId, pluginId);
        contextRegistryRepository.deleteByPluginIdAndContextId(pluginId, contextId);
    }

    /**
     * Validate that a set of required contexts are all available.
     *
     * @param requiredContexts the context IDs to check
     * @return list of missing context IDs (empty if all are available)
     */
    public List<String> validateRequiredContexts(List<String> requiredContexts) {
        if (requiredContexts == null || requiredContexts.isEmpty()) {
            return List.of();
        }

        List<String> missing = new ArrayList<>();
        for (String contextId : requiredContexts) {
            if (!isContextRegistered(contextId)) {
                missing.add(contextId);
            }
        }
        return missing;
    }

    /**
     * Perform topological sort on context descriptors by their dependencies.
     * Returns contexts in load order (no-dependency contexts first).
     *
     * @param descriptors the context descriptors to sort
     * @return sorted list (outermost providers first)
     * @throws IllegalStateException if circular dependencies are detected
     */
    public List<ContextDescriptor> topologicalSort(List<ContextDescriptor> descriptors) {
        Map<String, ContextDescriptor> byId = new LinkedHashMap<>();
        for (ContextDescriptor desc : descriptors) {
            byId.put(desc.getContextId(), desc);
        }

        List<ContextDescriptor> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>(); // for cycle detection

        for (String contextId : byId.keySet()) {
            if (!visited.contains(contextId)) {
                topologicalSortVisit(contextId, byId, visited, visiting, sorted);
            }
        }

        return sorted;
    }

    private void topologicalSortVisit(
            String contextId,
            Map<String, ContextDescriptor> byId,
            Set<String> visited,
            Set<String> visiting,
            List<ContextDescriptor> sorted) {

        if (visiting.contains(contextId)) {
            throw new IllegalStateException("Circular context dependency detected involving: " + contextId);
        }
        if (visited.contains(contextId)) {
            return;
        }

        visiting.add(contextId);

        ContextDescriptor desc = byId.get(contextId);
        if (desc != null && desc.getRequiredContexts() != null) {
            for (String dep : desc.getRequiredContexts()) {
                topologicalSortVisit(dep, byId, visited, visiting, sorted);
            }
        }

        visiting.remove(contextId);
        visited.add(contextId);

        if (desc != null) {
            sorted.add(desc);
        }
    }
}
