package dev.mainul35.cms.sitebuilder.runtimelink;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets the builder UI ask the live runtime which shared state is declared. Signed-in builder users only
 * (the {@code /api/**} rule). The admin token stays on the server; the browser never sees it.
 */
@RestController
@RequestMapping("/api/runtime")
public class RuntimeLinkController {

    private final RuntimeLinkService link;

    public RuntimeLinkController(RuntimeLinkService link) {
        this.link = link;
    }

    @GetMapping("/shared-state")
    public ResponseEntity<RuntimeLinkService.Catalog> sharedState() {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(link.sharedState());
    }
}
