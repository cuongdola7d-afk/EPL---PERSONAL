package com.premierhub.sync;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class FootballDataClient {
    private final ObjectMapper mapper;
    private final HttpClient http;

    @Autowired
    public FootballDataClient(ObjectMapper mapper) {
        this(mapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build());
    }

    FootballDataClient(ObjectMapper mapper, HttpClient http) {
        this.mapper = mapper;
        this.http = http;
    }

    public FootballDataBatch download() throws IOException, InterruptedException {
        return download(readKey(System.getenv("FOOTBALL_DATA_API_KEY"), Path.of(".env.local")));
    }

    FootballDataBatch download(String key) throws IOException, InterruptedException {
        if (key == null || !key.matches("[A-Za-z0-9_-]+")) {
            throw new IOException("FOOTBALL_DATA_API_KEY has an invalid format");
        }
        List<JsonNode> bodies = new ArrayList<>();
        for (String resource : List.of("teams", "matches", "standings")) {
            // Free plan: at most 10 requests/minute. No retries, including 401/403/429.
            if (!bodies.isEmpty()) Thread.sleep(6500);
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            "https://api.football-data.org/v4/competitions/PL/" + resource + "?season=2026"))
                    .timeout(Duration.ofSeconds(30)).header("X-Auth-Token", key)
                    .header("Accept", "application/json").GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("football-data.org " + resource + " returned HTTP " + response.statusCode()
                        + "; no data imported, no retry");
            }
            try {
                bodies.add(mapper.readTree(response.body()));
            } catch (RuntimeException invalidJson) {
                // Do not include provider response bodies in logs/exceptions.
                throw new IOException("football-data.org returned invalid JSON for " + resource);
            }
        }
        return FootballDataBatch.parse(bodies.get(0), bodies.get(1), bodies.get(2));
    }

    public FootballDataBatch readCache(Path directory) throws IOException {
        List<JsonNode> bodies = new ArrayList<>();
        for (String resource : List.of("teams", "matches", "standings")) {
            bodies.add(mapper.readTree(Files.readString(directory.resolve("football-data-2026-" + resource + ".json"))));
        }
        return FootballDataBatch.parse(bodies.get(0), bodies.get(1), bodies.get(2));
    }

    static String readKey(String environmentKey, Path localFile) throws IOException {
        if (environmentKey != null && !environmentKey.isBlank()) return environmentKey.strip();
        if (Files.exists(localFile)) {
            for (String line : Files.readAllLines(localFile)) {
                String clean = line.replace("\uFEFF", "").strip();
                int equals = clean.indexOf('=');
                if (equals < 0 || !clean.substring(0, equals).strip().equals("FOOTBALL_DATA_API_KEY")) continue;
                String key = clean.substring(equals + 1).strip();
                if (key.length() >= 2 && ((key.startsWith("\"") && key.endsWith("\""))
                        || (key.startsWith("'") && key.endsWith("'")))) key = key.substring(1, key.length() - 1);
                if (!key.isBlank()) return key;
            }
        }
        throw new IOException("Set FOOTBALL_DATA_API_KEY or run from backend/ with backend/.env.local");
    }
}
