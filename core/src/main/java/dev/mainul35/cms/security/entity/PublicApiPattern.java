package dev.mainul35.cms.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing a public API endpoint pattern.
 * These patterns define which API endpoints are accessible without authentication.
 * Changes take effect immediately without server restart.
 */
@Entity
@Table(name = "public_api_patterns")
@Getter
@Setter
@NoArgsConstructor
public class PublicApiPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The URL pattern (Ant-style).
     * Examples: /api/sample/**, /api/products/*, /api/public/data
     */
    @Column(nullable = false, unique = true)
    private String pattern;

    /**
     * HTTP methods allowed (comma-separated, or * for all).
     * Examples: GET, GET,POST, *
     */
    @Column(nullable = false)
    private String httpMethods = "GET";

    /**
     * Description of what this pattern is for.
     */
    @Column
    private String description;

    /**
     * Whether this pattern is currently active.
     */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * User who created this pattern.
     */
    @Column
    private Long createdBy;

    /**
     * Creation timestamp.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp.
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public PublicApiPattern(String pattern, String httpMethods, String description) {
        this.pattern = pattern;
        this.httpMethods = httpMethods;
        this.description = description;
        this.enabled = true;
    }
}
