package com.premierhub.accounts;

import org.junit.jupiter.api.AfterAll;
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
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:google-integration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=", "server.servlet.session.cookie.secure=false",
        "PREMIERHUB_GOOGLE_CLIENT_ID=test-client", "PREMIERHUB_GOOGLE_CLIENT_SECRET=local-test-secret",
        "PREMIERHUB_GOOGLE_FRONTEND_ORIGIN=http://localhost:5173",
        "PREMIERHUB_GOOGLE_CALLBACK_URI=http://localhost:5173/api/auth/google/callback"
})
@Import(GoogleOAuthIntegrationTest.ProviderConfiguration.class)
class GoogleOAuthIntegrationTest {
    static final LocalOidcProvider provider = new LocalOidcProvider();
    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean GoogleAccountService accounts;
    CookieManager cookies;
    HttpClient client;
    String email, subject;
    final JsonMapper mapper = JsonMapper.builder().build();
    static final String PASSWORD = "Email-test-password";

    @TestConfiguration
    static class ProviderConfiguration {
        @Bean @Primary GoogleOAuthSecurity simulatedProvider(GoogleOAuthSettings settings, GoogleOAuthFlow flow) {
            return new GoogleOAuthSecurity(settings, flow) {
                @Override ClientRegistration registration() {
                    return ClientRegistration.withRegistrationId("google").clientId("test-client").clientSecret("local-test-secret")
                            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                            .scope("openid", "email", "profile").redirectUri("{baseUrl}/api/auth/google/callback")
                            .authorizationUri(provider.origin() + "/authorize").tokenUri(provider.origin() + "/token")
                            .jwkSetUri(provider.origin() + "/jwks").issuerUri(provider.origin())
                            .userInfoUri(provider.origin() + "/userinfo").userNameAttributeName("sub").clientName("Simulated Google").build();
                }
            };
        }
    }

