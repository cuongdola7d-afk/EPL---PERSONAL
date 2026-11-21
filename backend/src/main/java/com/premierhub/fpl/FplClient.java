package com.premierhub.fpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class FplClient implements FplSource {
    public static final String DEFAULT_BASE_URL = "https://fantasy.premierleague.com/api/";
    public static final String USER_AGENT = "PremierHub-PrismaXI/0.1 (FPL-collector)";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_RETRIES = 2;

    private final HttpClient http;
    private final String baseUrl;
    private final long retryDelayMs;

    @Autowired
    public FplClient() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build(), DEFAULT_BASE_URL, 1000L);
    }

    FplClient(HttpClient http, String baseUrl, long retryDelayMs) {
        this.http = http;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.retryDelayMs = retryDelayMs;
    }

    @Override
    public String fetchBootstrapStatic() throws IOException, InterruptedException {
        return get("bootstrap-static/");
    }

    @Override
    public String fetchFixtures(int gameweek) throws IOException, InterruptedException {
        if (gameweek < 1 || gameweek > 38) {
            throw new IllegalArgumentException("gameweek must be between 1 and 38");
        }
        return get("fixtures/?event=" + gameweek);
    }

    @Override
    public String fetchEventLive(int gameweek) throws IOException, InterruptedException {
        if (gameweek < 1 || gameweek > 38) {
            throw new IllegalArgumentException("gameweek must be between 1 and 38");
        }
        return get("event/" + gameweek + "/live/");
    }

    @Override
    public String fetchElementSummary(int elementId) throws IOException, InterruptedException {
        if (elementId <= 0) {
            throw new IllegalArgumentException("elementId must be positive");
        }
        return get("element-summary/" + elementId + "/");
    }

    private String get(String path) throws IOException, InterruptedException {
        URI uri = URI.create(baseUrl + path);
        IOException lastFailure = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (attempt > 0 && retryDelayMs > 0) {
                Thread.sleep(retryDelayMs * attempt);
            }
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status == 200) {
                    return response.body();
                }
                lastFailure = new IOException("FPL endpoint " + path + " returned HTTP " + status);
                if (status < 500 && status != 429) {
                    throw lastFailure;
                }
            } catch (IOException error) {
                lastFailure = error;
                if (attempt == MAX_RETRIES) {
                    throw new IOException("FPL endpoint " + path + " failed after "
                            + (MAX_RETRIES + 1) + " attempts: " + error.getMessage(), error);
                }
            }
        }
        throw lastFailure != null ? lastFailure : new IOException("FPL endpoint " + path + " failed");
    }
}
