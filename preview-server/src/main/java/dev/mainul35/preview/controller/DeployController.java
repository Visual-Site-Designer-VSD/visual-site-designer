package dev.mainul35.preview.controller;

import dev.mainul35.preview.dto.DeploymentStatus;
import dev.mainul35.preview.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * REST controller for managing snapshot deployments.
 * The CMS uploads site snapshots (zip) to this endpoint.
 */
@Slf4j
@RestController
@RequestMapping("/api/deploy")
@RequiredArgsConstructor
public class DeployController {

    private final DeploymentService deploymentService;

    /**
     * Deploy a site snapshot.
     * Accepts a zip file containing pages, plugin assets, and plugin JARs.
     */
    @PostMapping
    public ResponseEntity<?> deploy(
            @RequestParam("snapshot") MultipartFile snapshot,
            @RequestParam(value = "siteId", required = false) Long siteId) {
        try {
            log.info("Receiving snapshot deployment: size={} bytes, siteId={}", snapshot.getSize(), siteId);
            DeploymentStatus status = deploymentService.deploy(snapshot, siteId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Deployment failed", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Deployment failed: " + e.getMessage()));
        }
    }

    /**
     * Get current deployment status.
     */
    @GetMapping("/status")
    public ResponseEntity<DeploymentStatus> getStatus() {
        return ResponseEntity.ok(deploymentService.getStatus());
    }

    /**
     * Reload pages from the data directory (clear cache).
     */
    @PostMapping("/reload")
    public ResponseEntity<DeploymentStatus> reload() {
        return ResponseEntity.ok(deploymentService.reload());
    }
}
