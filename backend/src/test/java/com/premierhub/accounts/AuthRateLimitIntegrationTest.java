package com.premierhub.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-rate-limit;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "PREMIERHUB_GOOGLE_CLIENT_ID=", "PREMIERHUB_GOOGLE_CLIENT_SECRET=",
        "PREMIERHUB_CORS_ALLOWED_ORIGINS=https://frontend.example",
        "premierhub.auth-rate-limit.login-ip.attempts=3", "premierhub.auth-rate-limit.login-ip.window=60s",
        "premierhub.auth-rate-limit.login-email.attempts=2", "premierhub.auth-rate-limit.login-email.window=60s",
        "premierhub.auth-rate-limit.registration-ip.attempts=2", "premierhub.auth-rate-limit.registration-ip.window=60s",
        "premierhub.auth-rate-limit.google-ip.attempts=2", "premierhub.auth-rate-limit.google-ip.window=60s"
})
@Import(AuthRateLimitIntegrationTest.TestClock.class)
class AuthRateLimitIntegrationTest {
    @TestConfiguration(proxyBeanMethods = false)
    static class TestClock {
        @Bean AtomicLong rateLimitTime() { return new AtomicLong(); }
        @Bean @Primary AuthRateLimiter testLimiter(AuthRateLimitSettings settings, AtomicLong time) {
            return new AuthRateLimiter(settings, time::get);
        }
    }

    @Value("${local.server.port}") int port;
    @Autowired AtomicLong time;
    @Autowired JdbcTemplate jdbc;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private HttpClient client;
    private String email;
    private static final String PASSWORD = "Rate-test-password";

    @BeforeEach void setup() {
        time.addAndGet(Duration.ofDays(1).toNanos());
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        email = "rate" + time.get() + "@example.com";
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, Map<String, ?> body, boolean csrf, String forwarded) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/" + path))
                .header("Content-Type", "application/json").header("Origin", "https://frontend.example");
        if (csrf) {
            var token = mapper.readTree(get("/api/auth/csrf").body());
            request.header(token.path("headerName").asText(), token.path("token").asText());
        }
        if (forwarded != null) request.header("X-Forwarded-For", forwarded);
        return client.send(request.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, String> registration(String address) {
        return Map.of("email", address, "displayName", "Rate test", "password", PASSWORD);
    }

    private void limited(HttpResponse<String> response) {
        assertEquals(429, response.statusCode());
        assertEquals("60", response.headers().firstValue("Retry-After").orElseThrow());
        assertTrue(response.headers().firstValue("Access-Control-Expose-Headers").orElseThrow().contains("Retry-After"));
        var body = mapper.readTree(response.body());
        assertEquals("AUTH_RATE_LIMITED", body.path("code").asText());
        assertEquals(60, body.path("retryAfterSeconds").asInt());
        assertFalse(response.body().contains(email));
        assertFalse(response.body().contains(PASSWORD));
    }

    @Test void normalizesEmailBeforeLimitingAndReturnsSafeRetryMetadata() throws Exception {
        assertEquals(401, post("login", Map.of("email", email, "password", PASSWORD), true, null).statusCode());
        assertEquals(401, post("login", Map.of("email", "  " + email.toUpperCase(java.util.Locale.ROOT) + "  ", "password", PASSWORD), true, null).statusCode());
        var blocked = post("login", Map.of("email", email, "password", PASSWORD), true, null);
        limited(blocked);
        time.addAndGet(Duration.ofSeconds(60).toNanos());
        assertEquals(401, post("login", Map.of("email", email, "password", PASSWORD), true, null).statusCode());
    }

    @Test void fakeForwardedHeadersCannotBypassIpLimitAndCsrfStillRunsFirst() throws Exception {
        for (int index = 0; index < 4; index++) {
            assertEquals(403, post("login", Map.of("email", email, "password", PASSWORD), false, "192.0.2." + index).statusCode());
        }
        for (int index = 0; index < 3; index++) {
            assertEquals(401, post("login", Map.of("email", index + email, "password", PASSWORD), true, "192.0.2." + index).statusCode());
        }
        limited(post("login", Map.of("email", "other" + email, "password", PASSWORD), true, "192.0.2.99"));
        assertEquals(200, get("/api/clubs").statusCode());
        assertEquals(401, get("/api/auth/me").statusCode());
    }

    @Test void registrationIsBoundedAndRecoversWithoutCreatingBlockedAccounts() throws Exception {
        assertEquals(201, post("register", registration(email), true, null).statusCode());
        assertEquals(201, post("register", registration("second" + email), true, null).statusCode());
        limited(post("register", registration("third" + email), true, null));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM accounts WHERE email=?", Integer.class, "third" + email));
        time.addAndGet(Duration.ofSeconds(60).toNanos());
        assertEquals(201, post("register", registration("third" + email), true, null).statusCode());
    }

    @Test void googleLoginAndLinkShareTheIpBucketBeforeStartingAnyOauthFlow() throws Exception {
        assertEquals(503, post("google/start", Map.of("mode", "LOGIN", "returnPath", "/"), true, null).statusCode());
        assertEquals(503, post("google/start", Map.of("mode", "LINK", "returnPath", "/"), true, null).statusCode());
        limited(post("google/start", Map.of("mode", "LOGIN", "returnPath", "/"), true, null));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM account_identities", Integer.class));
    }

    @Test void limitedLoginDoesNotRevokeExistingSessionOrBlockLogout() throws Exception {
        var registered = post("register", registration(email), true, null);
        assertEquals(201, registered.statusCode());
        long id = mapper.readTree(registered.body()).path("id").asLong();
        assertEquals(200, post("login", Map.of("email", email, "password", PASSWORD), true, null).statusCode());
        assertEquals(401, post("login", Map.of("email", email, "password", "Wrong-password"), true, null).statusCode());
        limited(post("login", Map.of("email", email, "password", PASSWORD), true, null));
        assertEquals(id, mapper.readTree(get("/api/auth/me").body()).path("id").asLong());
        assertEquals(204, post("logout", Map.of(), true, null).statusCode());
        assertEquals(401, get("/api/auth/me").statusCode());
    }
}