    @BeforeEach void setup() {
        cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder().cookieHandler(cookies).build();
        subject = UUID.randomUUID().toString(); email = subject + "@example.com";
    }
    @AfterAll static void close() { provider.close(); }
    HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonNode json(HttpResponse<String> response) { return mapper.readTree(response.body()); }
    HttpResponse<String> post(String path, Map<String, ?> body) throws Exception {
        var csrf = json(get("/api/auth/csrf"));
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json")
                .header(csrf.path("headerName").asText(), csrf.path("token").asText())
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }
    String cookie() { return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("SESSION")).findFirst().orElseThrow().getValue(); }
    Map<String, String> start(String mode) throws Exception {
        assertEquals(200, post("/api/auth/google/start", Map.of("mode", mode, "returnPath", "/#matches/12?season=2026")).statusCode());
        var redirect = get("/api/auth/google/authorize/google"); assertEquals(302, redirect.statusCode());
        var parameters = LocalOidcProvider.params(URI.create(redirect.headers().firstValue("location").orElseThrow()).getRawQuery());
        assertEquals(java.util.Set.of("openid", "email", "profile"), java.util.Set.of(parameters.get("scope").split(" ")));
        assertNotNull(parameters.get("nonce")); assertNotNull(parameters.get("state")); assertEquals("S256", parameters.get("code_challenge_method"));
        return parameters;
    }
    HttpResponse<String> finish(Map<String, String> flow, String subject, String email, String nonce, boolean verified) throws Exception {
        return get("/api/auth/google/callback?state=" + flow.get("state") + "&code=" + provider.code(subject, email, nonce, verified));
    }
    HttpResponse<String> google(String mode, String subject, String email) throws Exception {
        var flow = start(mode); return finish(flow, subject, email, flow.get("nonce"), true);
    }
    void outcome(HttpResponse<String> response, String result) {
        assertEquals(302, response.statusCode());
        assertEquals("http://localhost:5173/?google=" + result + "#matches/12?season=2026", response.headers().firstValue("location").orElseThrow());
    }
    long emailLogin(String email) throws Exception {
        assertEquals(201, post("/api/auth/register", Map.of("email", email, "displayName", "Email Player", "password", PASSWORD)).statusCode());
        var response = post("/api/auth/login", Map.of("email", email, "password", PASSWORD));
        assertEquals(200, response.statusCode()); return json(response).path("id").asLong();
    }

    @Test void createsUserOnceUsesSubjectOnReturnAndUsesTheSameSessionMeAndLogout() throws Exception {
        var flow = start("LOGIN"); String anonymous = cookie();
        outcome(finish(flow, subject, email.toUpperCase(java.util.Locale.ROOT), flow.get("nonce"), true), "success");
        assertNotEquals(anonymous, cookie());
        var me = json(get("/api/auth/me")); long id = me.path("id").asLong();
        assertEquals(email, me.path("email").asText()); assertEquals("USER", me.path("role").asText());
        assertFalse(me.has("passwordHash"));
        assertNull(jdbc.queryForObject("SELECT password_hash FROM accounts WHERE id=?", String.class, id));
        assertEquals(id, jdbc.queryForObject("SELECT account_id FROM account_identities WHERE subject_id=?", Long.class, subject));
        assertEquals(id, json(get("/api/auth/me")).path("id").asLong());
        assertEquals(204, post("/api/auth/logout", Map.of()).statusCode()); assertEquals(401, get("/api/auth/me").statusCode());
        outcome(google("LOGIN", subject, "changed-" + email), "success");
        assertEquals(id, json(get("/api/auth/me")).path("id").asLong());
        assertEquals(email, json(get("/api/auth/me")).path("email").asText());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM account_identities WHERE subject_id=?", Integer.class, subject));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM accounts WHERE email=?", Integer.class, "changed-" + email));
    }

    @Test void duplicateEmailRequiresExistingLoginAndSeparateConfirmedLink() throws Exception {
        long id = emailLogin(email); post("/api/auth/logout", Map.of());
        outcome(google("LOGIN", subject, email), "link_required"); assertEquals(401, get("/api/auth/me").statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM account_identities WHERE subject_id=?", Integer.class, subject));
        assertEquals(200, post("/api/auth/login", Map.of("email", email, "password", PASSWORD)).statusCode());
        outcome(google("LINK", subject, email), "confirm_link");
        assertEquals(id, json(get("/api/auth/me")).path("id").asLong());
        assertEquals(email, json(get("/api/auth/google/status")).path("pendingEmail").asText());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM account_identities WHERE subject_id=?", Integer.class, subject));
        assertEquals(400, post("/api/auth/google/link/confirm", Map.of("confirmed", false)).statusCode());
        assertEquals(200, post("/api/auth/google/link/confirm", Map.of("confirmed", true, "subject", "untrusted-client-subject")).statusCode());
        assertEquals(409, post("/api/auth/google/link/confirm", Map.of("confirmed", true)).statusCode());
        assertEquals(id, jdbc.queryForObject("SELECT account_id FROM account_identities WHERE subject_id=?", Long.class, subject));
        post("/api/auth/logout", Map.of()); outcome(google("LOGIN", subject, email), "success");
        assertEquals(id, json(get("/api/auth/me")).path("id").asLong());
    }

    @Test void cannotLinkAnIdentityAlreadyOwnedByAnotherAccount() throws Exception {
        outcome(google("LOGIN", subject, email), "success"); long owner = json(get("/api/auth/me")).path("id").asLong();
        post("/api/auth/logout", Map.of()); long target = emailLogin("other-" + email);
        outcome(google("LINK", subject, email), "confirm_link");
        var response = post("/api/auth/google/link/confirm", Map.of("confirmed", true));
        assertEquals(409, response.statusCode()); assertEquals("identity_linked", json(response).path("code").asText());
        assertEquals(owner, jdbc.queryForObject("SELECT account_id FROM account_identities WHERE subject_id=?", Long.class, subject));
        assertEquals(target, json(get("/api/auth/me")).path("id").asLong());
    }

    @Test void linkingDifferentEmailPreservesAccountAndCanBeCancelledOrRepeatedIdempotently() throws Exception {
        long id = emailLogin(email);
        outcome(google("LINK", subject, "google-" + email), "confirm_link");
        assertEquals(204, post("/api/auth/google/link/cancel", Map.of()).statusCode());
        assertEquals(409, post("/api/auth/google/link/confirm", Map.of("confirmed", true)).statusCode());
        for (int i = 0; i < 2; i++) {
            outcome(google("LINK", subject, "google-" + email), "confirm_link");
            assertEquals(200, post("/api/auth/google/link/confirm", Map.of("confirmed", true)).statusCode());
        }
        assertEquals(email, json(get("/api/auth/me")).path("email").asText());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM account_identities WHERE account_id=?", Integer.class, id));
    }

    @Test void rejectsStateNonceAndUnverifiedEmailWithoutCreatingOrAuthenticatingAnAccount() throws Exception {
        var flow = start("LOGIN");
        outcome(get("/api/auth/google/callback?state=wrong&code=not-exchanged"), "invalid_flow");
        assertEquals(401, get("/api/auth/me").statusCode());
        flow = start("LOGIN"); outcome(finish(flow, subject, email, "wrong-nonce", true), "provider_error");
        assertEquals(401, get("/api/auth/me").statusCode());
        flow = start("LOGIN"); outcome(finish(flow, subject, email, flow.get("nonce"), false), "invalid_identity");
        assertEquals(401, get("/api/auth/me").statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM accounts WHERE email=?", Integer.class, email));
    }

    @Test void cancellationAndProviderErrorsPreserveExistingAccountAndDoNotLeakDescriptions() throws Exception {
        long id = emailLogin(email);
        var flow = start("LINK");
        outcome(get("/api/auth/google/callback?state=" + flow.get("state") + "&error=access_denied&error_description=do-not-return-this"), "cancelled");
        assertEquals(id, json(get("/api/auth/me")).path("id").asLong());
        flow = start("LINK"); outcome(get("/api/auth/google/callback?state=" + flow.get("state") + "&error=server_error&error_description=do-not-return-this"), "provider_error");
        assertEquals(id, json(get("/api/auth/me")).path("id").asLong());
    }

    @Test void localPersistenceFailureDoesNotLeaveTheTemporaryGooglePrincipalAuthenticated() throws Exception {
        doThrow(new DataAccessResourceFailureException("do-not-return-this")).when(accounts).login(any());
        outcome(google("LOGIN", subject, email), "provider_error");
        assertEquals(401, get("/api/auth/me").statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM accounts WHERE email=?", Integer.class, email));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM account_identities WHERE subject_id=?", Integer.class, subject));
    }

    @Test void accountSwitchOrLogoutDuringLinkCannotLinkOrResurrectThePreviousSession() throws Exception {
        emailLogin(email); var flow = start("LINK");
        long next = emailLogin("other-" + email);
        outcome(finish(flow, subject, email, flow.get("nonce"), true), "invalid_flow");
        assertEquals(next, json(get("/api/auth/me")).path("id").asLong());
        flow = start("LINK"); post("/api/auth/logout", Map.of());
        var rejected = finish(flow, subject, email, flow.get("nonce"), true);
        assertEquals(302, rejected.statusCode()); assertEquals(401, get("/api/auth/me").statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM account_identities WHERE subject_id=?", Integer.class, subject));
    }

    @Test void redirectsArePinnedAndStartAndConfirmationRequireCsrfAndSessionProof() throws Exception {
        for (String path : java.util.List.of("https://evil.example/", "//evil.example/", "/\\evil", "/%2f%2fevil", "/outside", "/\r\nInjected")) {
            assertEquals(400, post("/api/auth/google/start", Map.of("mode", "LOGIN", "returnPath", path)).statusCode());
        }
        assertEquals(401, post("/api/auth/google/start", Map.of("mode", "LINK", "returnPath", "/#fantasy")).statusCode());
        var noCsrf = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/google/start"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"mode\":\"LOGIN\",\"returnPath\":\"/\"}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, noCsrf.statusCode());
        emailLogin(email);
        assertEquals(409, post("/api/auth/google/link/confirm", Map.of("confirmed", true, "subject", subject, "email", email)).statusCode());
    }
}
