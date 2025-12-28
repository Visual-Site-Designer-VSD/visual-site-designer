package dev.mainul35.siteruntime.data;

import dev.mainul35.siteruntime.config.SiteRuntimeProperties.ApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Fetches data from REST APIs.
 * Uses gateway URL from properties - customer can point to their own API gateway.
 */
public class RestApiDataFetcher implements DataFetcher {

    private static final Logger log = LoggerFactory.getLogger(RestApiDataFetcher.class);

    private final RestTemplate restTemplate;
    private final ApiProperties apiProperties;

    public RestApiDataFetcher(ApiProperties apiProperties) {
        this.apiProperties = apiProperties;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean supports(DataSourceType type) {
        return type == DataSourceType.API;
    }

    @Override
    public Object fetch(DataSourceConfig config, Map<String, String> params) {
        // Resolve endpoint - could be relative (use gateway) or absolute
        String endpoint = resolveEndpoint(config.getEndpoint());

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
        if (params != null) {
            params.forEach(builder::queryParam);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (config.getHeaders() != null) {
            config.getHeaders().forEach(headers::add);
        }

        HttpEntity<String> entity = new HttpEntity<>(headers);
        HttpMethod method = "POST".equalsIgnoreCase(config.getMethod())
                ? HttpMethod.POST
                : HttpMethod.GET;

        log.debug("Fetching from API: {} {}", method, builder.toUriString());

        ResponseEntity<Object> response = restTemplate.exchange(
                builder.toUriString(),
                method,
                entity,
                Object.class
        );

        return response.getBody();
    }

    /**
     * Resolve endpoint - prepend gateway URL if relative path.
     */
    private String resolveEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalArgumentException("API endpoint is required");
        }

        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;  // Absolute URL
        }

        // Relative path - prepend gateway URL
        String gateway = apiProperties.getGatewayUrl();
        if (!gateway.endsWith("/") && !endpoint.startsWith("/")) {
            gateway += "/";
        }
        return gateway + endpoint;
    }
}
