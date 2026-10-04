package com.premierhub.accounts;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Isolated simulated provider; real signatures/JWKS and HTTP, never contacts Google or production. */
class LocalOidcProvider implements AutoCloseable {
    private final HttpServer server;
    private final java.security.KeyPair keys;
    private final Map<String, Map<String, Object>> codes = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> access = new ConcurrentHashMap<>();
    private final JsonMapper json = JsonMapper.builder().build();

    LocalOidcProvider() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
            keys = generator.generateKeyPair();
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/jwks", exchange -> reply(exchange, new JWKSet(new RSAKey.Builder((RSAPublicKey) keys.getPublic()).keyID("local-key").build()).toJSONObject()));
            server.createContext("/token", exchange -> {
                try {
                    var form = params(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    var claims = codes.remove(form.get("code"));
                    if (claims == null || !form.containsKey("code_verifier")) { exchange.sendResponseHeaders(400, -1); exchange.close(); return; }
                    var builder = new JWTClaimsSet.Builder().issuer(origin()).audience("test-client")
                            .issueTime(Date.from(Instant.now())).expirationTime(Date.from(Instant.now().plusSeconds(300)));
                    claims.forEach(builder::claim);
                    var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("local-key").build(), builder.build());
                    jwt.sign(new RSASSASigner(keys.getPrivate()));
                    String token = UUID.randomUUID().toString(); access.put(token, claims);
                    reply(exchange, Map.of("access_token", token, "token_type", "Bearer", "expires_in", 300,
                            "scope", "openid email profile", "id_token", jwt.serialize()));
                } catch (Exception failure) { exchange.sendResponseHeaders(500, -1); exchange.close(); }
            });
            server.createContext("/userinfo", exchange -> {
                String header = exchange.getRequestHeaders().getFirst("Authorization");
                var claims = header == null ? null : access.get(header.substring("Bearer ".length()));
                if (claims == null) { exchange.sendResponseHeaders(401, -1); exchange.close(); return; }
                reply(exchange, claims);
            });
            server.start();
        } catch (Exception failure) { throw new IllegalStateException("Cannot start isolated OIDC fixture", failure); }
    }

    String origin() { return "http://localhost:" + server.getAddress().getPort(); }

    String code(String subject, String email, String nonce, boolean verified) {
        String code = UUID.randomUUID().toString();
        codes.put(code, Map.of("sub", subject, "email", email, "email_verified", verified, "name", "Google Player", "nonce", nonce, "role", "ADMIN"));
        return code;
    }

    static Map<String, String> params(String query) {
        var result = new HashMap<String, String>();
        if (query == null) return result;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            result.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), URLDecoder.decode(parts.length > 1 ? parts[1] : "", StandardCharsets.UTF_8));
        }
        return result;
    }

    private void reply(HttpExchange exchange, Object body) throws java.io.IOException {
        byte[] bytes = json.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
    }
    @Override public void close() { server.stop(0); }
}
