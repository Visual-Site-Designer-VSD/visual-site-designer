package dev.mainul35.cms.security.service;

import dev.mainul35.cms.security.entity.PublicApiPattern;
import dev.mainul35.cms.security.repository.PublicApiPatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing public API patterns.
 * Patterns are cached for performance but cache is invalidated on any change.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PublicApiPatternService {

    private final PublicApiPatternRepository repository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Get all patterns (including disabled).
     */
    public List<PublicApiPattern> getAllPatterns() {
        return repository.findAll();
    }

    /**
     * Get all enabled patterns (cached).
     */
    @Cacheable(value = "publicApiPatterns", key = "'enabled'")
    public List<PublicApiPattern> getEnabledPatterns() {
        log.debug("Loading enabled public API patterns from database");
        return repository.findAllEnabledOrderedByPattern();
    }

    /**
     * Check if a request path matches any enabled public pattern.
     * @param requestPath The request path (e.g., /api/sample/products)
     * @param httpMethod The HTTP method (e.g., GET, POST)
     * @return true if the path is publicly accessible
     */
    public boolean isPublicPath(String requestPath, String httpMethod) {
        List<PublicApiPattern> patterns = getEnabledPatterns();

        for (PublicApiPattern pattern : patterns) {
            if (matchesPattern(requestPath, pattern.getPattern())) {
                if (matchesMethod(httpMethod, pattern.getHttpMethods())) {
                    log.debug("Path {} matches public pattern {}", requestPath, pattern.getPattern());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a path matches a pattern using Ant-style matching.
     */
    private boolean matchesPattern(String path, String pattern) {
        return pathMatcher.match(pattern, path);
    }

    /**
     * Check if an HTTP method matches the allowed methods.
     */
    private boolean matchesMethod(String method, String allowedMethods) {
        if (allowedMethods == null || allowedMethods.equals("*")) {
            return true;
        }
        String[] methods = allowedMethods.toUpperCase().split(",");
        String upperMethod = method.toUpperCase().trim();
        for (String m : methods) {
            if (m.trim().equals(upperMethod)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a new pattern.
     */
    @Transactional
    @CacheEvict(value = "publicApiPatterns", allEntries = true)
    public PublicApiPattern createPattern(PublicApiPattern pattern) {
        if (repository.existsByPattern(pattern.getPattern())) {
            throw new IllegalArgumentException("Pattern already exists: " + pattern.getPattern());
        }
        log.info("Creating public API pattern: {}", pattern.getPattern());
        return repository.save(pattern);
    }

    /**
     * Update an existing pattern.
     */
    @Transactional
    @CacheEvict(value = "publicApiPatterns", allEntries = true)
    public PublicApiPattern updatePattern(Long id, PublicApiPattern updated) {
        PublicApiPattern existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pattern not found: " + id));

        // Check if new pattern conflicts with another existing pattern
        if (!existing.getPattern().equals(updated.getPattern())
                && repository.existsByPattern(updated.getPattern())) {
            throw new IllegalArgumentException("Pattern already exists: " + updated.getPattern());
        }

        existing.setPattern(updated.getPattern());
        existing.setHttpMethods(updated.getHttpMethods());
        existing.setDescription(updated.getDescription());
        existing.setEnabled(updated.isEnabled());

        log.info("Updated public API pattern: {}", existing.getPattern());
        return repository.save(existing);
    }

    /**
     * Delete a pattern.
     */
    @Transactional
    @CacheEvict(value = "publicApiPatterns", allEntries = true)
    public void deletePattern(Long id) {
        PublicApiPattern pattern = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pattern not found: " + id));
        log.info("Deleting public API pattern: {}", pattern.getPattern());
        repository.delete(pattern);
    }

    /**
     * Enable or disable a pattern.
     */
    @Transactional
    @CacheEvict(value = "publicApiPatterns", allEntries = true)
    public PublicApiPattern setEnabled(Long id, boolean enabled) {
        PublicApiPattern pattern = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pattern not found: " + id));
        pattern.setEnabled(enabled);
        log.info("{} public API pattern: {}", enabled ? "Enabled" : "Disabled", pattern.getPattern());
        return repository.save(pattern);
    }

    /**
     * Get pattern by ID.
     */
    public Optional<PublicApiPattern> getPatternById(Long id) {
        return repository.findById(id);
    }

    /**
     * Clear the cache manually (useful for testing or admin operations).
     */
    @CacheEvict(value = "publicApiPatterns", allEntries = true)
    public void clearCache() {
        log.info("Public API patterns cache cleared");
    }
}
