package com.premierhub.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "premierhub.fixture-evidence.enabled", havingValue = "true")
public class FixtureEvidenceCommand implements ApplicationRunner {
    private final FixtureEvidenceService evidence;
    private final FootballQueries queries;
    private final ObjectMapper mapper;

    public FixtureEvidenceCommand(FixtureEvidenceService evidence, FootballQueries queries,
                                  ObjectMapper mapper) {
        this.evidence = evidence;
        this.queries = queries;
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("premierhub.fixture-evidence.file")) {
            throw new IllegalArgumentException("Thiếu đường dẫn --premierhub.fixture-evidence.file.");
        }
        Path file = Path.of(args.getOptionValues("premierhub.fixture-evidence.file").getFirst());
        JsonNode payload = mapper.readTree(Files.readString(file));
        int fixtureId = payload.path("fixtureId").asInt(-1);
        evidence.save(payload);
        var detail = queries.matchDetail(fixtureId, 2024).orElseThrow();
        if (!"VERIFIED".equals(detail.evidenceStatus())) {
            throw new IllegalStateException("Bằng chứng fixture " + fixtureId + " không khớp: "
                    + detail.evidenceError());
        }
        long complete = java.util.stream.Stream.concat(detail.homePlayers().stream(),
                detail.awayPlayers().stream()).filter(player ->
                "COMPLETE".equals(player.score().status())).count();
        System.out.printf("EVIDENCE fixture=%d status=%s players=%d complete=%d%n",
                fixtureId, detail.evidenceStatus(),
                detail.homePlayers().size() + detail.awayPlayers().size(), complete);
    }
}
