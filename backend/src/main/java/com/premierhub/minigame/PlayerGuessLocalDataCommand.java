package com.premierhub.minigame;

import com.premierhub.positions.PlayerPositionCsvReader;
import com.premierhub.positions.PlayerPositionImporter;
import com.premierhub.profiles.PlayerProfileCsvReader;
import com.premierhub.profiles.PlayerProfileImporter;
import com.premierhub.roster.ManualRosterCsvReader;
import com.premierhub.roster.ManualRosterImporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Replays reviewed repository data only into the dedicated Minigame local database. */
@Component
@Profile("minigame-local & !prod")
@ConditionalOnProperty(name = "premierhub.minigame.local-data.enabled", havingValue = "true")
public class PlayerGuessLocalDataCommand implements ApplicationRunner {
    private static final String FINAL_DATA = "player-data-production-2026-10-04";
    private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 10, 4);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final String datasourceUrl;

    public PlayerGuessLocalDataCommand(JdbcTemplate jdbc, TransactionTemplate transactions,
                                      @Value("${spring.datasource.url}") String datasourceUrl) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.datasourceUrl = datasourceUrl;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        requireDedicatedLocalDatabase(datasourceUrl);
        try (var connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            requireDedicatedLocalDatabase(connection.getMetaData().getURL());
        }
        Result result = importReviewedData(Path.of("data"));
        System.out.printf("MINIGAME_LOCAL_DATA snapshot=%s inserted=%d roster=%d eligibleAnswers=%d%n",
                SNAPSHOT_DATE, result.inserted(), result.rosterPlayers(), result.eligibleAnswers());
    }

    static void requireDedicatedLocalDatabase(String url) {
        String prefix = "jdbc:h2:file:";
        if (url == null || !url.startsWith(prefix)) {
            throw new IllegalStateException("Minigame local data requires the dedicated file H2 database.");
        }
        String[] parts = url.substring(prefix.length()).split(";", -1);
        Path expected = Path.of("target", "minigame-local").toAbsolutePath().normalize();
        if (!Path.of(parts[0]).toAbsolutePath().normalize().equals(expected)) {
            throw new IllegalStateException("Minigame local data may only write target/minigame-local.");
        }
        for (int index = 1; index < parts.length; index++) {
            if (!Set.of("MODE=MySQL", "DATABASE_TO_LOWER=TRUE", "DB_CLOSE_ON_EXIT=FALSE").contains(parts[index])) {
                throw new IllegalStateException("Unexpected option in the Minigame local datasource.");
            }
        }
    }

    // Package-private for isolated H2 tests; the application entrypoint always checks the datasource first.
    Result importReviewedData(Path dataRoot) throws IOException {
        List<Club> clubs = readClubs(dataRoot.resolve(FINAL_DATA).resolve("club-coverage.csv"));
        List<Path> rosterFiles = new ArrayList<>();
        List<ManualRosterCsvReader.Row> roster = new ArrayList<>();
        for (int batch = 1; batch <= 4; batch++) {
            Path file = dataRoot.resolve("roster-2026-09-30").resolve("batch-%02d.csv".formatted(batch));
            rosterFiles.add(file);
            roster.addAll(new ManualRosterCsvReader().read(file));
        }
        Path profileFile = dataRoot.resolve(FINAL_DATA).resolve("profiles-final.csv");
        Path positionFile = dataRoot.resolve(FINAL_DATA).resolve("positions-final.csv");
        var profiles = new PlayerProfileCsvReader().read(profileFile);
        var positions = new PlayerPositionCsvReader().read(positionFile);
        validateSnapshot(clubs, roster, profiles, positions);
        return transactions.execute(status -> {
            try {
                return insertSnapshot(clubs, roster, rosterFiles, profileFile, positionFile);
            } catch (IOException failure) {
                throw new java.io.UncheckedIOException(failure);
            }
        });
    }

    private Result insertSnapshot(List<Club> clubs, List<ManualRosterCsvReader.Row> roster,
                                  List<Path> rosterFiles, Path profileFile, Path positionFile) throws IOException {
        int inserted = 0;
        if (count("SELECT COUNT(*) FROM seasons WHERE league_id=39 AND season_year=2026") == 0) {
            inserted += jdbc.update("INSERT INTO seasons (league_id,season_year) VALUES (39,2026)");
        }
        for (Club club : clubs) {
            List<String> saved = jdbc.queryForList("SELECT name FROM clubs WHERE id=?", String.class, club.id());
            if (saved.isEmpty()) {
                inserted += jdbc.update("INSERT INTO clubs (id,name) VALUES (?,?)", club.id(), club.name());
            } else if (!saved.getFirst().equals(club.name())) {
                throw new IllegalStateException("Conflicting local club identity: " + club.id());
            }
            if (count("SELECT COUNT(*) FROM season_clubs WHERE league_id=39 AND season_year=2026 AND club_id=?", club.id()) == 0) {
                inserted += jdbc.update("INSERT INTO season_clubs VALUES (39,2026,?)", club.id());
            }
        }
        Map<Integer, String> names = new HashMap<>();
        jdbc.query("SELECT id,name FROM players", rs -> { names.put(rs.getInt("id"), rs.getString("name")); });
        List<Object[]> identities = new ArrayList<>();
        for (var row : roster) {
            String saved = names.get(row.playerId());
            if (saved == null) {
                // Provider IDs are verified identities from the reviewed roster, never generated here.
                identities.add(new Object[] {row.playerId(), row.name()});
            } else if (!saved.equals(row.name())) {
                throw new IllegalStateException("Conflicting local player identity: " + row.playerId());
            }
            List<Date> ends = jdbc.query("""
                    SELECT end_date FROM manual_player_memberships WHERE league_id=39 AND season_year=2026
                    AND player_id=? AND club_id=? AND start_date=?
                    """, (rs, index) -> rs.getDate(1), row.playerId(), row.clubId(), Date.valueOf(row.startDate()));
            Date incoming = row.endDate() == null ? null : Date.valueOf(row.endDate());
            if (!ends.isEmpty() && !Objects.equals(ends.getFirst(), incoming)) {
                throw new IllegalStateException("Conflicting local membership: " + row.playerId());
            }
        }
        if (!identities.isEmpty()) {
            jdbc.batchUpdate("INSERT INTO players (id,name) VALUES (?,?)", identities);
            inserted += identities.size();
        }
        var rosterImporter = new ManualRosterImporter(jdbc, transactions);
        for (Path file : rosterFiles) {
            var result = rosterImporter.importFile(file);
            if (result.intervalsUpdated() != 0) throw new IllegalStateException("Local bootstrap must not replace memberships.");
            inserted += result.playersInserted() + result.membershipsInserted() + result.intervalsInserted();
        }
        inserted += new PlayerProfileImporter(jdbc, transactions).importFile(profileFile, SNAPSHOT_DATE).inserted();
        int eligiblePositionsBefore = count("SELECT COUNT(*) FROM player_eligible_positions WHERE league_id=39 AND season_year=2026");
        var positions = new PlayerPositionImporter(jdbc, transactions).importFile(positionFile);
        if (positions.updated() != 0) throw new IllegalStateException("Local bootstrap must not replace positions.");
        inserted += positions.inserted();
        inserted += count("SELECT COUNT(*) FROM player_eligible_positions WHERE league_id=39 AND season_year=2026") - eligiblePositionsBefore;
        int eligible = (int) new PlayerGuessRepository(jdbc).candidates(SNAPSHOT_DATE).stream()
                .filter(candidate -> candidate.eligible(SNAPSHOT_DATE)).count();
        if (eligible < 2) throw new IllegalStateException("Reviewed local data must allow both daily and practice.");
        return new Result(inserted, roster.size(), eligible);
    }

    private static void validateSnapshot(List<Club> clubs, List<ManualRosterCsvReader.Row> roster,
                                         List<PlayerProfileCsvReader.Row> profiles, List<PlayerPositionCsvReader.Row> positions) {
        if (clubs.size() != 20 || roster.size() != 534 || profiles.size() != 534 || positions.size() != 532) {
            throw new IllegalArgumentException("Incomplete reviewed snapshot: expected 20 clubs, 534 players/profiles and 532 position sets.");
        }
        Set<Integer> clubIds = new HashSet<>();
        clubs.forEach(club -> { if (!clubIds.add(club.id())) throw new IllegalArgumentException("Duplicate club ID."); });
        Set<Integer> playerIds = new HashSet<>();
        Set<String> memberships = new HashSet<>();
        for (var row : roster) {
            if (!clubIds.contains(row.clubId()) || !playerIds.add(row.playerId()) || row.startDate().isAfter(SNAPSHOT_DATE)
                    || row.endDate() != null && !row.endDate().isAfter(SNAPSHOT_DATE)) {
                throw new IllegalArgumentException("Roster is not uniquely active at the reviewed snapshot: " + row.playerId());
            }
            memberships.add(row.playerId() + ":" + row.clubId());
        }
        Set<String> profileKeys = new HashSet<>();
        profiles.forEach(row -> profileKeys.add(row.playerId() + ":" + row.clubId()));
        if (!memberships.equals(profileKeys)) throw new IllegalArgumentException("Profile IDs/clubs differ from the reviewed roster.");
        for (var row : positions) {
            if (!playerIds.contains(row.playerId()) || row.expected() != null) {
                throw new IllegalArgumentException("Position input must belong to this snapshot and insert without replacing saved data.");
            }
        }
    }

    private static List<Club> readClubs(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.getFirst().replaceFirst("^\uFEFF", "").equals(
                "club_id,club,roster,profiles,complete_5_fields,ovr,primary_and_eligible,fantasy_eligible")) {
            throw new IllegalArgumentException("Unexpected club coverage CSV header.");
        }
        List<Club> clubs = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) continue;
            String[] values = line.split(",", -1);
            if (values.length != 8 || values[1].isBlank() || values[1].length() > 200) {
                throw new IllegalArgumentException("Invalid reviewed club identity.");
            }
            int id = Integer.parseInt(values[0]);
            if (id <= 0) throw new IllegalArgumentException("Club ID must be positive.");
            clubs.add(new Club(id, values[1]));
        }
        return List.copyOf(clubs);
    }

    private int count(String sql, Object... args) { return jdbc.queryForObject(sql, Integer.class, args); }
    private record Club(int id, String name) { }
    record Result(int inserted, int rosterPlayers, int eligibleAnswers) { }
}
