package dev.mainul35.cms.sitebuilder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.cms.sdk.component.ComponentManifest;
import dev.mainul35.cms.sitebuilder.entity.ComponentRegistryEntry;
import dev.mainul35.cms.sitebuilder.repository.ComponentRegistryRepository;
import dev.mainul35.cms.sitebuilder.repository.PageVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComponentRegistryService")
class ComponentRegistryServiceTest {

    @Mock
    private ComponentRegistryRepository componentRegistryRepository;

    @Mock
    private PageVersionRepository pageVersionRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ComponentRegistryService componentRegistryService;

    private ComponentManifest labelManifest;
    private ComponentManifest buttonManifest;

    @BeforeEach
    void setUp() {
        labelManifest = ComponentManifest.builder()
                .pluginId("label-component-plugin")
                .componentId("label")
                .displayName("Label")
                .category("ui")
                .icon("L")
                .reactComponentPath("label.js")
                .build();

        buttonManifest = ComponentManifest.builder()
                .pluginId("button-component-plugin")
                .componentId("button")
                .displayName("Button")
                .category("ui")
                .icon("B")
                .reactComponentPath("button.js")
                .build();
    }

    @Nested
    @DisplayName("registerComponent")
    class RegisterComponent {

        @Test
        @DisplayName("should register a new component")
        void shouldRegisterNewComponent() {
            when(componentRegistryRepository.findByPluginIdAndComponentId("label-component-plugin", "label"))
                    .thenReturn(Optional.empty());
            when(componentRegistryRepository.save(any(ComponentRegistryEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ComponentRegistryEntry result = componentRegistryService.registerComponent(labelManifest);

            assertThat(result).isNotNull();
            assertThat(result.getPluginId()).isEqualTo("label-component-plugin");
            assertThat(result.getComponentId()).isEqualTo("label");
            assertThat(result.getComponentName()).isEqualTo("Label");
            assertThat(result.getCategory()).isEqualTo("ui");
            assertThat(result.getIsActive()).isTrue();
            assertThat(result.getComponentManifest()).contains("\"componentId\":\"label\"");
        }

        @Test
        @DisplayName("should update existing component")
        void shouldUpdateExistingComponent() {
            ComponentRegistryEntry existing = new ComponentRegistryEntry();
            existing.setPluginId("label-component-plugin");
            existing.setComponentId("label");
            existing.setComponentName("Old Label");

            when(componentRegistryRepository.findByPluginIdAndComponentId("label-component-plugin", "label"))
                    .thenReturn(Optional.of(existing));
            when(componentRegistryRepository.save(any(ComponentRegistryEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ComponentRegistryEntry result = componentRegistryService.registerComponent(labelManifest);

            assertThat(result.getComponentName()).isEqualTo("Label");
            verify(componentRegistryRepository).save(existing);
        }
    }

    @Nested
    @DisplayName("getAllComponents")
    class GetAllComponents {

        @Test
        @DisplayName("should return only active components")
        void shouldReturnActiveComponents() {
            ComponentRegistryEntry entry = new ComponentRegistryEntry();
            entry.setComponentId("label");
            entry.setIsActive(true);

            when(componentRegistryRepository.findByIsActiveTrue()).thenReturn(List.of(entry));

            List<ComponentRegistryEntry> result = componentRegistryService.getAllComponents();

            assertThat(result).hasSize(1);
            verify(componentRegistryRepository).findByIsActiveTrue();
        }
    }

    @Nested
    @DisplayName("getComponentsByCategory")
    class GetComponentsByCategory {

        @Test
        @DisplayName("should return components in the given category")
        void shouldReturnComponentsByCategory() {
            ComponentRegistryEntry entry = new ComponentRegistryEntry();
            entry.setCategory("ui");

            when(componentRegistryRepository.findByCategoryAndIsActiveTrue("ui")).thenReturn(List.of(entry));

            List<ComponentRegistryEntry> result = componentRegistryService.getComponentsByCategory("ui");

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getComponentManifest")
    class GetComponentManifest {

        @Test
        @DisplayName("should deserialize manifest from entry")
        void shouldDeserializeManifest() throws JsonProcessingException {
            ComponentRegistryEntry entry = new ComponentRegistryEntry();
            entry.setPluginId("label-component-plugin");
            entry.setComponentId("label");
            entry.setComponentManifest(objectMapper.writeValueAsString(labelManifest));

            when(componentRegistryRepository.findByPluginIdAndComponentId("label-component-plugin", "label"))
                    .thenReturn(Optional.of(entry));

            Optional<ComponentManifest> result = componentRegistryService.getComponentManifest(
                    "label-component-plugin", "label");

            assertThat(result).isPresent();
            assertThat(result.get().getComponentId()).isEqualTo("label");
            assertThat(result.get().getDisplayName()).isEqualTo("Label");
        }

        @Test
        @DisplayName("should return empty when component not found")
        void shouldReturnEmptyWhenNotFound() {
            when(componentRegistryRepository.findByPluginIdAndComponentId("unknown", "unknown"))
                    .thenReturn(Optional.empty());

            Optional<ComponentManifest> result = componentRegistryService.getComponentManifest("unknown", "unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deactivateComponent / activateComponent")
    class ActivateDeactivate {

        @Test
        @DisplayName("should deactivate an existing component")
        void shouldDeactivateComponent() {
            ComponentRegistryEntry entry = new ComponentRegistryEntry();
            entry.setIsActive(true);

            when(componentRegistryRepository.findByPluginIdAndComponentId("label-component-plugin", "label"))
                    .thenReturn(Optional.of(entry));
            when(componentRegistryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            componentRegistryService.deactivateComponent("label-component-plugin", "label");

            ArgumentCaptor<ComponentRegistryEntry> captor = ArgumentCaptor.forClass(ComponentRegistryEntry.class);
            verify(componentRegistryRepository).save(captor.capture());
            assertThat(captor.getValue().getIsActive()).isFalse();
        }

        @Test
        @DisplayName("should activate a deactivated component")
        void shouldActivateComponent() {
            ComponentRegistryEntry entry = new ComponentRegistryEntry();
            entry.setIsActive(false);

            when(componentRegistryRepository.findByPluginIdAndComponentId("label-component-plugin", "label"))
                    .thenReturn(Optional.of(entry));
            when(componentRegistryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            componentRegistryService.activateComponent("label-component-plugin", "label");

            ArgumentCaptor<ComponentRegistryEntry> captor = ArgumentCaptor.forClass(ComponentRegistryEntry.class);
            verify(componentRegistryRepository).save(captor.capture());
            assertThat(captor.getValue().getIsActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("registerComponents (batch)")
    class RegisterComponents {

        @Test
        @DisplayName("should register multiple components")
        void shouldRegisterMultipleComponents() {
            when(componentRegistryRepository.findByPluginIdAndComponentId(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(componentRegistryRepository.save(any(ComponentRegistryEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            List<ComponentRegistryEntry> result = componentRegistryService.registerComponents(
                    List.of(labelManifest, buttonManifest));

            assertThat(result).hasSize(2);
            verify(componentRegistryRepository, times(2)).save(any());
        }
    }

    @Nested
    @DisplayName("unregisterPluginComponents")
    class UnregisterPluginComponents {

        @Test
        @DisplayName("should delete all components for a plugin")
        void shouldDeleteAllComponentsForPlugin() {
            componentRegistryService.unregisterPluginComponents("label-component-plugin");

            verify(componentRegistryRepository).deleteByPluginId("label-component-plugin");
        }
    }

    @Nested
    @DisplayName("isComponentRegistered")
    class IsComponentRegistered {

        @Test
        @DisplayName("should return true when component exists")
        void shouldReturnTrueWhenExists() {
            when(componentRegistryRepository.existsByPluginIdAndComponentId("label-component-plugin", "label"))
                    .thenReturn(true);

            assertThat(componentRegistryService.isComponentRegistered("label-component-plugin", "label")).isTrue();
        }

        @Test
        @DisplayName("should return false when component does not exist")
        void shouldReturnFalseWhenNotExists() {
            when(componentRegistryRepository.existsByPluginIdAndComponentId("unknown", "unknown"))
                    .thenReturn(false);

            assertThat(componentRegistryService.isComponentRegistered("unknown", "unknown")).isFalse();
        }
    }

    @Nested
    @DisplayName("getCategories")
    class GetCategories {

        @Test
        @DisplayName("should return all known categories")
        void shouldReturnCategories() {
            List<String> categories = componentRegistryService.getCategories();

            assertThat(categories).containsExactly("ui", "layout", "form", "widget", "navbar", "data");
        }
    }
}
