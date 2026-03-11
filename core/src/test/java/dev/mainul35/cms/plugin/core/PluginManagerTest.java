package dev.mainul35.cms.plugin.core;

import dev.mainul35.cms.plugin.entity.Plugin;
import dev.mainul35.cms.plugin.repository.PluginRepository;
import dev.mainul35.cms.sitebuilder.service.ComponentRegistryService;
import dev.mainul35.cms.sitebuilder.service.ContextRegistryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PluginManager")
class PluginManagerTest {

    @Mock
    private PluginRepository pluginRepository;

    @Mock
    private ComponentRegistryService componentRegistryService;

    @Mock
    private ContextRegistryService contextRegistryService;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private PluginContextManager pluginContextManager;

    @Mock
    private PluginControllerRegistrar pluginControllerRegistrar;

    @Mock
    private PluginEntityRegistrar pluginEntityRegistrar;

    @InjectMocks
    private PluginManager pluginManager;

    @Nested
    @DisplayName("getAllPlugins")
    class GetAllPlugins {

        @Test
        @DisplayName("should return all plugins from repository")
        void shouldReturnAllPlugins() {
            Plugin p1 = new Plugin();
            p1.setPluginId("plugin-1");
            Plugin p2 = new Plugin();
            p2.setPluginId("plugin-2");

            when(pluginRepository.findAll()).thenReturn(List.of(p1, p2));

            List<Plugin> result = pluginManager.getAllPlugins();

            assertThat(result).hasSize(2);
            verify(pluginRepository).findAll();
        }
    }

    @Nested
    @DisplayName("getActivatedPlugins")
    class GetActivatedPlugins {

        @Test
        @DisplayName("should return only activated plugins")
        void shouldReturnActivatedPlugins() {
            Plugin active = new Plugin();
            active.setPluginId("active-plugin");
            active.activate();

            when(pluginRepository.findAllActivated()).thenReturn(List.of(active));

            List<Plugin> result = pluginManager.getActivatedPlugins();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPluginId()).isEqualTo("active-plugin");
        }
    }

    @Nested
    @DisplayName("getPlugin")
    class GetPlugin {

        @Test
        @DisplayName("should return plugin when found")
        void shouldReturnPluginWhenFound() {
            Plugin plugin = new Plugin();
            plugin.setPluginId("test-plugin");

            when(pluginRepository.findByPluginId("test-plugin")).thenReturn(Optional.of(plugin));

            Optional<Plugin> result = pluginManager.getPlugin("test-plugin");

            assertThat(result).isPresent();
            assertThat(result.get().getPluginId()).isEqualTo("test-plugin");
        }

        @Test
        @DisplayName("should return empty when plugin not found")
        void shouldReturnEmptyWhenNotFound() {
            when(pluginRepository.findByPluginId("unknown")).thenReturn(Optional.empty());

            Optional<Plugin> result = pluginManager.getPlugin("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("isPluginLoaded")
    class IsPluginLoaded {

        @Test
        @DisplayName("should return false for unloaded plugin")
        void shouldReturnFalseForUnloaded() {
            assertThat(pluginManager.isPluginLoaded("not-loaded")).isFalse();
        }
    }

    @Nested
    @DisplayName("getLoadedPlugin")
    class GetLoadedPlugin {

        @Test
        @DisplayName("should return null for unloaded plugin")
        void shouldReturnNullForUnloaded() {
            assertThat(pluginManager.getLoadedPlugin("not-loaded")).isNull();
        }
    }

    @Nested
    @DisplayName("uninstallPlugin")
    class UninstallPlugin {

        @Test
        @DisplayName("should throw when plugin not found")
        void shouldThrowWhenPluginNotFound() {
            when(pluginRepository.findByPluginId("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pluginManager.uninstallPlugin("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plugin not found");
        }
    }

    @Nested
    @DisplayName("activatePlugin")
    class ActivatePlugin {

        @Test
        @DisplayName("should throw when plugin not found")
        void shouldThrowWhenPluginNotFound() {
            when(pluginRepository.findByPluginId("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pluginManager.activatePlugin("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plugin not found");
        }

        @Test
        @DisplayName("should skip activation when already active")
        void shouldSkipWhenAlreadyActive() throws Exception {
            Plugin plugin = new Plugin();
            plugin.setPluginId("active-plugin");
            plugin.activate();

            when(pluginRepository.findByPluginId("active-plugin")).thenReturn(Optional.of(plugin));

            pluginManager.activatePlugin("active-plugin");

            // Should return early without saving
            verify(pluginRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deactivatePlugin")
    class DeactivatePlugin {

        @Test
        @DisplayName("should throw when plugin not found")
        void shouldThrowWhenPluginNotFound() {
            when(pluginRepository.findByPluginId("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pluginManager.deactivatePlugin("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plugin not found");
        }

        @Test
        @DisplayName("should skip deactivation when not active")
        void shouldSkipWhenNotActive() throws Exception {
            Plugin plugin = new Plugin();
            plugin.setPluginId("inactive-plugin");
            plugin.setStatus("installed");

            when(pluginRepository.findByPluginId("inactive-plugin")).thenReturn(Optional.of(plugin));

            pluginManager.deactivatePlugin("inactive-plugin");

            verify(pluginRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("installPlugin")
    class InstallPlugin {

        @Test
        @DisplayName("should throw when JAR file does not exist")
        void shouldThrowWhenJarNotFound() {
            java.io.File nonexistent = new java.io.File("/nonexistent/plugin.jar");

            assertThatThrownBy(() -> pluginManager.installPlugin(nonexistent))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("JAR file does not exist");
        }

        @Test
        @DisplayName("should throw when file is not a JAR")
        void shouldThrowWhenNotJar() throws Exception {
            java.io.File tempFile = java.io.File.createTempFile("not-a-plugin", ".txt");
            tempFile.deleteOnExit();

            assertThatThrownBy(() -> pluginManager.installPlugin(tempFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a JAR file");
        }
    }
}
