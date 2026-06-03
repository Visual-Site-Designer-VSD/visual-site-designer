package dev.mainul35.cms.config;

import lombok.RequiredArgsConstructor;
import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /**
     * Enable ForwardedHeaderFilter to handle X-Forwarded-* headers from reverse proxy.
     * This ensures HTTPS redirect URIs are built correctly when behind Cloudflare/nginx.
     * Must run before Spring Security filters.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    private final CorsProperties corsProperties;

    /**
     * Configure CORS dynamically based on application.properties.
     * This allows different origins for dev, staging, and production environments.
     *
     * Configure via:
     * - application.properties: cors.allowed-origins=https://myapp.com,https://admin.myapp.com
     * - Environment variable: CORS_ALLOWED_ORIGINS=https://myapp.com,https://admin.myapp.com
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsProperties.getAllowedOrigins())
                .allowedMethods(corsProperties.getAllowedMethods())
                .allowedHeaders("*")
                .allowCredentials(corsProperties.isAllowCredentials());
    }

    /**
     * Configure resource handlers for React SPA
     * This is the KEY configuration for making React Router work
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Uploaded media files
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(3600);

        // React SPA static resources
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // Don't swallow paths owned by other servlets (H2 console, actuator).
                        if (resourcePath.startsWith("h2-console") || resourcePath.startsWith("actuator")) {
                            return null;
                        }

                        Resource requestedResource = location.createRelative(resourcePath);

                        // If the resource exists (JS, CSS, images, etc.), serve it
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // Otherwise, serve index.html (for React Router routes)
                        // API calls won't reach here because they're handled by controllers
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }

    /**
     * Spring Boot 4 removed H2ConsoleAutoConfiguration, so the H2 console servlet
     * has to be registered explicitly. Toggle with spring.h2.console.enabled.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServletRegistration() {
        ServletRegistrationBean<JakartaWebServlet> registration =
                new ServletRegistrationBean<>(new JakartaWebServlet(), "/h2-console/*");
        registration.setName("h2-console");
        registration.setLoadOnStartup(1);
        return registration;
    }
}
