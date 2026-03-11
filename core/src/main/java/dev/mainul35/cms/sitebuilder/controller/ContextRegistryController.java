package dev.mainul35.cms.sitebuilder.controller;

import dev.mainul35.cms.sdk.context.ContextDescriptor;
import dev.mainul35.cms.sitebuilder.entity.ContextRegistryEntry;
import dev.mainul35.cms.sitebuilder.service.ContextRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the context provider registry.
 * Exposes endpoints for frontend discovery of active context providers.
 */
@RestController
@RequestMapping("/api/contexts")
@RequiredArgsConstructor
@Slf4j
public class ContextRegistryController {

    private final ContextRegistryService contextRegistryService;

    /**
     * Get all active context descriptors (sorted by dependency order).
     */
    @GetMapping
    public ResponseEntity<List<ContextDescriptor>> getActiveContexts() {
        List<ContextDescriptor> descriptors = contextRegistryService.getActiveContextDescriptors();

        // Sort by dependency order for frontend provider tree
        try {
            List<ContextDescriptor> sorted = contextRegistryService.topologicalSort(descriptors);
            return ResponseEntity.ok(sorted);
        } catch (IllegalStateException e) {
            log.error("Context dependency error: {}", e.getMessage());
            // Return unsorted if there's a cycle (shouldn't happen if validated at load time)
            return ResponseEntity.ok(descriptors);
        }
    }

    /**
     * Get a specific context descriptor by context ID.
     */
    @GetMapping("/{contextId}")
    public ResponseEntity<ContextDescriptor> getContext(@PathVariable String contextId) {
        return contextRegistryService.getContextDescriptor(contextId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all contexts from a specific plugin.
     */
    @GetMapping("/plugin/{pluginId}")
    public ResponseEntity<List<ContextRegistryEntry>> getPluginContexts(@PathVariable String pluginId) {
        return ResponseEntity.ok(contextRegistryService.getPluginContexts(pluginId));
    }

    /**
     * Validate that a list of required contexts are all available.
     */
    @PostMapping("/validate")
    public ResponseEntity<List<String>> validateRequiredContexts(@RequestBody List<String> requiredContexts) {
        List<String> missing = contextRegistryService.validateRequiredContexts(requiredContexts);
        return ResponseEntity.ok(missing);
    }
}
