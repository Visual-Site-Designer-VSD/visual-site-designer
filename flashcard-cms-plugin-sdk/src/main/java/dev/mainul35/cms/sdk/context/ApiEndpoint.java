package dev.mainul35.cms.sdk.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Describes a REST API endpoint exposed by a context provider plugin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpoint {

    /**
     * Endpoint path (e.g., "/api/ctx/auth/session")
     */
    private String path;

    /**
     * HTTP method (e.g., "GET", "POST", "PUT", "DELETE")
     */
    private String method;

    /**
     * Human-readable description of what this endpoint does
     */
    private String description;
}
