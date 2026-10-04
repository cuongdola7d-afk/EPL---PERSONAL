package com.premierhub.accounts;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Per-instance fixed windows. No database lookup or account existence affects the counters. */
@Component
public class AuthRateLimiter {
    private record Key(String scope, String value) { }
    private record Attempt(Key key, AuthRateLimitSettings.Policy policy) { }
    private record Bucket(int count, long expiresAt) { }

    private final AuthRateLimitSettings settings;
    private final LongSupplier ticker;
    private final Map<Key, Bucket> buckets = new HashMap<>();
    private ScheduledExecutorService cleanup;

    @Autowired
    public AuthRateLimiter(AuthRateLimitSettings settings) { this(settings, System::nanoTime); }

    AuthRateLimiter(AuthRateLimitSettings settings, LongSupplier ticker) {
        if (settings.getMaxEntries() < 2 || settings.getMaxEntries() > 1_000_000
                || settings.getCleanupInterval() == null
                || settings.getCleanupInterval().compareTo(java.time.Duration.ofSeconds(1)) < 0
                || settings.getCleanupInterval().compareTo(java.time.Duration.ofDays(1)) > 0) {
            throw new IllegalArgumentException("Auth store requires 2..1000000 entries and cleanup every 1 second..1 day.");
        }
        this.settings = settings;
        this.ticker = ticker;
    }

    @PostConstruct
    void startCleanup() {
        cleanup = Executors.newSingleThreadScheduledExecutor(task -> {
            var thread = new Thread(task, "auth-rate-limit-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        long delay = settings.getCleanupInterval().toNanos();
        cleanup.scheduleWithFixedDelay(this::removeExpired, delay, delay, TimeUnit.NANOSECONDS);
    }

    @PreDestroy
    public void close() { if (cleanup != null) cleanup.shutdownNow(); }

    public void login(String ip, String email) {
        consume(List.of(new Attempt(new Key("login-ip", ip), settings.getLoginIp()),
                new Attempt(new Key("login-email", AccountService.normalizeEmail(email)), settings.getLoginEmail())));
    }

    public void register(String ip) {
        consume(List.of(new Attempt(new Key("register-ip", ip), settings.getRegistrationIp())));
    }

    public void google(String ip) {
        // LOGIN and LINK share a bucket; switching mode cannot bypass this limit.
        consume(List.of(new Attempt(new Key("google-ip", ip), settings.getGoogleIp())));
    }

    private synchronized void consume(List<Attempt> attempts) {
        long now = ticker.getAsLong();
        long wait = 0;
        int missing = 0;
        for (var attempt : attempts) {
            var bucket = buckets.get(attempt.key());
            if (bucket != null && now - bucket.expiresAt() >= 0) {
                buckets.remove(attempt.key());
                bucket = null;
            }
            if (bucket == null) missing++;
            else if (bucket.count() >= attempt.policy().attempts()) wait = Math.max(wait, bucket.expiresAt() - now);
        }
        if (wait > 0) throw limited(wait);
        if (buckets.size() + missing > settings.getMaxEntries()) {
            removeExpired(now);
            if (buckets.size() + missing > settings.getMaxEntries()) {
                // Never evict active counters, which would allow an attacker to reset limits.
                long earliest = buckets.values().stream().mapToLong(bucket -> bucket.expiresAt() - now).min().orElse(1);
                throw limited(earliest);
            }
        }
        for (var attempt : attempts) {
            var previous = buckets.get(attempt.key());
            buckets.put(attempt.key(), previous == null
                    ? new Bucket(1, now + attempt.policy().window().toNanos())
                    : new Bucket(previous.count() + 1, previous.expiresAt()));
        }
    }

    private AuthRateLimitException limited(long remainingNanos) {
        return new AuthRateLimitException(Math.max(1, (remainingNanos + 999_999_999L) / 1_000_000_000L));
    }

    synchronized void removeExpired() {
        removeExpired(ticker.getAsLong());
    }

    private void removeExpired(long now) {
        buckets.values().removeIf(bucket -> now - bucket.expiresAt() >= 0);
    }

    synchronized int bucketCount() { return buckets.size(); }
}
