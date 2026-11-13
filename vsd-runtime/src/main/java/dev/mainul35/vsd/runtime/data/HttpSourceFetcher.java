package dev.mainul35.vsd.runtime.data;

import com.fasterxml.jackson.databind.JsonNode;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import dev.mainul35.vsd.runtime.util.Json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Set;

/**
 * Calls the HTTP endpoint of an API data source. The endpoint is authored by the page owner, so the operator
 * decides which hosts may be called ({@code allowed-hosts}); on top of that, hosts that resolve to loopback,
 * private or link-local addresses are refused unless explicitly enabled. Redirects are not followed, time and
 * response size are capped, and only GET and POST are possible.
 *
 * <p>Known limit: the address is checked before the call and the HTTP client resolves the name again, so a
 * hostile DNS server could answer differently the second time. The allow-list is what stops this in practice,
 * so only list hosts you trust.
 */
public class HttpSourceFetcher implements SourceLoader {

    private static final Set<String> RESTRICTED_HEADERS = Set.of(
            "host", "content-length", "connection", "transfer-encoding", "upgrade", "expect", "te", "trailer");

    private final RuntimeProperties.Data config;
    private final HttpClient client;

    public HttpSourceFetcher(RuntimeProperties.Data config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(config.getTimeout())
                .build();
    }

    @Override
    public JsonNode load(SourceSpec spec) throws Exception {
        URI uri = checkedUri(spec.endpoint());
        HttpRequest.Builder req = HttpRequest.newBuilder(uri).timeout(config.getTimeout())
                .header("Accept", "application/json");
        spec.headers().forEach((k, v) -> {
            if (!RESTRICTED_HEADERS.contains(k.toLowerCase(Locale.ROOT)) && k.matches("[A-Za-z0-9-]{1,64}")
                    && v != null && v.length() <= 4096 && v.indexOf('\r') < 0 && v.indexOf('\n') < 0) {
                req.header(k, v);
            }
        });
        if ("POST".equals(spec.method())) {
            req.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            req.GET();
        }
        HttpResponse<InputStream> res = client.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = res.body()) {
            if (res.statusCode() / 100 != 2) {
                throw new IOException("HTTP " + res.statusCode());
            }
            return Json.MAPPER.readTree(readCapped(in, config.getMaxResponseBytes()));
        }
    }

    /** Validates the endpoint against the operator's rules; returns the URI to call. */
    URI checkedUri(String endpoint) throws IOException {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("API data source has no endpoint");
        }
        URI uri;
        try {
            uri = URI.create(endpoint.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("endpoint is not a valid URL");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("endpoint must be an absolute http(s) URL");
        }
        if (uri.getRawUserInfo() != null || uri.getHost() == null) {
            throw new IllegalArgumentException("endpoint must have a host and no credentials");
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (config.getAllowedHosts().stream().noneMatch(h -> h.equalsIgnoreCase(host))) {
            throw new SecurityException("host '" + host + "' is not in vsd.runtime.data.allowed-hosts");
        }
        if (!config.isAllowPrivateAddresses()) {
            for (InetAddress a : InetAddress.getAllByName(host)) {
                if (isInternal(a)) {
                    throw new SecurityException("host '" + host + "' resolves to an internal address");
                }
            }
        }
        return uri;
    }

    /** Loopback, wildcard, link-local, private, multicast, carrier-grade NAT and IPv6 unique-local addresses. */
    static boolean isInternal(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) {
            return true;
        }
        byte[] b = a.getAddress();
        if (b.length == 4) {
            return (b[0] & 0xff) == 100 && (b[1] & 0xc0) == 0x40; // 100.64.0.0/10
        }
        return b.length == 16 && (b[0] & 0xfe) == 0xfc; // fc00::/7
    }

    private static byte[] readCapped(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            if (out.size() + n > max) {
                throw new IOException("response is larger than " + max + " bytes");
            }
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
