package dev.mainul35.cms.sitebuilder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.cms.sdk.context.ApiEndpoint;
import dev.mainul35.cms.sdk.context.ContextDescriptor;
import dev.mainul35.cms.sitebuilder.entity.ContextRegistryEntry;
import dev.mainul35.cms.sitebuilder.repository.ContextRegistryRepository;
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
@DisplayName("ContextRegistryService")
class ContextRegistryServiceTest {

    @Mock
    private ContextRegistryRepository contextRegistryRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ContextRegistryService contextRegistryService;

    private ContextDescriptor authDescriptor;
    private ContextDescriptor cartDescriptor;
    private ContextDescriptor orderDescriptor;

    @BeforeEach
    void setUp() {
        authDescriptor = ContextDescriptor.builder()
                .contextId("auth")
                .pluginId("auth-context-plugin")
                .pluginVersion("1.0.0")
                .providerComponentPath("AuthProvider.js")
                .apiEndpoints(List.of(
                        ApiEndpoint.builder().path("/api/auth/login").method("POST").description("Login").build(),
                        ApiEndpoint.builder().path("/api/auth/logout").method("POST").description("Logout").build()
                ))
                .requiredContexts(List.of())
                .build();

        cartDescriptor = ContextDescriptor.builder()
                .contextId("cart")
                .pluginId("cart-context-plugin")
                .pluginVersion("1.0.0")
                .providerComponentPath("CartProvider.js")
                .apiEndpoints(List.of(
                        ApiEndpoint.builder().path("/api/cart").method("GET").description("Get cart").build()
                ))
                .requiredContexts(List.of("auth"))
                .build();

        orderDescriptor = ContextDescriptor.builder()
                .contextId("order")
                .pluginId("order-context-plugin")
                .pluginVersion("1.0.0")
                .providerComponentPath("OrderProvider.js")
                .apiEndpoints(List.of())
                .requiredContexts(List.of("auth", "cart"))
                .build();
    }

    @Nested
    @DisplayName("registerContext")
    class RegisterContext {

