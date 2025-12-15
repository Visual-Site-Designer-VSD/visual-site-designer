package dev.mainul35.cms.sdk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Base class that all plugin entities MUST extend.
 * Provides common fields and ensures consistent schema across all plugins.
 *
 * This enforces the design pattern where every plugin entity has:
 * - A plugin_id column for data isolation
 * - Standard timestamp fields for auditing
 */
@MappedSuperclass
@Data
public abstract class PluginEntity {

    /**
     * Plugin identifier - automatically populated by the plugin framework
     * This column is used for data isolation between plugins in shared tables
     */
    @Column(name = "plugin_id", nullable = false, length = 100)
    private String pluginId;

    /**
     * Timestamp when the record was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the record was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Default constructor
     */
    protected PluginEntity() {
        // Empty constructor for JPA
    }

    /**
     * Constructor with plugin ID
     * Use this in your plugin's entity constructors
     */
    protected PluginEntity(String pluginId) {
        this.pluginId = pluginId;
    }

    /**
     * Check if this entity belongs to a specific plugin
     */
    public boolean belongsToPlugin(String pluginId) {
        return this.pluginId != null && this.pluginId.equals(pluginId);
    }
}
