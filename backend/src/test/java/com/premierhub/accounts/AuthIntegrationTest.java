package com.premierhub.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:account-integration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "server.servlet.session.cookie.secure=false",
        "PREMIERHUB_CORS_ALLOWED_ORIGINS=https://frontend.example"
})
class AuthIntegrationTest {
    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private static final AtomicInteger ids = new AtomicInteger();
    private static final String PASSWORD = "Test-password-2026";
    private CookieManager cookies;
    private HttpClient client;
    private String email;

    @BeforeEach void setup() {
        cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder().cookieHandler(cookies).build();
        email = "account" + ids.incrementAndGet() + "@example.com";
    }

    HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
    JsonNode json(HttpResponse<String> response) { return mapper.readTree(response.body()); }
    JsonNode csrf() throws Exception {
        var response = get("/api/auth/csrf"); assertEquals(200, response.statusCode()); return json(response);
    }
    HttpResponse<String> post(String path, String body, JsonNode csrf) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json");
        if (csrf != null) request.header(csrf.path("headerName").asText(), csrf.path("token").asText());
        return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> register(String address, String password) throws Exception {
        return post("/api/auth/register", mapper.writeValueAsString(Map.of("email", address, "displayName", "Người chơi", "password", password, "role", "ADMIN")), csrf());
    }
    HttpResponse<String> login(String address, String password) throws Exception {
        return post("/api/auth/login", mapper.writeValueAsString(Map.of("email", address, "password", password)), csrf());
    }
    String sessionCookie() { return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("SESSION")).findFirst().orElseThrow().getValue(); }

    @Test void normalizesEmailRejectsDuplicateAndNeverAcceptsRoleOrExposesHash() throws Exception {
        var created = register("  " + email.toUpperCase(java.util.Locale.ROOT) + "  ", PASSWORD);
        assertEquals(201, created.statusCode());
        assertEquals(email, json(created).path("email").asText());
        assertEquals("USER", json(created).path("role").asText());
        assertTrue(json(created).path("id").asLong() > 0);
        assertFalse(created.body().contains("password")); assertFalse(created.body().contains(PASSWORD));
        assertEquals(401, get("/api/auth/me").statusCode());
        assertEquals(409, register(email, PASSWORD).statusCode());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM accounts WHERE email=?", Integer.class, email));
        String hash = jdbc.queryForObject("SELECT password_hash FROM accounts WHERE email=?", String.class, email);
        assertNotEquals(PASSWORD, hash); assertTrue(hash.startsWith("{bcrypt}"));
        assertEquals("USER", jdbc.queryForObject("SELECT role FROM accounts WHERE email=?", String.class, email));
    }

    @Test void rejectsWrongPasswordAndUnknownEmailWithTheSameSafeError() throws Exception {
        assertEquals(201, register(email, PASSWORD).statusCode());
        var wrong = login(email, "Wrong-password");
        var unknown = login("unknown-" + email, "Wrong-password");
        assertEquals(401, wrong.statusCode()); assertEquals(401, unknown.statusCode());
        assertEquals(wrong.body(), unknown.body());
        assertFalse(wrong.body().contains("Wrong-password"));
        assertEquals(401, get("/api/auth/me").statusCode());
    }

    @Test void rotatesSessionAndCsrfPersistsLoginAndInvalidatesLogoutIncludingReplay() throws Exception {
        assertEquals(201, register(email, PASSWORD).statusCode());
        var oldToken = csrf(); String anonymousCookie = sessionCookie();
        var loggedIn = post("/api/auth/login", mapper.writeValueAsString(Map.of("email", email.toUpperCase(java.util.Locale.ROOT), "password", PASSWORD)), oldToken);
        assertEquals(200, loggedIn.statusCode());
        String authenticatedCookie = sessionCookie(); assertNotEquals(anonymousCookie, authenticatedCookie);
        String setCookie = loggedIn.headers().allValues("set-cookie").toString();
        assertTrue(setCookie.contains("HttpOnly")); assertTrue(setCookie.contains("SameSite=Lax"));
        assertEquals(200, get("/api/auth/me").statusCode());
        assertEquals(email, json(get("/api/auth/me")).path("email").asText());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION WHERE PRINCIPAL_NAME=?", Integer.class, email));
        assertEquals(401, replay(anonymousCookie).statusCode());
        assertEquals(403, post("/api/auth/logout", "{}", oldToken).statusCode());
        assertEquals(200, get("/api/auth/me").statusCode());
        assertEquals(204, post("/api/auth/logout", "{}", csrf()).statusCode());
        assertEquals(401, get("/api/auth/me").statusCode());
        assertEquals(401, replay(authenticatedCookie).statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION WHERE PRINCIPAL_NAME=?", Integer.class, email));
    }

    HttpResponse<String> replay(String cookie) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/me"))
                .header("Cookie", "SESSION=" + cookie).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void protectsRegistrationAndLoginAgainstMissingCsrf() throws Exception {
        String body = mapper.writeValueAsString(Map.of("email", email, "displayName", "Player", "password", PASSWORD));
        assertEquals(403, post("/api/auth/register", body, null).statusCode());
        assertEquals(403, post("/api/auth/login", body, null).statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM accounts WHERE email=?", Integer.class, email));
    }

    @Test void validatesInputAndRedactsMalformedRequestAndMultibytePasswords() throws Exception {
        var invalidEmail = register("not-an-email", PASSWORD); assertEquals(400, invalidEmail.statusCode());
        assertFalse(invalidEmail.body().contains(PASSWORD));
        assertEquals(400, register(email, "short").statusCode());
        assertEquals(400, register(email, "é".repeat(40)).statusCode());
        var malformed = post("/api/auth/register", "{\"password\":\"do-not-log-this\",", csrf());
        assertEquals(400, malformed.statusCode()); assertFalse(malformed.body().contains("do-not-log-this"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM accounts WHERE email=?", Integer.class, email));
    }

    @Test void lookupApisAndFantasyValidationStayPublicForBothSeasons() throws Exception {
        assertEquals(401, get("/api/auth/me").statusCode());
        for (int season : List.of(2024, 2026)) for (String path : List.of("clubs", "players", "matches", "standings")) {
            assertEquals(200, get("/api/" + path + "?season=" + season).statusCode(), path + " " + season);
        }
        var validation = post("/api/fantasy/2026/validate", "{\"formation\":\"4-3-3\",\"picks\":{}}", null);
        assertEquals(200, validation.statusCode()); assertFalse(json(validation).path("valid").asBoolean());
    }

    @Test void corsAllowsCredentialedAuthOnlyForExplicitOrigins() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Origin", "https://frontend.example").header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-csrf-token").method("OPTIONS", HttpRequest.BodyPublishers.noBody());
        var allowed = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, allowed.statusCode());
        assertEquals("true", allowed.headers().firstValue("access-control-allow-credentials").orElseThrow());
        assertEquals("https://frontend.example", allowed.headers().firstValue("access-control-allow-origin").orElseThrow());
        var denied = client.send(request.setHeader("Origin", "https://untrusted.example").build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, denied.statusCode());
    }
}
