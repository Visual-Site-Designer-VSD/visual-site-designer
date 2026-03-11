package dev.mainul35.cms.sitebuilder.repository;

import dev.mainul35.cms.sitebuilder.entity.ContextRegistryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for context provider registry operations.
 */
@Repository
public interface ContextRegistryRepository extends JpaRepository<ContextRegistryEntry, Long> {

    Optional<ContextRegistryEntry> findByPluginIdAndContextId(String pluginId, String contextId);

    Optional<ContextRegistryEntry> findByContextId(String contextId);

    List<ContextRegistryEntry> findByPluginId(String pluginId);

    List<ContextRegistryEntry> findByIsActiveTrue();

    boolean existsByContextId(String contextId);

    void deleteByPluginId(String pluginId);

    void deleteByPluginIdAndContextId(String pluginId, String contextId);
}
