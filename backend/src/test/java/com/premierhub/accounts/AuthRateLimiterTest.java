package com.premierhub.accounts;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class AuthRateLimiterTest {
    private final AtomicLong time = new AtomicLong();
    private final AuthRateLimitSettings settings = new AuthRateLimitSettings();

    @Test void normalizedEmailHasOneLimitAcrossIpsWithoutBlockingOtherUsers() {
        settings.setLoginEmail(new AuthRateLimitSettings.Policy(2, Duration.ofSeconds(60)));
        var limiter = new AuthRateLimiter(settings, time::get);
        limiter.login("192.0.2.1", " PLAYER@Example.com ");
        limiter.login("192.0.2.2", "player@example.com");
        assertEquals(60, assertThrows(AuthRateLimitException.class,
                () -> limiter.login("192.0.2.3", "Player@example.com")).retryAfterSeconds());
        assertDoesNotThrow(() -> limiter.login("192.0.2.3", "another@example.com"));
        assertDoesNotThrow(() -> limiter.login("192.0.2.1", "another@example.com"));
    }

    @Test void changingEmailCannotBypassIpLimitAndOtherIpsRemainIndependent() {
        settings.setLoginIp(new AuthRateLimitSettings.Policy(2, Duration.ofSeconds(60)));
        var limiter = new AuthRateLimiter(settings, time::get);
        limiter.login("192.0.2.1", "one@example.com");
        limiter.login("192.0.2.1", "two@example.com");
        assertThrows(AuthRateLimitException.class, () -> limiter.login("192.0.2.1", "three@example.com"));
        assertDoesNotThrow(() -> limiter.login("192.0.2.2", "three@example.com"));
    }

    @Test void blockedAttemptsDoNotExtendWaitAndExactExpiryAllowsRetry() {
        settings.setRegistrationIp(new AuthRateLimitSettings.Policy(1, Duration.ofSeconds(3)));
        var limiter = new AuthRateLimiter(settings, time::get);
        limiter.register("client");
        time.set(Duration.ofMillis(1501).toNanos());
        assertEquals(2, assertThrows(AuthRateLimitException.class, () -> limiter.register("client")).retryAfterSeconds());
        time.set(Duration.ofMillis(2999).toNanos());
        assertEquals(1, assertThrows(AuthRateLimitException.class, () -> limiter.register("client")).retryAfterSeconds());
        time.set(Duration.ofSeconds(3).toNanos());
        assertDoesNotThrow(() -> limiter.register("client"));
    }

    @Test void registrationAndGoogleHaveSeparateBuckets() {
        settings.setRegistrationIp(new AuthRateLimitSettings.Policy(1, Duration.ofSeconds(60)));
        settings.setGoogleIp(new AuthRateLimitSettings.Policy(1, Duration.ofSeconds(60)));
        var limiter = new AuthRateLimiter(settings, time::get);
        limiter.register("client");
        assertThrows(AuthRateLimitException.class, () -> limiter.register("client"));
        assertDoesNotThrow(() -> limiter.google("client"));
        assertThrows(AuthRateLimitException.class, () -> limiter.google("client"));
        assertDoesNotThrow(() -> limiter.login("client", "player@example.com"));
    }

    @Test void capacityCannotEvictActiveLimitsAndExpiredEntriesAreRemoved() {
        settings.setMaxEntries(2);
        settings.setRegistrationIp(new AuthRateLimitSettings.Policy(1, Duration.ofSeconds(2)));
        var limiter = new AuthRateLimiter(settings, time::get);
        limiter.register("one"); limiter.register("two");
        assertEquals(2, assertThrows(AuthRateLimitException.class, () -> limiter.register("three")).retryAfterSeconds());
        assertThrows(AuthRateLimitException.class, () -> limiter.register("one"));
        assertEquals(2, limiter.bucketCount());
        time.set(Duration.ofSeconds(2).toNanos());
        limiter.removeExpired();
        assertEquals(0, limiter.bucketCount());
        assertDoesNotThrow(() -> limiter.login("four", "player@example.com"));
        assertEquals(2, limiter.bucketCount());
    }

    @Test void parallelRequestsCannotExceedThreshold() throws Exception {
        settings.setLoginIp(new AuthRateLimitSettings.Policy(7, Duration.ofSeconds(60)));
        var limiter = new AuthRateLimiter(settings, time::get);
        try (var workers = Executors.newFixedThreadPool(8)) {
            var results = workers.invokeAll(IntStream.range(0, 100).<java.util.concurrent.Callable<Boolean>>mapToObj(index -> () -> {
                try { limiter.login("same-ip", "player" + index + "@example.com"); return true; }
                catch (AuthRateLimitException limited) { return false; }
            }).toList());
            int accepted = 0;
            for (var result : results) if (result.get()) accepted++;
            assertEquals(7, accepted);
        }
    }

    @Test void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new AuthRateLimitSettings.Policy(0, Duration.ofSeconds(60)));
        assertThrows(IllegalArgumentException.class, () -> new AuthRateLimitSettings.Policy(1, Duration.ZERO));
        settings.setMaxEntries(1);
        assertThrows(IllegalArgumentException.class, () -> new AuthRateLimiter(settings, time::get));
    }
}
