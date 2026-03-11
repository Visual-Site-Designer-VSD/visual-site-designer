package dev.mainul35.cms.sitebuilder.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entity representing a registered context provider from a plugin.
 * Stored in the cms_context_registry table.
 */
@Entity
@Table(name = "cms_context_registry")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContextRegistryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plugin_id", nullable = false, length = 100)
    private String pluginId;

    @Column(name = "context_id", nullable = false, length = 100)
    private String contextId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_descriptor", nullable = false, columnDefinition = "JSON")
    private String contextDescriptor; // JSON string of ContextDescriptor

    @Column(name = "provider_bundle_path", length = 500)
    private String providerBundlePath;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;
}
