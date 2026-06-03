package dev.mainul35.cms.sitebuilder.controller;

import dev.mainul35.cms.sitebuilder.dto.PreviewStatus;
import dev.mainul35.cms.sitebuilder.service.PreviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for deploying site snapshots to the live preview server.
 * The preview server itself is started/stopped externally (via IntelliJ plugin).
 */
@Slf4j
@RestController
@RequestMapping("/api/preview")
@RequiredArgsConstructor
public class PreviewController {

    private final PreviewService previewService;

    /**
     * Deploy a site snapshot to the running preview server.
     */
    @PostMapping("/deploy")
    public ResponseEntity<PreviewStatus> deployToPreview(@RequestParam Long siteId) {
        log.info("Deploying site {} to preview server", siteId);
        PreviewStatus status = previewService.deployToPreview(siteId);
        if (status.error() != null) {
            return ResponseEntity.status(502).body(status);
        }
        return ResponseEntity.ok(status);
    }

    /**
     * Get current deployment status from the preview server.
     */
    @GetMapping("/status")
    public ResponseEntity<PreviewStatus> getPreviewStatus() {
        return ResponseEntity.ok(previewService.getPreviewStatus());
    }

    /**
     * Redeploy the site snapshot to the preview server.
     */
    @PostMapping("/refresh")
    public ResponseEntity<PreviewStatus> refreshPreview(@RequestParam Long siteId) {
        log.info("Redeploying site {} to preview server", siteId);
        PreviewStatus status = previewService.refreshPreview(siteId);
        if (status.error() != null) {
            return ResponseEntity.status(502).body(status);
        }
        return ResponseEntity.ok(status);
    }
}
