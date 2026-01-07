package dev.mainul35.cms.sitebuilder.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.cms.security.filter.JwtAuthenticationFilter.JwtUserPrincipal;
import dev.mainul35.cms.sitebuilder.dto.CreatePageRequest;
import dev.mainul35.cms.sitebuilder.dto.PageDto;
import dev.mainul35.cms.sitebuilder.dto.PageVersionDto;
import dev.mainul35.cms.sitebuilder.dto.SavePageVersionRequest;
import dev.mainul35.cms.sitebuilder.dto.UpdatePageRequest;
import dev.mainul35.cms.sitebuilder.service.ComponentRegistryService;
import dev.mainul35.cms.sitebuilder.service.PageService;
import dev.mainul35.cms.sitebuilder.service.PageVersionService;
import dev.mainul35.cms.sitebuilder.service.SiteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Page management within a site
 */
@RestController
@RequestMapping("/api/sites/{siteId}/pages")
@CrossOrigin
public class PageController {

    private final PageService pageService;
    private final PageVersionService pageVersionService;
    private final SiteService siteService;
    private final ComponentRegistryService componentRegistryService;
    private final ObjectMapper objectMapper;

    public PageController(PageService pageService, PageVersionService pageVersionService,
                          SiteService siteService, ComponentRegistryService componentRegistryService,
                          ObjectMapper objectMapper) {
        this.pageService = pageService;
        this.pageVersionService = pageVersionService;
        this.siteService = siteService;
        this.componentRegistryService = componentRegistryService;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all pages for a site
     */
    @GetMapping
    public ResponseEntity<?> getAllPages(
            @PathVariable Long siteId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Verify site ownership
        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        List<PageDto> pages = pageService.getPagesBySite(siteId);
        return ResponseEntity.ok(pages);
    }

    /**
     * Get a page by ID
     */
    @GetMapping("/{pageId}")
    public ResponseEntity<?> getPageById(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        return pageService.getPageById(siteId, pageId)
                .map(page -> ResponseEntity.ok(page))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get a page by slug
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getPageBySlug(
            @PathVariable Long siteId,
            @PathVariable String slug,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        return pageService.getPageBySlug(siteId, slug)
                .map(page -> ResponseEntity.ok(page))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new page
     */
    @PostMapping
    public ResponseEntity<?> createPage(
            @PathVariable Long siteId,
            @Valid @RequestBody CreatePageRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        try {
            PageDto page = pageService.createPage(siteId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(page);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create page: " + e.getMessage()));
        }
    }

    /**
     * Update a page
     */
    @PutMapping("/{pageId}")
    public ResponseEntity<?> updatePage(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @Valid @RequestBody UpdatePageRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        return pageService.updatePage(siteId, pageId, request)
                .map(page -> ResponseEntity.ok(page))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a page
     */
    @DeleteMapping("/{pageId}")
    public ResponseEntity<?> deletePage(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        if (pageService.deletePage(siteId, pageId)) {
            return ResponseEntity.ok(Map.of("message", "Page deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Publish a page
     */
    @PostMapping("/{pageId}/publish")
    public ResponseEntity<?> publishPage(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        return pageService.publishPage(siteId, pageId)
                .map(page -> ResponseEntity.ok(page))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Unpublish a page
     */
    @PostMapping("/{pageId}/unpublish")
    public ResponseEntity<?> unpublishPage(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        return pageService.unpublishPage(siteId, pageId)
                .map(page -> ResponseEntity.ok(page))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Duplicate a page
     */
    @PostMapping("/{pageId}/duplicate")
    public ResponseEntity<?> duplicatePage(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        String newPageName = body != null ? body.get("newPageName") : null;
        return pageService.duplicatePage(siteId, pageId, newPageName)
                .map(page -> ResponseEntity.status(HttpStatus.CREATED).body(page))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get child pages of a parent page
     */
    @GetMapping("/{parentPageId}/children")
    public ResponseEntity<?> getChildPages(
            @PathVariable Long siteId,
            @PathVariable Long parentPageId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        List<PageDto> children = pageService.getChildPages(siteId, parentPageId);
        return ResponseEntity.ok(children);
    }

    /**
     * Reorder pages
     */
    @PostMapping("/reorder")
    public ResponseEntity<?> reorderPages(
            @PathVariable Long siteId,
            @RequestBody Map<String, List<PageService.PageReorderItem>> body,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        List<PageService.PageReorderItem> pageOrders = body.get("pageOrders");
        if (pageOrders != null) {
            pageService.reorderPages(siteId, pageOrders);
        }

        return ResponseEntity.ok(Map.of("message", "Pages reordered successfully"));
    }

    // ========== Page Version Endpoints ==========

    /**
     * Get page definition (JSON) from the active version
     * Parses the stored JSON string and returns it as a proper JSON object
     */
    @GetMapping(value = "/{pageId}/definition", produces = "application/json")
    public ResponseEntity<?> getPageDefinition(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        return pageVersionService.getPageDefinition(pageId)
                .map(definition -> {
                    try {
                        // Parse JSON string to JsonNode to avoid double-encoding
                        JsonNode jsonNode = objectMapper.readTree(definition);
                        return ResponseEntity.ok(jsonNode);
                    } catch (JsonProcessingException e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Failed to parse page definition"));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Save a new page version
     */
    @PostMapping("/{pageId}/versions")
    public ResponseEntity<?> savePageVersion(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @Valid @RequestBody SavePageVersionRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        try {
            PageVersionDto version = pageVersionService.savePageVersion(
                    siteId,
                    pageId,
                    request.getPageDefinition(),
                    request.getChangeDescription(),
                    principal.userId()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(version);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to save version: " + e.getMessage()));
        }
    }

    /**
     * Get all versions for a page
     */
    @GetMapping("/{pageId}/versions")
    public ResponseEntity<?> getVersionHistory(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        return ResponseEntity.ok(pageVersionService.getVersionHistory(pageId));
    }

    /**
     * Get the active version for a page
     */
    @GetMapping("/{pageId}/versions/active")
    public ResponseEntity<?> getActiveVersion(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        return pageVersionService.getActiveVersion(pageId)
                .map(version -> ResponseEntity.ok(version))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get a specific version by ID
     */
    @GetMapping("/{pageId}/versions/{versionId}")
    public ResponseEntity<?> getVersion(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @PathVariable Long versionId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        return pageVersionService.getVersion(pageId, versionId)
                .map(version -> ResponseEntity.ok(version))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Restore a page to a specific version
     */
    @PostMapping("/{pageId}/versions/{versionId}/restore")
    public ResponseEntity<?> restoreVersion(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @PathVariable Long versionId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        return pageVersionService.restoreVersion(siteId, pageId, versionId, principal.userId())
                .map(version -> ResponseEntity.ok(version))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a specific version (cannot delete active version)
     */
    @DeleteMapping("/{pageId}/versions/{versionId}")
    public ResponseEntity<?> deleteVersion(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @PathVariable Long versionId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        try {
            if (pageVersionService.deleteVersion(pageId, versionId)) {
                return ResponseEntity.ok(Map.of("message", "Version deleted successfully"));
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== Component Validation Endpoints ==========

    /**
     * Validate page components - check if any components are deactivated.
     * Returns list of deactivated components found in the page.
     * Use this before allowing page editing to block editing pages with deactivated components.
     */
    @PostMapping("/{pageId}/validate-components")
    public ResponseEntity<?> validatePageComponents(
            @PathVariable Long siteId,
            @PathVariable Long pageId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!siteService.isOwner(siteId, principal.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You don't have access to this site"));
        }

        // Get the active page definition
        return pageVersionService.getPageDefinition(pageId)
                .map(definition -> {
                    List<Map<String, Object>> deactivatedComponents =
                            componentRegistryService.validatePageComponents(definition);

                    boolean isValid = deactivatedComponents.isEmpty();

                    return ResponseEntity.ok(Map.of(
                            "valid", isValid,
                            "deactivatedComponents", deactivatedComponents,
                            "deactivatedCount", deactivatedComponents.size()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
