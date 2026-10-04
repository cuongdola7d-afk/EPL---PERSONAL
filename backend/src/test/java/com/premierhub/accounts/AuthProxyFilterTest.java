package com.premierhub.accounts;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AuthProxyFilterTest {
    private static final String SECRET = "a".repeat(64); // Synthetic test credential, never a deployment secret.

    private MockEnvironment environment() {
        return new MockEnvironment().withProperty("premierhub.auth-proxy.enabled", "true")
                .withProperty("PREMIERHUB_AUTH_PROXY_SECRET", SECRET)
                .withProperty("PREMIERHUB_AUTH_PUBLIC_ORIGIN", "https://premierhub.vercel.app")
                .withProperty("server.servlet.session.cookie.secure", "true");
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("GET", "/api/auth/csrf");
        request.setServletPath("/api/auth/csrf");
        request.setRemoteAddr("10.0.0.2");
        request.addHeader(AuthProxyFilter.PROXY_SECRET, SECRET);
        request.addHeader("X-Vercel-Forwarded-For", "198.51.100.7");
        return request;
    }

    @Test void verifiedProxyUsesPinnedHttpsOriginAndClientIpAndHidesSecret() throws Exception {
        var request = request();
        request.addHeader("Forwarded", "for=192.0.2.99;host=attacker.example;proto=http");
        request.addHeader("X-Forwarded-Host", "attacker.example");
        request.addHeader("X-Forwarded-For", "192.0.2.99");
        var response = new MockHttpServletResponse();
        var filter = new AuthProxyFilter(new AuthProxySettings(environment()));
        var called = new AtomicBoolean();
        filter.doFilter(request, response, (wrapped, ignored) -> {
            called.set(true);
            var verified = (HttpServletRequest) wrapped;
            assertTrue(verified.isSecure());
            assertEquals("https", verified.getScheme());
            assertEquals("premierhub.vercel.app", verified.getServerName());
            assertEquals(443, verified.getServerPort());
            assertEquals("https://premierhub.vercel.app/api/auth/csrf", verified.getRequestURL().toString());
            assertNull(verified.getHeader(AuthProxyFilter.PROXY_SECRET));
            assertFalse(verified.getHeaders(AuthProxyFilter.PROXY_SECRET).hasMoreElements());
            assertNull(verified.getHeader("Forwarded"));
            assertNull(verified.getHeader("X-Forwarded-Host"));
            assertEquals("198.51.100.7", new AuthClientIpResolver(new AuthRateLimitSettings(), "none").resolve(verified));
        });
        assertTrue(called.get());
        assertEquals("no-store", response.getHeader("Vercel-CDN-Cache-Control"));
    }

    @Test void missingInvalidOrDuplicateProofOrIpNeverReachesAuth() throws Exception {
        for (int scenario = 0; scenario < 6; scenario++) {
            var request = request();
            switch (scenario) {
                case 0 -> request.removeHeader(AuthProxyFilter.PROXY_SECRET);
                case 1 -> { request.removeHeader(AuthProxyFilter.PROXY_SECRET); request.addHeader(AuthProxyFilter.PROXY_SECRET, "b".repeat(64)); }
                case 2 -> request.addHeader(AuthProxyFilter.PROXY_SECRET, SECRET);
                case 3 -> request.removeHeader("X-Vercel-Forwarded-For");
                case 4 -> { request.removeHeader("X-Vercel-Forwarded-For"); request.addHeader("X-Vercel-Forwarded-For", "192.0.2.1, 192.0.2.2"); }
                case 5 -> request.addHeader("X-Vercel-Forwarded-For", "192.0.2.99");
            }
            var response = new MockHttpServletResponse();
            new AuthProxyFilter(new AuthProxySettings(environment())).doFilter(request, response, (req, res) -> fail("Untrusted request reached auth"));
            assertEquals(403, response.getStatus());
            assertTrue(response.getContentAsString().contains("AUTH_PROXY_REQUIRED"));
            assertFalse(response.getContentAsString().contains(SECRET));
        }
    }

    @Test void directLocalHttpAndPublicFootballRequestsKeepWorking() throws Exception {
        var response = new MockHttpServletResponse();
        var called = new AtomicBoolean();
        var request = request();
        request.removeHeader(AuthProxyFilter.PROXY_SECRET);
        new AuthProxyFilter(new AuthProxySettings(new MockEnvironment())).doFilter(request, response, (req, res) -> {
            called.set(true); assertFalse(((HttpServletRequest) req).isSecure());
        });
        assertTrue(called.get());
        request.setServletPath("/api/clubs");
        new AuthProxyFilter(new AuthProxySettings(environment())).doFilter(request, response, (req, res) -> called.set(false));
        assertFalse(called.get());
    }

    @Test void failsStartupForUnsafeCookieForwardingOrOriginConfiguration() {
        for (String[] invalid : new String[][] {
                {"PREMIERHUB_AUTH_PROXY_SECRET", "short"}, {"PREMIERHUB_AUTH_PUBLIC_ORIGIN", "http://premierhub.vercel.app"},
                {"PREMIERHUB_AUTH_PUBLIC_ORIGIN", "https://premierhub.vercel.app/"},
                {"server.servlet.session.cookie.secure", "false"}, {"server.servlet.session.cookie.http-only", "false"},
                {"server.servlet.session.cookie.domain", "railway.app"}, {"server.servlet.session.cookie.path", "/api"},
                {"server.servlet.session.cookie.same-site", "none"}, {"server.forward-headers-strategy", "framework"}
        }) assertThrows(IllegalArgumentException.class, () -> new AuthProxySettings(environment().withProperty(invalid[0], invalid[1])));
    }

    @Test void gameweekAdministrationRequiresTheSameProxyProofButPublicInfoDoesNot() throws Exception {
        var request = request();
        request.setServletPath("/api/fantasy/2026/admin/gameweeks/6/publish-deadline");
        request.removeHeader(AuthProxyFilter.PROXY_SECRET);
        var response = new MockHttpServletResponse();
        var filter = new AuthProxyFilter(new AuthProxySettings(environment()));
        filter.doFilter(request, response, (req, res) -> fail("Admin request bypassed proxy proof"));
        assertEquals(403, response.getStatus());
        request.addHeader(AuthProxyFilter.PROXY_SECRET, SECRET);
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> assertTrue(((HttpServletRequest) req).isSecure()));
        request.setServletPath("/api/fantasy/2026/gameweeks");
        request.removeHeader(AuthProxyFilter.PROXY_SECRET);
        var called = new AtomicBoolean();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> called.set(true));
        assertTrue(called.get());
    }

    @Test void refusesGoogleCallbackOnADifferentOriginAndAcceptsExactFrontendCallback() {
        var environment = environment().withProperty("PREMIERHUB_GOOGLE_CLIENT_ID", "test-client")
                .withProperty("PREMIERHUB_GOOGLE_CLIENT_SECRET", "test-secret")
                .withProperty("PREMIERHUB_GOOGLE_FRONTEND_ORIGIN", "https://premierhub.vercel.app")
                .withProperty("PREMIERHUB_GOOGLE_CALLBACK_URI", "https://epl-personal-production.up.railway.app/api/auth/google/callback");
        assertThrows(IllegalArgumentException.class, () -> new AuthProxySettings(environment));
        environment.withProperty("PREMIERHUB_GOOGLE_CALLBACK_URI", "https://premierhub.vercel.app/api/auth/google/callback");
        assertDoesNotThrow(() -> new AuthProxySettings(environment));
    }
}
