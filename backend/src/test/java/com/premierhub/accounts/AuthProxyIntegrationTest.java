package com.premierhub.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-proxy;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "premierhub.auth-proxy.enabled=true", "PREMIERHUB_AUTH_PROXY_SECRET=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "PREMIERHUB_AUTH_PUBLIC_ORIGIN=https://premierhub.vercel.app",
        "server.servlet.session.cookie.secure=true", "server.servlet.session.cookie.path=/",
        "PREMIERHUB_CORS_ALLOWED_ORIGINS=https://premierhub.vercel.app",
        "PREMIERHUB_GOOGLE_CLIENT_ID=test-client", "PREMIERHUB_GOOGLE_CLIENT_SECRET=test-secret",
        "PREMIERHUB_GOOGLE_FRONTEND_ORIGIN=https://premierhub.vercel.app",
        "PREMIERHUB_GOOGLE_CALLBACK_URI=https://premierhub.vercel.app/api/auth/google/callback"
})
class AuthProxyIntegrationTest {
    @Value("${local.server.port}") int port;
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private final JsonMapper mapper = JsonMapper.builder().build();
    private static final AtomicInteger ids = new AtomicInteger();
    private String cookie;
    private String email;
    private String ip;

    @BeforeEach void setup() { cookie = null; int id = ids.incrementAndGet(); email = "proxy" + id + "@example.com"; ip = "192.0.2." + id; }

    private HttpResponse<String> call(String method, String path, Map<String, ?> body, boolean trusted, JsonNode csrf) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Origin", "https://premierhub.vercel.app");
        if (trusted) request.header(AuthProxyFilter.PROXY_SECRET, "a".repeat(64)).header("X-Vercel-Forwarded-For", ip);
        // Model the cookie forwarded by Vercel, not a browser sending Secure cookies over HTTP.
        if (cookie != null) request.header("Cookie", cookie);
        if (csrf != null) request.header(csrf.path("headerName").asText(), csrf.path("token").asText());
        if (body != null) request.header("Content-Type", "application/json");
        var response = client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
        for (String set : response.headers().allValues("Set-Cookie")) {
            if (set.startsWith("SESSION=")) cookie = set.contains("Max-Age=0") ? null : set.split(";", 2)[0];
        }
        return response;
    }

    private JsonNode csrf() throws Exception {
        var response = call("GET", "/api/auth/csrf", null, true, null);
        assertEquals(200, response.statusCode());
        return mapper.readTree(response.body());
    }

    private long signIn() throws Exception {
        assertEquals(201, call("POST", "/api/auth/register", Map.of("email", email, "displayName", "Proxy test", "password", "Proxy-test-password"), true, csrf()).statusCode());
        var response = call("POST", "/api/auth/login", Map.of("email", email, "password", "Proxy-test-password"), true, csrf());
        assertEquals(200, response.statusCode());
        return mapper.readTree(response.body()).path("id").asLong();
    }

    @Test void secureHostOnlyCookieCsrfMeAndLogoutUseTheSameProxiedSession() throws Exception {
        var initial = call("GET", "/api/auth/csrf", null, true, null);
        assertEquals(200, initial.statusCode());
        String set = initial.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(set.contains("Secure")); assertTrue(set.contains("HttpOnly"));
        assertTrue(set.contains("SameSite=Lax")); assertTrue(set.contains("Path=/")); assertFalse(set.contains("Domain="));
        assertEquals("no-store", initial.headers().firstValue("Vercel-CDN-Cache-Control").orElseThrow());
        long id = signIn();
        assertEquals(id, mapper.readTree(call("GET", "/api/auth/me", null, true, null).body()).path("id").asLong());
        assertEquals(403, call("POST", "/api/auth/logout", null, true, null).statusCode());
        assertEquals(204, call("POST", "/api/auth/logout", null, true, csrf()).statusCode());
        assertNull(cookie);
        assertEquals(401, call("GET", "/api/auth/me", null, true, null).statusCode());
    }

    @Test void directAuthIsRejectedButPublicLookupsRemainPublic() throws Exception {
        assertEquals(403, call("GET", "/api/auth/csrf", null, false, null).statusCode());
        assertEquals(403, call("GET", "/api/auth/me", null, false, null).statusCode());
        assertNull(cookie);
        assertEquals(200, call("GET", "/api/clubs", null, false, null).statusCode());
    }

    @Test void googleLoginAndLinkCallbacksMatchTheFrontendOriginAndPreserveTheAccount() throws Exception {
        cancelGoogle("LOGIN");
        assertEquals(401, call("GET", "/api/auth/me", null, true, null).statusCode());
        long id = signIn();
        cancelGoogle("LINK");
        assertEquals(id, mapper.readTree(call("GET", "/api/auth/me", null, true, null).body()).path("id").asLong());
    }

    private void cancelGoogle(String mode) throws Exception {
        var body = Map.of("mode", mode, "returnPath", "/#fantasy");
        assertEquals(200, call("POST", "/api/auth/google/start", body, true, csrf()).statusCode());
        var authorize = call("GET", "/api/auth/google/authorize/google", null, true, null);
        assertEquals(302, authorize.statusCode());
        URI provider = URI.create(authorize.headers().firstValue("Location").orElseThrow());
        var params = java.util.Arrays.stream(provider.getRawQuery().split("&")).map(pair -> pair.split("=", 2))
                .collect(java.util.stream.Collectors.toMap(pair -> pair[0], pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
        assertEquals("https://premierhub.vercel.app/api/auth/google/callback", params.get("redirect_uri"));
        var callback = call("GET", "/api/auth/google/callback?error=access_denied&state="
                + URLEncoder.encode(params.get("state"), StandardCharsets.UTF_8), null, true, null);
        assertEquals(302, callback.statusCode());
        assertEquals("https://premierhub.vercel.app/?google=cancelled#fantasy", callback.headers().firstValue("Location").orElseThrow());
    }
}
