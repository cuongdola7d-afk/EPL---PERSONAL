package com.premierhub.accounts;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthClientIpResolverTest {
    private AuthClientIpResolver resolver(String... proxies) {
        var settings = new AuthRateLimitSettings();
        settings.setTrustedProxies(List.of(proxies));
        return new AuthClientIpResolver(settings, "none");
    }

    private MockHttpServletRequest request(String peer, String forwarded) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        request.addHeader("X-Forwarded-For", forwarded);
        request.addHeader("Forwarded", "for=192.0.2.99");
        return request;
    }

    @Test void ignoresSpoofedHeadersFromUntrustedPeersByDefault() {
        assertEquals("198.51.100.1", resolver().resolve(request("198.51.100.1", "192.0.2.1")));
        assertEquals("198.51.100.1", resolver("10.0.0.0/24").resolve(request("198.51.100.1", "192.0.2.1")));
    }

    @Test void walksTrustedProxyChainFromTheRightAndStopsAtTheClient() {
        var resolver = resolver("10.0.0.0/24");
        assertEquals("198.51.100.7", resolver.resolve(request("10.0.0.2", "192.0.2.99, 198.51.100.7, 10.0.0.3")));
        var multipleHeaders = request("10.0.0.2", "192.0.2.99");
        multipleHeaders.addHeader("X-Forwarded-For", "198.51.100.7");
        assertEquals("198.51.100.7", resolver.resolve(multipleHeaders));
    }

    @Test void invalidOrOversizedHeadersFallBackToTheSocketPeerWithoutDnsLookups() {
        var resolver = resolver("10.0.0.0/24");
        for (String header : List.of("attacker.example", "198.51.100.1,", "198.51.100.999", "x".repeat(2049), "1.1.1.1,".repeat(33))) {
            assertEquals("10.0.0.2", resolver.resolve(request("10.0.0.2", header)));
        }
    }

    @Test void canonicalizesIpv6AndUsesIpv6TrustedNetworks() {
        var resolver = resolver("2001:db8:1::/48");
        assertEquals("2001:db8:2:0:0:0:0:1", resolver.resolve(request("2001:db8:1::2", "2001:db8:2::1")));
        assertEquals(resolver().resolve(request("::1", "192.0.2.1")),
                resolver().resolve(request("0:0:0:0:0:0:0:1", "192.0.2.1")));
    }

    @Test void refusesGlobalForwardingAndInvalidTrustConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new AuthClientIpResolver(new AuthRateLimitSettings(), "framework"));
        for (String proxy : List.of("proxy.example", "0.0.0.0/0", "::/0", "10.0.0.1/33", "10.0.0.1/no", "10.0.0.1/24/1")) {
            assertThrows(IllegalArgumentException.class, () -> resolver(proxy));
        }
    }
}
