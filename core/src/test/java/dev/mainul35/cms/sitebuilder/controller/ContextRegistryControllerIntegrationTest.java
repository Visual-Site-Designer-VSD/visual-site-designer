package dev.mainul35.cms.sitebuilder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mainul35.cms.sdk.context.ApiEndpoint;
import dev.mainul35.cms.sdk.context.ContextDescriptor;
import dev.mainul35.cms.sitebuilder.service.ContextRegistryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ContextRegistryController Integration Tests")
class ContextRegistryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContextRegistryService contextRegistryService;

    @Nested
    @DisplayName("GET /api/contexts")
    class GetActiveContexts {

        @Test
        @DisplayName("should return empty list when no contexts registered")
        void shouldReturnEmptyList() throws Exception {
            mockMvc.perform(get("/api/contexts").with(user("testuser")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("should return registered contexts")
        void shouldReturnRegisteredContexts() throws Exception {
            // Register a context
            ContextDescriptor descriptor = ContextDescriptor.builder()
                    .contextId("test-auth")
                    .pluginId("test-auth-plugin")
                    .pluginVersion("1.0.0")
                    .providerComponentPath("TestAuthProvider.js")
                    .apiEndpoints(List.of(
                            ApiEndpoint.builder()
                                    .path("/api/test-auth/login")
                                    .method("POST")
                                    .description("Test login")
                                    .build()
                    ))
                    .requiredContexts(List.of())
                    .build();

            contextRegistryService.registerContext(descriptor);

            try {
                mockMvc.perform(get("/api/contexts").with(user("testuser")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", hasSize(1)))
                        .andExpect(jsonPath("$[0].contextId", is("test-auth")))
                        .andExpect(jsonPath("$[0].pluginId", is("test-auth-plugin")))
                        .andExpect(jsonPath("$[0].providerComponentPath", is("TestAuthProvider.js")));
            } finally {
                // Clean up
                contextRegistryService.unregisterPluginContexts("test-auth-plugin");
            }
        }
    }

    @Nested
    @DisplayName("GET /api/contexts/{contextId}")
    class GetContext {

        @Test
        @DisplayName("should return 404 for nonexistent context")
        void shouldReturn404ForNonexistent() throws Exception {
            mockMvc.perform(get("/api/contexts/nonexistent").with(user("testuser")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return context descriptor when exists")
        void shouldReturnContextWhenExists() throws Exception {
            ContextDescriptor descriptor = ContextDescriptor.builder()
                    .contextId("test-cart")
                    .pluginId("test-cart-plugin")
                    .pluginVersion("1.0.0")
                    .providerComponentPath("TestCartProvider.js")
                    .apiEndpoints(List.of())
                    .requiredContexts(List.of())
                    .build();

            contextRegistryService.registerContext(descriptor);

            try {
                mockMvc.perform(get("/api/contexts/test-cart").with(user("testuser")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.contextId", is("test-cart")))
                        .andExpect(jsonPath("$.pluginId", is("test-cart-plugin")));
            } finally {
                contextRegistryService.unregisterPluginContexts("test-cart-plugin");
            }
        }
    }

    @Nested
    @DisplayName("POST /api/contexts/validate")
    class ValidateContexts {

        @Test
        @DisplayName("should return missing contexts")
        void shouldReturnMissingContexts() throws Exception {
            mockMvc.perform(post("/api/contexts/validate")
                            .with(user("admin").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of("auth", "cart"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$", containsInAnyOrder("auth", "cart")));
        }

        @Test
        @DisplayName("should return empty when all contexts available")
        void shouldReturnEmptyWhenAllAvailable() throws Exception {
            ContextDescriptor descriptor = ContextDescriptor.builder()
                    .contextId("test-available")
                    .pluginId("test-available-plugin")
                    .pluginVersion("1.0.0")
                    .providerComponentPath("Provider.js")
                    .apiEndpoints(List.of())
                    .requiredContexts(List.of())
                    .build();

            contextRegistryService.registerContext(descriptor);

            try {
                mockMvc.perform(post("/api/contexts/validate")
                                .with(user("admin").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(List.of("test-available"))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", hasSize(0)));
            } finally {
                contextRegistryService.unregisterPluginContexts("test-available-plugin");
            }
        }
    }
}