        @Test
        @DisplayName("should register a new context")
        void shouldRegisterNewContext() throws JsonProcessingException {
            when(contextRegistryRepository.findByPluginIdAndContextId("auth-context-plugin", "auth"))
                    .thenReturn(Optional.empty());
            when(contextRegistryRepository.save(any(ContextRegistryEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ContextRegistryEntry result = contextRegistryService.registerContext(authDescriptor);

            assertThat(result).isNotNull();
            assertThat(result.getPluginId()).isEqualTo("auth-context-plugin");
            assertThat(result.getContextId()).isEqualTo("auth");
            assertThat(result.getProviderBundlePath()).isEqualTo("AuthProvider.js");
            assertThat(result.getIsActive()).isTrue();
            assertThat(result.getContextDescriptor()).contains("\"contextId\":\"auth\"");

            verify(contextRegistryRepository).save(any(ContextRegistryEntry.class));
        }

        @Test
        @DisplayName("should update existing context")
        void shouldUpdateExistingContext() {
            ContextRegistryEntry existingEntry = new ContextRegistryEntry();
            existingEntry.setPluginId("auth-context-plugin");
            existingEntry.setContextId("auth");
            existingEntry.setProviderBundlePath("OldProvider.js");

            when(contextRegistryRepository.findByPluginIdAndContextId("auth-context-plugin", "auth"))
                    .thenReturn(Optional.of(existingEntry));
            when(contextRegistryRepository.save(any(ContextRegistryEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ContextRegistryEntry result = contextRegistryService.registerContext(authDescriptor);

            assertThat(result.getProviderBundlePath()).isEqualTo("AuthProvider.js");
            assertThat(result.getIsActive()).isTrue();
            verify(contextRegistryRepository).save(existingEntry);
        }
    }

    @Nested
    @DisplayName("getActiveContexts")
    class GetActiveContexts {

        @Test
        @DisplayName("should return active context entries")
        void shouldReturnActiveContextEntries() {
            List<ContextRegistryEntry> entries = List.of(new ContextRegistryEntry());
            when(contextRegistryRepository.findByIsActiveTrue()).thenReturn(entries);

            List<ContextRegistryEntry> result = contextRegistryService.getActiveContexts();

            assertThat(result).hasSize(1);
            verify(contextRegistryRepository).findByIsActiveTrue();
        }

        @Test
        @DisplayName("should return empty list when no active contexts")
        void shouldReturnEmptyListWhenNoActiveContexts() {
            when(contextRegistryRepository.findByIsActiveTrue()).thenReturn(List.of());

            List<ContextRegistryEntry> result = contextRegistryService.getActiveContexts();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getActiveContextDescriptors")
    class GetActiveContextDescriptors {

        @Test
        @DisplayName("should deserialize active context entries to descriptors")
        void shouldDeserializeToDescriptors() throws JsonProcessingException {
            ContextRegistryEntry entry = new ContextRegistryEntry();
            entry.setContextId("auth");
            entry.setContextDescriptor(objectMapper.writeValueAsString(authDescriptor));
            entry.setIsActive(true);

            when(contextRegistryRepository.findByIsActiveTrue()).thenReturn(List.of(entry));

            List<ContextDescriptor> result = contextRegistryService.getActiveContextDescriptors();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContextId()).isEqualTo("auth");
            assertThat(result.get(0).getPluginId()).isEqualTo("auth-context-plugin");
        }

        @Test
        @DisplayName("should skip entries with invalid JSON")
        void shouldSkipInvalidJson() {
            ContextRegistryEntry validEntry = new ContextRegistryEntry();
            validEntry.setContextId("auth");
            try {
                validEntry.setContextDescriptor(objectMapper.writeValueAsString(authDescriptor));
            } catch (JsonProcessingException e) {
                fail("Setup failed");
            }
            validEntry.setIsActive(true);

            ContextRegistryEntry invalidEntry = new ContextRegistryEntry();
            invalidEntry.setContextId("broken");
            invalidEntry.setContextDescriptor("{invalid json}");
            invalidEntry.setIsActive(true);

            when(contextRegistryRepository.findByIsActiveTrue()).thenReturn(List.of(validEntry, invalidEntry));

            List<ContextDescriptor> result = contextRegistryService.getActiveContextDescriptors();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContextId()).isEqualTo("auth");
        }
    }

    @Nested
    @DisplayName("deactivateContext")
    class DeactivateContext {

        @Test
        @DisplayName("should deactivate an existing context")
        void shouldDeactivateExistingContext() {
            ContextRegistryEntry entry = new ContextRegistryEntry();
            entry.setIsActive(true);

            when(contextRegistryRepository.findByPluginIdAndContextId("auth-context-plugin", "auth"))
                    .thenReturn(Optional.of(entry));
            when(contextRegistryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            contextRegistryService.deactivateContext("auth-context-plugin", "auth");

            ArgumentCaptor<ContextRegistryEntry> captor = ArgumentCaptor.forClass(ContextRegistryEntry.class);
            verify(contextRegistryRepository).save(captor.capture());
            assertThat(captor.getValue().getIsActive()).isFalse();
        }

        @Test
        @DisplayName("should do nothing when context not found")
        void shouldDoNothingWhenContextNotFound() {
            when(contextRegistryRepository.findByPluginIdAndContextId("unknown", "unknown"))
                    .thenReturn(Optional.empty());

            contextRegistryService.deactivateContext("unknown", "unknown");

            verify(contextRegistryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("unregisterPluginContexts")
    class UnregisterPluginContexts {

        @Test
        @DisplayName("should delete all contexts for a plugin")
        void shouldDeleteAllContextsForPlugin() {
            contextRegistryService.unregisterPluginContexts("auth-context-plugin");

            verify(contextRegistryRepository).deleteByPluginId("auth-context-plugin");
        }
    }

    @Nested
    @DisplayName("validateRequiredContexts")
    class ValidateRequiredContexts {

        @Test
        @DisplayName("should return empty list when all contexts are available")
        void shouldReturnEmptyWhenAllAvailable() {
            when(contextRegistryRepository.existsByContextId("auth")).thenReturn(true);
            when(contextRegistryRepository.existsByContextId("cart")).thenReturn(true);

            List<String> missing = contextRegistryService.validateRequiredContexts(List.of("auth", "cart"));

            assertThat(missing).isEmpty();
        }

        @Test
        @DisplayName("should return missing context IDs")
        void shouldReturnMissingContextIds() {
            when(contextRegistryRepository.existsByContextId("auth")).thenReturn(true);
            when(contextRegistryRepository.existsByContextId("cart")).thenReturn(false);

            List<String> missing = contextRegistryService.validateRequiredContexts(List.of("auth", "cart"));

            assertThat(missing).containsExactly("cart");
        }

        @Test
        @DisplayName("should return empty list for null input")
        void shouldReturnEmptyForNullInput() {
            List<String> missing = contextRegistryService.validateRequiredContexts(null);

            assertThat(missing).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for empty input")
        void shouldReturnEmptyForEmptyInput() {
            List<String> missing = contextRegistryService.validateRequiredContexts(List.of());

            assertThat(missing).isEmpty();
        }
    }

    @Nested
    @DisplayName("topologicalSort")
    class TopologicalSort {

        @Test
        @DisplayName("should sort independent contexts in original order")
        void shouldSortIndependentContexts() {
            ContextDescriptor a = ContextDescriptor.builder()
                    .contextId("a").requiredContexts(List.of()).build();
            ContextDescriptor b = ContextDescriptor.builder()
                    .contextId("b").requiredContexts(List.of()).build();

            List<ContextDescriptor> sorted = contextRegistryService.topologicalSort(List.of(a, b));

            assertThat(sorted).hasSize(2);
            assertThat(sorted.get(0).getContextId()).isEqualTo("a");
            assertThat(sorted.get(1).getContextId()).isEqualTo("b");
        }

        @Test
        @DisplayName("should place dependency before dependent")
        void shouldPlaceDependencyFirst() {
            // cart depends on auth → auth should come first
            List<ContextDescriptor> sorted = contextRegistryService.topologicalSort(
                    List.of(cartDescriptor, authDescriptor));

            assertThat(sorted).hasSize(2);
            assertThat(sorted.get(0).getContextId()).isEqualTo("auth");
            assertThat(sorted.get(1).getContextId()).isEqualTo("cart");
        }

        @Test
        @DisplayName("should handle transitive dependencies")
        void shouldHandleTransitiveDependencies() {
            // order depends on auth + cart, cart depends on auth
            // Expected order: auth → cart → order
            List<ContextDescriptor> sorted = contextRegistryService.topologicalSort(
                    List.of(orderDescriptor, cartDescriptor, authDescriptor));

            assertThat(sorted).hasSize(3);
            assertThat(sorted.get(0).getContextId()).isEqualTo("auth");
            assertThat(sorted.get(1).getContextId()).isEqualTo("cart");
            assertThat(sorted.get(2).getContextId()).isEqualTo("order");
        }

        @Test
        @DisplayName("should detect circular dependency")
        void shouldDetectCircularDependency() {
            ContextDescriptor a = ContextDescriptor.builder()
                    .contextId("a").requiredContexts(List.of("b")).build();
            ContextDescriptor b = ContextDescriptor.builder()
                    .contextId("b").requiredContexts(List.of("a")).build();

            assertThatThrownBy(() -> contextRegistryService.topologicalSort(List.of(a, b)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Circular context dependency");
        }

        @Test
        @DisplayName("should detect self-referencing dependency")
        void shouldDetectSelfReferencingDependency() {
            ContextDescriptor self = ContextDescriptor.builder()
                    .contextId("self").requiredContexts(List.of("self")).build();

            assertThatThrownBy(() -> contextRegistryService.topologicalSort(List.of(self)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Circular context dependency");
        }

        @Test
        @DisplayName("should handle empty list")
        void shouldHandleEmptyList() {
            List<ContextDescriptor> sorted = contextRegistryService.topologicalSort(List.of());

            assertThat(sorted).isEmpty();
        }

        @Test
        @DisplayName("should handle single context with no dependencies")
        void shouldHandleSingleContext() {
            List<ContextDescriptor> sorted = contextRegistryService.topologicalSort(List.of(authDescriptor));

            assertThat(sorted).hasSize(1);
            assertThat(sorted.get(0).getContextId()).isEqualTo("auth");
        }

        @Test
        @DisplayName("should handle dependency on unknown context gracefully")
        void shouldHandleUnknownDependency() {
            ContextDescriptor withUnknownDep = ContextDescriptor.builder()
                    .contextId("widget").requiredContexts(List.of("nonexistent")).build();

            // Should not throw — unknown deps are simply skipped in the visit
            List<ContextDescriptor> sorted = contextRegistryService.topologicalSort(List.of(withUnknownDep));

            assertThat(sorted).hasSize(1);
            assertThat(sorted.get(0).getContextId()).isEqualTo("widget");
        }

        @Test
        @DisplayName("should handle three-node circular dependency")
        void shouldDetectThreeNodeCycle() {
            ContextDescriptor a = ContextDescriptor.builder()
                    .contextId("a").requiredContexts(List.of("c")).build();
            ContextDescriptor b = ContextDescriptor.builder()
                    .contextId("b").requiredContexts(List.of("a")).build();
            ContextDescriptor c = ContextDescriptor.builder()
                    .contextId("c").requiredContexts(List.of("b")).build();

            assertThatThrownBy(() -> contextRegistryService.topologicalSort(List.of(a, b, c)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Circular context dependency");
        }

        @Test
        @DisplayName("should handle diamond dependency graph")
        void shouldHandleDiamondDependency() {
            // Diamond: D depends on B and C, both B and C depend on A
            ContextDescriptor a = ContextDescriptor.builder()
                    .contextId("a").requiredContexts(List.of()).build();
            ContextDescriptor b = ContextDescriptor.builder()
                    .contextId("b").requiredContexts(List.of("a")).build();
            ContextDescriptor c = ContextDescriptor.builder()
                    .contextId("c").requiredContexts(List.of("a")).build();
            ContextDescriptor d = ContextDescriptor.builder()
                    .contextId("d").requiredContexts(List.of("b", "c")).build();

            List<ContextDescriptor> sorted = contextRegistryService.topologicalSort(List.of(d, c, b, a));

            assertThat(sorted).hasSize(4);
            // A must be first (no deps)
            assertThat(sorted.get(0).getContextId()).isEqualTo("a");
            // D must be last (depends on B and C)
            assertThat(sorted.get(3).getContextId()).isEqualTo("d");
            // B and C can be in either order but both must come before D
            List<String> middleIds = List.of(sorted.get(1).getContextId(), sorted.get(2).getContextId());
            assertThat(middleIds).containsExactlyInAnyOrder("b", "c");
        }
    }
}
