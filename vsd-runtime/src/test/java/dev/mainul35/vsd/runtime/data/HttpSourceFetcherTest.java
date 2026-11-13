package dev.mainul35.vsd.runtime.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpSourceFetcherTest {

    private static RuntimeProperties.Data config(boolean allowPrivate, String... hosts) {
        RuntimeProperties.Data d = new RuntimeProperties.Data();
        d.setAllowedHosts(List.of(hosts));
        d.setAllowPrivateAddresses(allowPrivate);
        d.setMaxResponseBytes(1000);
        return d;
    }

    private static SourceSpec api(String url) throws Exception {
        return SourceSpec.parse(new ObjectMapper().readTree("{\"type\":\"api\",\"endpoint\":\"" + url + "\"}"));
    }

    @Test
    void refusesHostsThatAreNotAllowListed() {
        HttpSourceFetcher f = new HttpSourceFetcher(config(false, "api.example.com"));
        assertThrows(SecurityException.class, () -> f.checkedUri("https://evil.example.org/x"));
        assertThrows(SecurityException.class, () -> f.checkedUri("http://localhost:8080/x"));
    }

    @Test
    void withNoAllowedHostsEveryApiSourceIsRefused() {
        HttpSourceFetcher f = new HttpSourceFetcher(config(true));
        assertThrows(SecurityException.class, () -> f.checkedUri("https://api.example.com/x"));
    }

    @Test
    void refusesOddEndpoints() {
        HttpSourceFetcher f = new HttpSourceFetcher(config(true, "a.test"));
        for (String bad : new String[]{"", "   ", "/api/relative", "ftp://a.test/x", "file:///etc/passwd",
                "https://user:pw@a.test/x", "javascript:alert(1)", "http://"}) {
            assertThrows(IllegalArgumentException.class, () -> f.checkedUri(bad), bad);
        }
    }

    @Test
    void hostMatchIsExactNotASuffix() {
        HttpSourceFetcher f = new HttpSourceFetcher(config(true, "a.test"));
        assertThrows(SecurityException.class, () -> f.checkedUri("https://a.test.evil.com/x"));
        assertThrows(SecurityException.class, () -> f.checkedUri("https://evil-a.test/x"));
    }

    @Test
    void internalAddressesAreRecognised() throws Exception {
        for (String ip : new String[]{"127.0.0.1", "10.1.2.3", "192.168.0.5", "172.16.0.1", "169.254.169.254",
                "0.0.0.0", "100.64.0.1", "::1", "fd00::1", "fe80::1", "224.0.0.1"}) {
            assertTrue(HttpSourceFetcher.isInternal(InetAddress.getByName(ip)), ip);
        }
        for (String ip : new String[]{"8.8.8.8", "93.184.216.34", "2606:4700:4700::1111"}) {
            assertFalse(HttpSourceFetcher.isInternal(InetAddress.getByName(ip)), ip);
        }
    }

    @Test
    void aListedHostOnAnInternalAddressIsStillRefusedUnlessPrivateAddressesAreAllowed() {
        assertThrows(SecurityException.class, () -> new HttpSourceFetcher(config(false, "127.0.0.1")).checkedUri("http://127.0.0.1:1/x"));
        // allowed once the operator opts in
        assertDoesNotThrow(() -> new HttpSourceFetcher(config(true, "127.0.0.1")).checkedUri("http://127.0.0.1:1/x"));
    }

    @Test
    void fetchesJsonSendsConfiguredHeadersAndDoesNotFollowRedirects() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        AtomicReference<String> auth = new AtomicReference<>();
        server.createContext("/ok", ex -> {
            auth.set(ex.getRequestHeaders().getFirst("X-Api-Key"));
            byte[] body = "{\"items\":[1,2,3]}".getBytes();
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.createContext("/redirect", ex -> {
            ex.getResponseHeaders().add("Location", "/ok");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        server.createContext("/big", ex -> {
            byte[] body = ("\"" + "x".repeat(5000) + "\"").getBytes();
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            HttpSourceFetcher f = new HttpSourceFetcher(config(true, "127.0.0.1"));
            SourceSpec spec = SourceSpec.parse(new ObjectMapper().readTree(
                    "{\"type\":\"api\",\"endpoint\":\"http://127.0.0.1:" + port + "/ok\",\"headers\":{\"X-Api-Key\":\"k1\",\"Host\":\"evil\"}}"));
            JsonNode body = f.load(spec);
            assertEquals(3, body.get("items").size());
            assertEquals("k1", auth.get());

            Exception redirect = assertThrows(Exception.class, () -> f.load(api("http://127.0.0.1:" + port + "/redirect")));
            assertTrue(redirect.getMessage().contains("302"), redirect.getMessage());
            Exception big = assertThrows(Exception.class, () -> f.load(api("http://127.0.0.1:" + port + "/big")));
            assertTrue(big.getMessage().contains("larger"), big.getMessage());
        } finally {
            server.stop(0);
        }
    }
}
