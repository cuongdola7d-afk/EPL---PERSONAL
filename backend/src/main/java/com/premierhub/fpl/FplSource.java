package com.premierhub.fpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public interface FplSource {
    String fetchBootstrapStatic() throws IOException, InterruptedException;

    String fetchFixtures(int gameweek) throws IOException, InterruptedException;

    String fetchEventLive(int gameweek) throws IOException, InterruptedException;

    String fetchElementSummary(int elementId) throws IOException, InterruptedException;

    static FplSource fromDirectory(Path directory) {
        return new LocalDirectorySource(directory);
    }

    final class LocalDirectorySource implements FplSource {
        private final Path directory;

        private LocalDirectorySource(Path directory) {
            this.directory = directory;
        }

        @Override
        public String fetchBootstrapStatic() throws IOException {
            return read("bootstrap-static.json");
        }

        @Override
        public String fetchFixtures(int gameweek) throws IOException {
            Path gwFile = directory.resolve(String.format("fixtures-gw%02d.json", gameweek));
            if (Files.exists(gwFile)) {
                return Files.readString(gwFile, StandardCharsets.UTF_8);
            }
            return read("fixtures.json");
        }

        @Override
        public String fetchEventLive(int gameweek) throws IOException {
            Path gwFile = directory.resolve(String.format("event-gw%02d-live.json", gameweek));
            if (Files.exists(gwFile)) {
                return Files.readString(gwFile, StandardCharsets.UTF_8);
            }
            return read("event-live.json");
        }

        @Override
        public String fetchElementSummary(int elementId) throws IOException {
            return read("element-summary-" + elementId + ".json");
        }

        private String read(String fileName) throws IOException {
            Path file = directory.resolve(fileName);
            if (!Files.exists(file)) {
                throw new IOException("Missing local FPL JSON file: " + file);
            }
            return Files.readString(file, StandardCharsets.UTF_8);
        }
    }
}
