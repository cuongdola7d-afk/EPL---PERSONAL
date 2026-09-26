package com.premierhub.sync;

import com.premierhub.service.FootballQueries;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;

@Service
public class FootballDataSync {
    private static final int LEAGUE = FootballQueries.LEAGUE_ID; // Internal PL: provider competition 2021 -> 39.
    private static final int SEASON = FootballDataBatch.SEASON;
    private static final int ID_BASE = 1_000_000_000;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final FootballDataClient client;

    public FootballDataSync(JdbcTemplate jdbc, TransactionTemplate transactions, FootballDataClient client) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.client = client;
    }

    public Result sync() throws IOException, InterruptedException {
        // All HTTP calls and validation finish before opening the write transaction.
        return importBatch(client.download());
    }

    public Result importBatch(FootballDataBatch batch) {
        return transactions.execute(tx -> {
            guardSeason();
            jdbc.update("INSERT INTO seasons (league_id, season_year) SELECT ?, ? WHERE NOT EXISTS "
                    + "(SELECT 1 FROM seasons WHERE league_id=? AND season_year=?)", LEAGUE, SEASON, LEAGUE, SEASON);
            int teamsInserted = 0;
            int fixturesInserted = 0;
            for (var team : batch.teams()) {
                int internal = internalId(team.id());
                List<Integer> mapped = jdbc.queryForList(
                        "SELECT club_id FROM football_data_teams WHERE provider_id=?", Integer.class, team.id());
                if (mapped.isEmpty()) {
                    requireUnused("clubs", internal);
                    jdbc.update("INSERT INTO clubs (id, name, city) VALUES (?, ?, NULL)", internal, team.name());
                    jdbc.update("INSERT INTO football_data_teams (provider_id, club_id) VALUES (?, ?)", team.id(), internal);
                    teamsInserted++;
                } else {
                    requireMapping(mapped.getFirst(), internal);
                    jdbc.update("UPDATE clubs SET name=? WHERE id=?", team.name(), internal);
                }
                jdbc.update("INSERT INTO season_clubs (league_id, season_year, club_id) SELECT ?, ?, ? "
                                + "WHERE NOT EXISTS (SELECT 1 FROM season_clubs WHERE league_id=? AND season_year=? AND club_id=?)",
                        LEAGUE, SEASON, internal, LEAGUE, SEASON, internal);
            }
            for (var fixture : batch.fixtures()) {
                int internal = internalId(fixture.id());
                List<Integer> mapped = jdbc.queryForList(
                        "SELECT fixture_id FROM football_data_fixtures WHERE provider_id=?", Integer.class, fixture.id());
                Date date = Date.valueOf(fixture.kickoff().atOffset(ZoneOffset.UTC).toLocalDate());
                Timestamp now = Timestamp.from(Instant.now());
                if (mapped.isEmpty()) {
                    requireUnused("fixtures", internal);
                    jdbc.update("""
                            INSERT INTO fixtures (id, league_id, season_year, home_club_id, away_club_id,
                              gameweek, match_date, status, provider_status, home_goals, away_goals, payload_hash, synced_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """, internal, LEAGUE, SEASON, internalId(fixture.home()), internalId(fixture.away()),
                            fixture.round(), date, fixture.status(), fixture.status(), fixture.homeGoals(), fixture.awayGoals(),
                            hash(fixture.payload()), now);
                    jdbc.update("INSERT INTO football_data_fixtures (provider_id, fixture_id, kickoff_utc, provider_status) "
                                    + "VALUES (?, ?, ?, ?)", fixture.id(), internal, fixture.kickoff().toString(), fixture.providerStatus());
                    fixturesInserted++;
                } else {
                    requireMapping(mapped.getFirst(), internal);
                    int updated = jdbc.update("""
                            UPDATE fixtures SET home_club_id=?, away_club_id=?, gameweek=?, match_date=?, status=?,
                              provider_status=?, home_goals=?, away_goals=?, payload_hash=?, synced_at=?
                            WHERE id=? AND league_id=? AND season_year=?
                            """, internalId(fixture.home()), internalId(fixture.away()), fixture.round(), date,
                            fixture.status(), fixture.status(), fixture.homeGoals(), fixture.awayGoals(), hash(fixture.payload()),
                            now, internal, LEAGUE, SEASON);
                    if (updated != 1) throw new IllegalStateException("Mapped fixture belongs to a different season");
                    jdbc.update("UPDATE football_data_fixtures SET kickoff_utc=?, provider_status=? WHERE provider_id=?",
                            fixture.kickoff().toString(), fixture.providerStatus(), fixture.id());
                }
            }
            for (var row : batch.standings()) {
                int internal = internalId(row.team());
                boolean exists = jdbc.queryForObject("SELECT COUNT(*) FROM standings WHERE league_id=? AND season_year=? AND club_id=?",
                        Integer.class, LEAGUE, SEASON, internal) != 0;
                if (exists) jdbc.update("""
                        UPDATE standings SET position=?, played=?, won=?, drawn=?, lost=?, goals_for=?,
                          goals_against=?, goal_difference=?, points=? WHERE league_id=? AND season_year=? AND club_id=?
                        """, row.position(), row.played(), row.won(), row.drawn(), row.lost(), row.goalsFor(), row.goalsAgainst(),
                        row.difference(), row.points(), LEAGUE, SEASON, internal);
                else jdbc.update("""
                        INSERT INTO standings (league_id, season_year, club_id, position, played, won, drawn, lost,
                          goals_for, goals_against, goal_difference, points) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, LEAGUE, SEASON, internal, row.position(), row.played(), row.won(), row.drawn(), row.lost(),
                        row.goalsFor(), row.goalsAgainst(), row.difference(), row.points());
            }
            return new Result(teamsInserted, fixturesInserted, batch.teams().size(), batch.fixtures().size(), batch.standings().size());
        });
    }

    private void guardSeason() {
        int foreignTeams = jdbc.queryForObject("""
                SELECT COUNT(*) FROM season_clubs s LEFT JOIN football_data_teams m ON m.club_id=s.club_id
                WHERE s.league_id=? AND s.season_year=? AND m.club_id IS NULL
                """, Integer.class, LEAGUE, SEASON);
        int foreignFixtures = jdbc.queryForObject("""
                SELECT COUNT(*) FROM fixtures f LEFT JOIN football_data_fixtures m ON m.fixture_id=f.id
                WHERE f.league_id=? AND f.season_year=? AND m.fixture_id IS NULL
                """, Integer.class, LEAGUE, SEASON);
        int foreignStandings = jdbc.queryForObject("""
                SELECT COUNT(*) FROM standings s LEFT JOIN football_data_teams m ON m.club_id=s.club_id
                WHERE s.league_id=? AND s.season_year=? AND m.club_id IS NULL
                """, Integer.class, LEAGUE, SEASON);
        if (foreignTeams != 0 || foreignFixtures != 0 || foreignStandings != 0) {
            throw new IllegalStateException("Season 2026 already contains unmapped provider data; import refused");
        }
    }

    private void requireUnused(String table, int id) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id=?", Integer.class, id) != 0) {
            throw new IllegalStateException("Internal ID collision in " + table + ": " + id);
        }
    }

    private static int internalId(int providerId) {
        if (providerId <= 0 || providerId >= ID_BASE) throw new IllegalArgumentException("Invalid provider ID");
        return ID_BASE + providerId;
    }

    private static void requireMapping(int actual, int expected) {
        if (actual != expected) throw new IllegalStateException("Unexpected football-data.org ID mapping");
    }

    private static String hash(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public record Result(int teamsInserted, int fixturesInserted, int teams, int fixtures, int standings) { }
}
