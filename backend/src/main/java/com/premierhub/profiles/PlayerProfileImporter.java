package com.premierhub.profiles;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
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

@Service
public class PlayerProfileImporter {
    private static final int LEAGUE = 39;
    private static final int SEASON = 2026;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final PlayerProfileCsvReader reader = new PlayerProfileCsvReader();

    public PlayerProfileImporter(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    public Result importFile(Path file, LocalDate asOf) throws IOException {
        Objects.requireNonNull(asOf, "asOf");
        if (asOf.isBefore(LocalDate.of(2026, 7, 1)) || asOf.isAfter(LocalDate.of(2027, 6, 30))) {
            throw new IllegalArgumentException("asOf must be inside season 2026/27");
        }
        List<PlayerProfileCsvReader.Row> rows = reader.read(file);
        return transactions.execute(status -> importRows(rows, asOf));
    }

    private Result importRows(List<PlayerProfileCsvReader.Row> rows, LocalDate asOf) {
        Set<Key> seasonMemberships = new HashSet<>();
        jdbc.query("SELECT player_id, club_id FROM player_season_stats WHERE league_id=? AND season_year=?",
                rs -> {
                    seasonMemberships.add(new Key(rs.getInt("player_id"), rs.getInt("club_id")));
                },
                LEAGUE, SEASON);

        Map<Integer, List<Integer>> activeClubs = new HashMap<>();
        jdbc.query("""
                SELECT player_id, club_id FROM manual_player_memberships
                WHERE league_id=? AND season_year=? AND start_date<=?
                  AND (end_date IS NULL OR end_date>?)
                """, rs -> {
                    activeClubs.computeIfAbsent(rs.getInt("player_id"), ignored -> new ArrayList<>())
                            .add(rs.getInt("club_id"));
                }, LEAGUE, SEASON, Date.valueOf(asOf), Date.valueOf(asOf));

        Map<Key, Profile> saved = new HashMap<>();
        jdbc.query("""
                SELECT player_id, club_id, nationality, birth_date, height_cm, preferred_foot,
                       shirt_number, fc27_overall FROM player_season_profiles
                WHERE league_id=? AND season_year=?
                """, rs -> {
                    Date birthDate = rs.getDate("birth_date");
                    saved.put(new Key(rs.getInt("player_id"), rs.getInt("club_id")),
                            new Profile(rs.getString("nationality"),
                                    birthDate == null ? null : birthDate.toLocalDate(),
                                    rs.getObject("height_cm", Integer.class), rs.getString("preferred_foot"),
                                    rs.getObject("shirt_number", Integer.class),
                                    rs.getObject("fc27_overall", Integer.class)));
                }, LEAGUE, SEASON);

        List<Object[]> inserts = new ArrayList<>();
        for (var row : rows) {
            if (row.birthDate() != null && (row.birthDate().isBefore(LocalDate.of(1900, 1, 1))
                    || row.birthDate().isAfter(asOf))) {
                throw invalid(row, "birth_date must be between 1900-01-01 and " + asOf);
            }
            Key key = new Key(row.playerId(), row.clubId());
            if (!seasonMemberships.contains(key)) {
                throw invalid(row, "player_id has no matching club membership in season 2026/27");
            }
            List<Integer> clubs = activeClubs.getOrDefault(row.playerId(), List.of());
            if (clubs.size() != 1 || clubs.getFirst() != row.clubId()) {
                throw invalid(row, "player_id must have exactly one active membership in the CSV club on " + asOf);
            }
            Profile incoming = new Profile(row.nationality(), row.birthDate(), row.heightCm(),
                    row.preferredFoot(), row.shirtNumber(), row.fc27Overall());
            Profile current = saved.get(key);
            if (current != null && !current.equals(incoming)) {
                throw invalid(row, "conflicting saved profile; existing data was not overwritten");
            }
            if (current == null) {
                inserts.add(new Object[] {LEAGUE, SEASON, row.playerId(), row.clubId(), row.nationality(),
                        row.birthDate() == null ? null : Date.valueOf(row.birthDate()), row.heightCm(),
                        row.preferredFoot(), row.shirtNumber(), row.fc27Overall()});
            }
        }
        if (!inserts.isEmpty()) {
            jdbc.batchUpdate("""
                    INSERT INTO player_season_profiles
                    (league_id, season_year, player_id, club_id, nationality, birth_date, height_cm,
                     preferred_foot, shirt_number, fc27_overall)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, inserts);
        }
        return new Result(rows.size(), inserts.size());
    }

    private static IllegalArgumentException invalid(PlayerProfileCsvReader.Row row, String message) {
        return new IllegalArgumentException("CSV line " + row.line() + ": " + message
                + " (player_id=" + row.playerId() + ", club_id=" + row.clubId() + ")");
    }

    private record Key(int playerId, int clubId) { }
    private record Profile(String nationality, LocalDate birthDate, Integer heightCm,
                           String preferredFoot, Integer shirtNumber, Integer fc27Overall) { }
    public record Result(int rows, int inserted) { }
}
