package dev.mainul35.plugins.controller;

import dev.mainul35.cms.sdk.annotation.PluginController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for test-component-plugin
 * Base path: /api/plugins/test-component-plugin/testcomponent
 */
@PluginController(basePath = "testcomponent")
@RequestMapping("/testcomponent")
public class TestComponentController extends dev.mainul35.cms.sdk.controller.PluginController {

    /**
     * GET /testcomponent
     * Example endpoint - returns a simple response
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Hello from test-component-plugin!");
        response.put("pluginId", getPluginId());

        logInfo("GET request received for /testcomponent");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /testcomponent/{id}
     * Example endpoint - returns item by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("id", id);
        response.put("pluginId", getPluginId());

        logInfo("GET request received for /testcomponent/{}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /testcomponent
     * Example endpoint - creates a new item
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "created");
        response.put("data", body);
        response.put("pluginId", getPluginId());

        logInfo("POST request received for /testcomponent");
        return ResponseEntity.status(201).body(response);
    }
}
