package dev.mainul35.cms.sitebuilder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.cms.sdk.context.ApiEndpoint;
import dev.mainul35.cms.sdk.context.ContextDescriptor;
import dev.mainul35.cms.sitebuilder.entity.ContextRegistryEntry;
import dev.mainul35.cms.sitebuilder.service.ContextRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContextRegistryController")
class ContextRegistryControllerTest {

    @Mock
    private ContextRegistryService contextRegistryService;

    @InjectMocks
    private ContextRegistryController controller;

    private ContextDescriptor authDescriptor;
    private ContextDescriptor cartDescriptor;

    @BeforeEach
    void setUp() {
        authDescriptor = ContextDescriptor.builder()
                .contextId("auth")
                .pluginId("auth-context-plugin")
                .pluginVersion("1.0.0")
                .providerComponentPath("AuthProvider.js")
                .apiEndpoints(List.of(
                        ApiEndpoint.builder().path("/api/auth/login").method("POST").description("Login").build()
                ))
                .requiredContexts(List.of())
                .build();

        cartDescriptor = ContextDescriptor.builder()
                .contextId("cart")
                .pluginId("cart-context-plugin")
                .pluginVersion("1.0.0")
                .providerComponentPath("CartProvider.js")
                .apiEndpoints(List.of())
                .requiredContexts(List.of("auth"))
                .build();
    }

    @Nested
    @DisplayName("GET /api/contexts")
    class GetActiveContexts {

        @Test
        @DisplayName("should return sorted active contexts")
        void shouldReturnSortedActiveContexts() {
            List<ContextDescriptor> descriptors = List.of(cartDescriptor, authDescriptor);
            List<ContextDescriptor> sorted = List.of(authDescriptor, cartDescriptor);

            when(contextRegistryService.getActiveContextDescriptors()).thenReturn(descriptors);
            when(contextRegistryService.topologicalSort(descriptors)).thenReturn(sorted);

            ResponseEntity<List<ContextDescriptor>> response = controller.getActiveContexts();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).getContextId()).isEqualTo("auth");
            assertThat(response.getBody().get(1).getContextId()).isEqualTo("cart");
        }

        @Test
        @DisplayName("should return unsorted when circular dependency detected")
        void shouldReturnUnsortedOnCyclicDependency() {
            List<ContextDescriptor> descriptors = List.of(cartDescriptor, authDescriptor);

            when(contextRegistryService.getActiveContextDescriptors()).thenReturn(descriptors);
            when(contextRegistryService.topologicalSort(descriptors))
                    .thenThrow(new IllegalStateException("Circular dependency"));

            ResponseEntity<List<ContextDescriptor>> response = controller.getActiveContexts();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            // Returns unsorted (original order)
            assertThat(response.getBody().get(0).getContextId()).isEqualTo("cart");
        }

        @Test
        @DisplayName("should return empty list when no contexts")
        void shouldReturnEmptyList() {
            when(contextRegistryService.getActiveContextDescriptors()).thenReturn(List.of());
            when(contextRegistryService.topologicalSort(List.of())).thenReturn(List.of());

            ResponseEntity<List<ContextDescriptor>> response = controller.getActiveContexts();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/contexts/{contextId}")
    class GetContext {

        @Test
        @DisplayName("should return context when found")
        void shouldReturnContextWhenFound() {
            when(contextRegistryService.getContextDescriptor("auth"))
                    .thenReturn(Optional.of(authDescriptor));

            ResponseEntity<ContextDescriptor> response = controller.getContext("auth");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContextId()).isEqualTo("auth");
        }

        @Test
        @DisplayName("should return 404 when context not found")
        void shouldReturn404WhenNotFound() {
            when(contextRegistryService.getContextDescriptor("unknown"))
                    .thenReturn(Optional.empty());

            ResponseEntity<ContextDescriptor> response = controller.getContext("unknown");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/contexts/plugin/{pluginId}")
    class GetPluginContexts {

        @Test
        @DisplayName("should return contexts for a plugin")
        void shouldReturnPluginContexts() {
            ContextRegistryEntry entry = new ContextRegistryEntry();
            entry.setContextId("auth");
            entry.setPluginId("auth-context-plugin");

            when(contextRegistryService.getPluginContexts("auth-context-plugin"))
                    .thenReturn(List.of(entry));

            ResponseEntity<List<ContextRegistryEntry>> response =
                    controller.getPluginContexts("auth-context-plugin");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("POST /api/contexts/validate")
    class ValidateRequiredContexts {

        @Test
        @DisplayName("should return empty list when all available")
        void shouldReturnEmptyWhenAllAvailable() {
            when(contextRegistryService.validateRequiredContexts(List.of("auth")))
                    .thenReturn(List.of());

            ResponseEntity<List<String>> response =
                    controller.validateRequiredContexts(List.of("auth"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }

        @Test
        @DisplayName("should return missing context IDs")
        void shouldReturnMissingContextIds() {
            when(contextRegistryService.validateRequiredContexts(List.of("auth", "cart")))
                    .thenReturn(List.of("cart"));

            ResponseEntity<List<String>> response =
                    controller.validateRequiredContexts(List.of("auth", "cart"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsExactly("cart");
        }
    }
}
