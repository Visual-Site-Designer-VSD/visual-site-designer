import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A stand-in for the VSD auth server so single sign-on can be tried locally.
 * It approves every sign-in at once as one fixed user and issues real RS256 tokens.
 *
 *   java -cp "<runtime classpath>" scripts/FakeIssuer.java [port]
 *
 * Development only: it has no login page and checks nothing about the visitor.
 */
public class FakeIssuer {

    static final Map<String, String[]> codes = new ConcurrentHashMap<>(); // code -> {nonce, clientId}

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9099;
        String issuer = "http://localhost:" + port;
        RSAKey key = new RSAKeyGenerator(2048).keyID("fake-1").generate();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/oauth2/jwks", ex -> send(ex, 200, "application/json", new JWKSet(key.toPublicJWK()).toString()));

        server.createContext("/oauth2/authorize", ex -> {
            Map<String, String> q = form(ex.getRequestURI().getRawQuery());
            String code = UUID.randomUUID().toString();
            codes.put(code, new String[]{q.get("nonce"), q.get("client_id")});
            String to = q.get("redirect_uri") + "?code=" + enc(code) + "&state=" + enc(q.get("state"));
            ex.getResponseHeaders().add("Location", to);
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });

        server.createContext("/oauth2/token", ex -> {
            Map<String, String> f = form(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String[] saved = codes.remove(f.get("code"));
            if (saved == null) {
                send(ex, 400, "application/json", "{\"error\":\"invalid_grant\"}");
                return;
            }
            try {
                JWTClaimsSet claims = new JWTClaimsSet.Builder()
                        .issuer(issuer).subject("ada@example.com").audience(saved[1])
                        .issueTime(new Date()).expirationTime(new Date(System.currentTimeMillis() + 300_000))
                        .claim("nonce", saved[0])
                        .claim("user_id", 7)
                        .claim("display_name", "Ada Lovelace")
                        .claim("email", "ada@example.com")
                        .claim("roles", List.of("ROLE_EDITOR"))
                        .build();
                SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
                jwt.sign(new RSASSASigner(key));
                send(ex, 200, "application/json", "{\"id_token\":\"" + jwt.serialize() + "\",\"token_type\":\"Bearer\"}");
            } catch (Exception e) {
                send(ex, 500, "application/json", "{\"error\":\"server_error\"}");
            }
        });

        server.createContext("/connect/logout", ex -> {
            Map<String, String> q = form(ex.getRequestURI().getRawQuery());
            ex.getResponseHeaders().add("Location", q.getOrDefault("post_logout_redirect_uri", "/"));
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });

        server.start();
        System.out.println("Fake issuer on " + issuer);
    }

    static void send(HttpExchange ex, int status, String type, String body) throws java.io.IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", type);
        ex.sendResponseHeaders(status, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }

    static Map<String, String> form(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null) {
            return m;
        }
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            if (i > 0) {
                m.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
            }
        }
        return m;
    }

    static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
