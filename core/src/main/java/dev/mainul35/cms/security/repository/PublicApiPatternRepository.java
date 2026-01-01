package dev.mainul35.cms.security.repository;

import dev.mainul35.cms.security.entity.PublicApiPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing public API patterns.
 */
@Repository
public interface PublicApiPatternRepository extends JpaRepository<PublicApiPattern, Long> {

    /**
     * Find all enabled patterns.
     */
    List<PublicApiPattern> findByEnabledTrue();

    /**
     * Find pattern by exact pattern string.
     */
    Optional<PublicApiPattern> findByPattern(String pattern);

    /**
     * Check if a pattern exists.
     */
    boolean existsByPattern(String pattern);

    /**
     * Find all enabled patterns ordered by pattern.
     */
    @Query("SELECT p FROM PublicApiPattern p WHERE p.enabled = true ORDER BY p.pattern")
    List<PublicApiPattern> findAllEnabledOrderedByPattern();
}
