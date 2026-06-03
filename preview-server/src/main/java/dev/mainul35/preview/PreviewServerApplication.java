package dev.mainul35.preview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Preview Server Application.
 * Launches the site-runtime as a standalone Spring Boot server for live preview.
 *
 * Accepts CLI args:
 * --server.port=3001
 * --site.runtime.data-dir=/path/to/snapshot
 * --site.runtime.api.gateway-url=http://localhost:8080
 * --spring.config.additional-location=file:/path/to/application.properties
 */
@SpringBootApplication
public class PreviewServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PreviewServerApplication.class, args);
    }
}
