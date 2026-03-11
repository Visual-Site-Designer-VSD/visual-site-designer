-- ========================================
-- Migration V11: Add context provider registry table
-- Supports the Context Provider Plugin architecture (ADR-008)
-- ========================================

CREATE TABLE IF NOT EXISTS cms_context_registry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plugin_id VARCHAR(100) NOT NULL,
    context_id VARCHAR(100) NOT NULL,
    context_descriptor JSON NOT NULL,
    provider_bundle_path VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_context_registry UNIQUE (plugin_id, context_id)
);

CREATE INDEX IF NOT EXISTS idx_context_registry_plugin ON cms_context_registry(plugin_id);
CREATE INDEX IF NOT EXISTS idx_context_registry_context ON cms_context_registry(context_id);
CREATE INDEX IF NOT EXISTS idx_context_registry_active ON cms_context_registry(is_active);
