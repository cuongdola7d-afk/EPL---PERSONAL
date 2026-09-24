package com.premierhub.sync;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Component
public class ApiFootballClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper;
    private int calls;
    private int dailyRemaining = Integer.MAX_VALUE;
    private long lastRequestNanos;

    public ApiFootballClient(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode get(String path, int budget) throws IOException, InterruptedException {
        if (calls >= budget || dailyRemaining <= 5) {
            throw new RequestBudgetReachedException("API request budget or daily safety reserve reached");
        }
        long elapsed = System.nanoTime() - lastRequestNanos;
        long wait = Duration.ofMillis(6500).toNanos() - elapsed;
        if (lastRequestNanos != 0 && wait > 0) {
            Thread.sleep(Duration.ofNanos(wait));
        }
        String key = readKey();
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://v3.football.api-sports.io" + path))
                .timeout(Duration.ofSeconds(30))
                .header("x-apisports-key", key)
                .header("Accept", "application/json")
                .GET().build();
        lastRequestNanos = System.nanoTime();
        calls++;
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        response.headers().firstValue("x-ratelimit-requests-remaining").ifPresent(value -> {
            try { dailyRemaining = Integer.parseInt(value); }
            catch (NumberFormatException ignored) { /* Continue using the local budget. */ }
        });
        if (response.statusCode() != 200) {
            throw new IOException("API-Football returned HTTP " + response.statusCode());
        }
        JsonNode body = mapper.readTree(response.body());
        if (body.path("errors").size() > 0) {
            throw new IOException("API-Football returned an API error for " + path);
        }
        return body;
    }

    public int calls() { return calls; }
    public int dailyRemaining() { return dailyRemaining; }

    private static String readKey() throws IOException {
        String key = System.getenv("API_FOOTBALL_KEY");
        if (key != null && !key.isBlank()) return key.strip();
        Path file = Path.of(".env.local");
        if (!Files.exists(file)) throw new IOException("Set API_FOOTBALL_KEY or backend/.env.local");
        List<String> lines = Files.readAllLines(file);
        for (String line : lines) {
            String clean = line.strip().replaceFirst("^\\uFEFF", "");
            if (clean.startsWith("API_FOOTBALL_KEY=")) {
                String value = clean.substring("API_FOOTBALL_KEY=".length()).strip();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!value.isBlank()) return value;
            }
        }
        throw new IOException("API_FOOTBALL_KEY is missing from backend/.env.local");
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static final class RequestBudgetReachedException extends RuntimeException {
        public RequestBudgetReachedException(String message) { super(message); }
    }
}
