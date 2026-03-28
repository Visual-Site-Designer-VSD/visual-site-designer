package dev.mainul35.siteruntime.controller;

import dev.mainul35.siteruntime.config.SiteRuntimeProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;

/**
 * API Proxy Controller for forwarding requests from the SPA frontend to external APIs.
 *
 * The OAuth2 access token lives in the server-side session. This controller:
 * - Forwards GET requests to the gateway URL (public reads, no auth token required)
 * - Forwards POST/PUT/DELETE requests with the OAuth2 access token attached
 *
 * All requests to /api/proxy/** are forwarded to the configured gateway URL.
 * Example: GET /api/proxy/articles -> GET {gateway-url}/articles
 */
@RestController
@RequestMapping("/api/proxy")
public class ApiProxyController {

    private static final Logger log = LoggerFactory.getLogger(ApiProxyController.class);
    private static final String PROXY_PATH_PREFIX = "/api/proxy";

    private final SiteRuntimeProperties properties;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestTemplate restTemplate;

    public ApiProxyController(
            SiteRuntimeProperties properties,
            OAuth2AuthorizedClientService authorizedClientService) {
        this.properties = properties;
        this.authorizedClientService = authorizedClientService;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Forward GET requests to the gateway (public reads).
     * No OAuth2 token is attached for GET requests.
     */
    @GetMapping("/**")
    public ResponseEntity<String> proxyGet(HttpServletRequest request) {
        String targetUrl = buildTargetUrl(request);
        log.debug("Proxying GET {} -> {}", request.getRequestURI(), targetUrl);

        HttpHeaders headers = buildHeaders(request, false);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl, HttpMethod.GET, entity, String.class);
            return buildProxyResponse(response);
        } catch (Exception e) {
            log.error("Proxy GET failed for {}: {}", targetUrl, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Failed to fetch from upstream\",\"message\":\"" +
                            escapeJson(e.getMessage()) + "\"}");
        }
    }

    /**
     * Forward POST requests with OAuth2 token (authenticated writes).
     */
    @PostMapping("/**")
    public ResponseEntity<String> proxyPost(HttpServletRequest request) throws IOException {
        return proxyMutatingRequest(request, HttpMethod.POST);
    }

    /**
     * Forward PUT requests with OAuth2 token (authenticated updates).
     */
    @PutMapping("/**")
    public ResponseEntity<String> proxyPut(HttpServletRequest request) throws IOException {
        return proxyMutatingRequest(request, HttpMethod.PUT);
    }

    /**
     * Forward DELETE requests with OAuth2 token (authenticated deletes).
     */
    @DeleteMapping("/**")
    public ResponseEntity<String> proxyDelete(HttpServletRequest request) throws IOException {
        return proxyMutatingRequest(request, HttpMethod.DELETE);
    }

    private ResponseEntity<String> proxyMutatingRequest(HttpServletRequest request, HttpMethod method) throws IOException {
        String targetUrl = buildTargetUrl(request);
        log.debug("Proxying {} {} -> {}", method, request.getRequestURI(), targetUrl);

        HttpHeaders headers = buildHeaders(request, true);

        // Read request body
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl, method, entity, String.class);
            return buildProxyResponse(response);
        } catch (Exception e) {
            log.error("Proxy {} failed for {}: {}", method, targetUrl, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Failed to send to upstream\",\"message\":\"" +
                            escapeJson(e.getMessage()) + "\"}");
        }
    }

    /**
     * Build the target URL by replacing /api/proxy prefix with the gateway URL.
     */
    private String buildTargetUrl(HttpServletRequest request) {
        String gatewayUrl = properties.getApi().getGatewayUrl();
        String path = request.getRequestURI().substring(PROXY_PATH_PREFIX.length());
        String queryString = request.getQueryString();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(gatewayUrl)
                .path(path);

        if (queryString != null && !queryString.isEmpty()) {
            builder.query(queryString);
        }

        return builder.build().toUriString();
    }

    /**
     * Build headers for the proxied request.
     * For mutating requests, attaches the OAuth2 access token.
     */
    private HttpHeaders buildHeaders(HttpServletRequest request, boolean includeAuthToken) {
        HttpHeaders headers = new HttpHeaders();

        // Forward content-type
        String contentType = request.getContentType();
        if (contentType != null) {
            headers.set(HttpHeaders.CONTENT_TYPE, contentType);
        }

        // Forward accept header
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept != null) {
            headers.set(HttpHeaders.ACCEPT, accept);
        }

        // Attach OAuth2 access token for authenticated requests
        if (includeAuthToken) {
            String accessToken = getOAuth2AccessToken();
            if (accessToken != null) {
                headers.setBearerAuth(accessToken);
            } else {
                log.warn("No OAuth2 access token available for authenticated proxy request");
            }
        }

        return headers;
    }

    /**
     * Retrieve the OAuth2 access token from the current session.
     */
    private String getOAuth2AccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();
            String principalName = oauthToken.getName();

            if (authorizedClientService != null) {
                OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                        registrationId, principalName);
                if (client != null && client.getAccessToken() != null) {
                    return client.getAccessToken().getTokenValue();
                }
            }
        }
        return null;
    }

    /**
     * Build proxy response, forwarding status and content type.
     */
    private ResponseEntity<String> buildProxyResponse(ResponseEntity<String> upstreamResponse) {
        HttpHeaders responseHeaders = new HttpHeaders();
        MediaType contentType = upstreamResponse.getHeaders().getContentType();
        if (contentType != null) {
            responseHeaders.setContentType(contentType);
        }
        return ResponseEntity.status(upstreamResponse.getStatusCode())
                .headers(responseHeaders)
                .body(upstreamResponse.getBody());
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
