package dev.mainul35.plugins.ui.controller;

import dev.mainul35.plugins.entities.label.DynamicLabel;
import dev.mainul35.plugins.ui.service.DynamicLabelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for dynamic label content.
 * Provides API endpoints for fetching and managing label content.
 *
 * Base path: /api/labels
 */
@RestController
@RequestMapping("/api/labels")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class DynamicLabelController {

    private static final Logger log = LoggerFactory.getLogger(DynamicLabelController.class);

    private final DynamicLabelService labelService;

    public DynamicLabelController(DynamicLabelService labelService) {
        this.labelService = labelService;
    }

    /**
     * Get label content by key
     *
     * GET /api/labels/{key}
     * GET /api/labels/{key}?lang=en
     *
     * Response: { "key": "...", "content": "...", "language": "..." }
     */
    @GetMapping("/{key}")
    public ResponseEntity<?> getLabelByKey(
            @PathVariable String key,
            @RequestParam(name = "lang", defaultValue = "en") String language) {

        log.debug("Fetching label for key: {}, language: {}", key, language);

        try {
            return labelService.getLabelByKey(key, language)
                    .map(label -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("key", label.getContentKey());
                        response.put("content", label.getContent());
                        response.put("language", label.getLanguage());
                        response.put("title", label.getTitle());
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        log.debug("Label not found for key: {}", key);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            log.error("Error fetching label for key: {}", key, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch label content"));
        }
    }

    /**
     * Get all labels (optionally filtered by language)
     *
     * GET /api/labels
     * GET /api/labels?lang=en
     */
    @GetMapping
    public ResponseEntity<?> getAllLabels(
            @RequestParam(name = "lang", required = false) String language) {

        log.debug("Fetching all labels, language filter: {}", language);

        try {
            List<DynamicLabel> labels = language != null
                    ? labelService.getAllLabels(language)
                    : labelService.getAllLabels();

            List<Map<String, Object>> response = labels.stream()
                    .map(label -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", label.getId());
                        map.put("key", label.getContentKey());
                        map.put("content", label.getContent());
                        map.put("language", label.getLanguage());
                        map.put("title", label.getTitle());
                        map.put("active", label.isActive());
                        return map;
                    })
                    .toList();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching all labels", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch labels"));
        }
    }

    /**
     * Create a new label
     *
     * POST /api/labels
     * Body: { "key": "...", "content": "...", "language": "en", "title": "...", "description": "..." }
     */
    @PostMapping
    public ResponseEntity<?> createLabel(@RequestBody Map<String, String> request) {
        log.info("Creating new label: {}", request.get("key"));

        try {
            String key = request.get("key");
            String content = request.get("content");
            String language = request.getOrDefault("language", "en");
            String title = request.get("title");
            String description = request.get("description");

            if (key == null || key.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Content key is required"));
            }

            if (labelService.labelExists(key, language)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Label with this key and language already exists"));
            }

            DynamicLabel label = labelService.createLabel(key, content, language, title, description);

            Map<String, Object> response = new HashMap<>();
            response.put("id", label.getId());
            response.put("key", label.getContentKey());
            response.put("content", label.getContent());
            response.put("language", label.getLanguage());
            response.put("title", label.getTitle());
            response.put("message", "Label created successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating label", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create label"));
        }
    }

    /**
     * Update an existing label
     *
     * PUT /api/labels/{id}
     * Body: { "content": "...", "title": "...", "description": "...", "active": true }
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLabel(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        log.info("Updating label with id: {}", id);

        try {
            String content = (String) request.get("content");
            String title = (String) request.get("title");
            String description = (String) request.get("description");
            Boolean active = (Boolean) request.get("active");

            return labelService.updateLabel(id, content, title, description, active)
                    .map(label -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("id", label.getId());
                        response.put("key", label.getContentKey());
                        response.put("content", label.getContent());
                        response.put("language", label.getLanguage());
                        response.put("title", label.getTitle());
                        response.put("active", label.isActive());
                        response.put("message", "Label updated successfully");
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error updating label with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update label"));
        }
    }

    /**
     * Delete a label
     *
     * DELETE /api/labels/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLabel(@PathVariable Long id) {
        log.info("Deleting label with id: {}", id);

        try {
            if (labelService.deleteLabel(id)) {
                return ResponseEntity.ok(Map.of("message", "Label deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error deleting label with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete label"));
        }
    }
}
