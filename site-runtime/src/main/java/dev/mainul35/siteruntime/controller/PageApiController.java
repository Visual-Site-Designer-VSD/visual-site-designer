package dev.mainul35.siteruntime.controller;

import dev.mainul35.siteruntime.service.PageDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API controller for serving page data to the React SPA.
 * Replaces Thymeleaf server-side rendering with JSON APIs.
 */
@Slf4j
@RestController
@RequestMapping("/api/pages")
@RequiredArgsConstructor
public class PageApiController {

    private final PageDataService pageDataService;

    /**
     * Get page data by page name.
     * Returns page metadata + all resolved data sources as JSON.
     */
    @GetMapping("/{pageName}")
    public ResponseEntity<?> getPageData(
            @PathVariable String pageName,
            @RequestParam(required = false) Map<String, String> params) {
        try {
            var pageData = (params != null && !params.isEmpty())
                    ? pageDataService.loadPageData(pageName, params)
                    : pageDataService.loadPageData(pageName);
            return ResponseEntity.ok(pageData);
        } catch (Exception e) {
            log.error("Failed to load page data for: {}", pageName, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Fetch a single data source for a page.
     * Useful for lazy-loading or refreshing individual data sections.
     */
    @GetMapping("/{pageName}/data/{dataSourceKey}")
    public ResponseEntity<?> getDataSource(
            @PathVariable String pageName,
            @PathVariable String dataSourceKey,
            @RequestParam(required = false) Map<String, String> params) {
        try {
            var data = pageDataService.fetchDataSource(pageName, dataSourceKey, params);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Failed to fetch data source '{}' for page '{}'", dataSourceKey, pageName, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to fetch data source: " + dataSourceKey));
        }
    }

    /**
     * Clear all cached page data.
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<?> clearCache() {
        pageDataService.clearCache();
        return ResponseEntity.ok(Map.of("message", "Cache cleared"));
    }
}
