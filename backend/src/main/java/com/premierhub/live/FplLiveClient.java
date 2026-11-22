package com.premierhub.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Component
public class FplLiveClient {
    public static final String DEFAULT_BASE_URL = "https://fantasy.premierleague.com/api/";
    public static final String USER_AGENT = "PremierHub-PrismaXI/0.1 (live-scores; +https://premierhub.vercel.app)";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final long[] BACKOFF_SECONDS = {60L, 120L, 240L, 300L};

    private static final Logger LOG = LoggerFactory.getLogger(FplLiveClient.class);

    private final HttpClient http;
    private final String baseUrl;
    private final Clock clock;

    private int consecutiveFailures = 0;
    private long currentBackoffSeconds = 0L;
    private Instant nextAllowedFetchAt = Instant.EPOCH;
    private long totalRequests = 0L;

    @Autowired
    public FplLiveClient(@Value("${premierhub.live.fpl-base-url:" + DEFAULT_BASE_URL + "}") String baseUrl) {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build(), baseUrl, Clock.systemUTC());
    }

    public FplLiveClient(HttpClient http, String baseUrl, Clock clock) {
        this.http = http;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.clock = clock;
    }

    public synchronized boolean isBackingOff() {
        return Instant.now(clock).isBefore(nextAllowedFetchAt);
    }

    public synchronized long currentBackoffSeconds() {
        return currentBackoffSeconds;
    }

    public synchronized Instant nextAllowedFetchAt() {
        return nextAllowedFetchAt;
    }

    public synchronized long totalRequests() {
        return totalRequests;
    }

    /**
     * Fetches fixtures/?event={gameweek} with at most 1 HTTP request per cycle.
     * Returns Optional.empty() when currently backing off or when the request fails.
     */
    public synchronized Optional<String> fetchEventFixtures(int gameweek) {
        if (gameweek < 1 || gameweek > 38) {
            throw new IllegalArgumentException("gameweek must be between 1 and 38");
        }
        Instant now = Instant.now(clock);
        if (now.isBefore(nextAllowedFetchAt)) {
            LOG.debug("Skipping FPL live fetch for GW{} during backoff until {}", gameweek, nextAllowedFetchAt);
            return Optional.empty();
        }

        URI uri = URI.create(baseUrl + "fixtures/?event=" + gameweek);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();

        totalRequests++;
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 200) {
                if (consecutiveFailures > 0) {
                    LOG.info("FPL live fetch recovered on GW{} after {} failures", gameweek, consecutiveFailures);
                }
                consecutiveFailures = 0;
                currentBackoffSeconds = 0L;
                nextAllowedFetchAt = Instant.EPOCH;
                return Optional.ofNullable(response.body());
            }
            if (status == 429) {
                long retrySec = parseRetryAfterSeconds(
                        response.headers().firstValue("Retry-After").orElse(null), now);
                applyBackoff(now, retrySec, "HTTP 429 Too Many Requests");
                return Optional.empty();
            }
            if (status >= 500) {
                applyExponentialBackoff(now, "HTTP " + status);
                return Optional.empty();
            }
            LOG.warn("FPL live fetch for GW{} returned unexpected HTTP {}", gameweek, status);
            return Optional.empty();
        } catch (IOException ex) {
            applyExponentialBackoff(now, "IOException: " + ex.getMessage());
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            applyExponentialBackoff(now, "InterruptedException");
            return Optional.empty();
        }
    }

    private void applyExponentialBackoff(Instant now, String reason) {
        int idx = Math.min(consecutiveFailures, BACKOFF_SECONDS.length - 1);
        long delaySec = BACKOFF_SECONDS[idx];
        applyBackoff(now, delaySec, reason);
    }

    private void applyBackoff(Instant now, long delaySec, String reason) {
        consecutiveFailures++;
        currentBackoffSeconds = delaySec;
        nextAllowedFetchAt = now.plusSeconds(delaySec);
        LOG.warn("FPL live fetch failed ({}); backing off for {}s until {} (failure #{})",
                reason, delaySec, nextAllowedFetchAt, consecutiveFailures);
    }

    private long parseRetryAfterSeconds(String retryAfter, Instant now) {
        int idx = Math.min(consecutiveFailures, BACKOFF_SECONDS.length - 1);
        long fallback = BACKOFF_SECONDS[idx];
        if (retryAfter == null || retryAfter.isBlank()) {
            return fallback;
        }
        String trimmed = retryAfter.strip();
        try {
            long seconds = Long.parseLong(trimmed);
            return Math.max(1L, seconds);
        } catch (NumberFormatException ignored) {
            try {
                Instant target = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                long diff = Duration.between(now, target).getSeconds();
                return Math.max(1L, diff);
            } catch (DateTimeParseException ignoredDate) {
                return fallback;
            }
        }
    }
}
