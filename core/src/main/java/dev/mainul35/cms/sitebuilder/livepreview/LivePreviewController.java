package dev.mainul35.cms.sitebuilder.livepreview;

import dev.mainul35.cms.security.filter.JwtAuthenticationFilter.JwtUserPrincipal;
import dev.mainul35.cms.sitebuilder.service.SiteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * "Run on live server" for the builder. Starting needs ownership of the site; stopping and heartbeats need to be
 * the user who started the preview. Everything else about the preview stays on the server.
 */
@RestController
@RequestMapping("/api")
public class LivePreviewController {

    private final LivePreviewService live;
    private final SiteService siteService;

    public LivePreviewController(LivePreviewService live, SiteService siteService) {
        this.live = live;
        this.siteService = siteService;
    }

    /** Builds a snapshot of the site's saved pages and starts the runtime for it. Takes a few seconds. */
    @PostMapping("/sites/{siteId}/live-preview")
    public ResponseEntity<?> start(@PathVariable Long siteId, @AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You don't have access to this site"));
        }
        try {
            return ResponseEntity.ok(live.start(siteId, principal.userId()));
        } catch (LivePreviewException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/live-preview")
    public ResponseEntity<?> status(@AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Only the designer who started it may see how to reach it.
        return ResponseEntity.ok(live.statusFor(principal.userId()));
    }

    /** The builder is still showing the preview. */
    @PostMapping("/live-preview/heartbeat")
    public ResponseEntity<?> heartbeat(@AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("running", live.heartbeat(principal.userId())));
    }

    /** The designer closed the preview: stop the runtime now. */
    @DeleteMapping("/live-preview")
    public ResponseEntity<?> stop(@AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("stopped", live.stop(principal.userId())));
    }
}
