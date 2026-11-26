package com.premierhub.admin;

import com.premierhub.fantasy.GameweekService;
import com.premierhub.service.FantasyLineupService;
import com.premierhub.service.FootballQueries;
import com.premierhub.web.dto.PlayerResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminDataHealthService {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int LEAGUE = FootballQueries.LEAGUE_ID;
    private static final int SEASON = 2026;
    private static final int ITEM_LIMIT = 20;

    public record PlayedWithoutMembershipItem(
            int playerId,
            String playerName,
            String clubName,
            int gameweek,
            String participationStatus,
            Integer minutes,
            LocalDate lastClosedEndDate
    ) {
    }

    public record RosterNotSelectableItem(
            int playerId,
            String playerName,
            String clubName,
            List<String> missing
    ) {
    }

    public record FplUnmappedItem(
            int fplElementId,
            String fplName,
            String clubName,
            int gameweek,
            String fixtureLabel,
            int minutes
    ) {
    }

    public record MultipleOpenMembershipItem(
            int playerId,
            String playerName,
            int openCount,
            List<String> clubs
    ) {
    }

    public record PlayedMissingCoreItem(
            int playerId,
            String playerName,
            String clubName,
            int gameweek,
            String fixtureLabel,
            Integer minutes,
            List<String> missing
    ) {
    }

    public record Check(
            String key,
            long count,
            String status,
            List<?> items
    ) {
        static Check of(String key, long count, List<?> items) {
            return new Check(key, count, count == 0 ? "OK" : "WARN", List.copyOf(items));
        }
    }

    public record DataHealth(
            Instant checkedAtUtc,
            int season,
            LocalDate rosterAsOf,
            Integer openGameweek,
            int warnCount,
            String status,
            List<Check> checks
    ) {
    }

    private final JdbcTemplate jdbc;
    private final FootballQueries footballQueries;
    private final GameweekService gameweekService;
    private final Clock clock;

    @Autowired
    public AdminDataHealthService(
            JdbcTemplate jdbc,
            FootballQueries footballQueries,
            GameweekService gameweekService,
            @Qualifier("fantasyGameweekClock") Clock clock) {
        this.jdbc = jdbc;
        this.footballQueries = footballQueries;
        this.gameweekService = gameweekService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DataHealth check() {
        Instant now = clock.instant();
        GameweekService.Overview gwOverview = gameweekService.overview();
        GameweekService.View openView = gwOverview.gameweeks().stream()
                .filter(v -> v.gameweek() >= 6 && v.gameweek() <= 38)
                .filter(v -> v.configured() && v.status() == GameweekService.Status.OPEN)
                .max(Comparator.comparingInt(GameweekService.View::gameweek))
                .orElse(null);

        LocalDate rosterAsOf = openView != null
                ? openView.rosterAsOf()
                : now.atZone(VIETNAM).toLocalDate();
        Integer openGameweek = openView != null ? openView.gameweek() : null;

        List<Check> checks = List.of(
                checkPlayedWithoutMembership(),
                checkRosterNotSelectable(rosterAsOf),
                checkFplUnmapped(),
                checkMultipleOpenMemberships(),
                checkPlayedMissingCore()
        );
        int warnCount = (int) checks.stream().filter(c -> !"OK".equals(c.status())).count();
        return new DataHealth(
                now,
                SEASON,
                rosterAsOf,
                openGameweek,
                warnCount,
                warnCount == 0 ? "OK" : "WARN",
                checks
        );
    }

    private Check checkPlayedWithoutMembership() {
        List<Integer> recentGws = jdbc.queryForList("""
                SELECT DISTINCT gameweek
                FROM fixtures
                WHERE league_id = ? AND season_year = ? AND status = 'FINISHED'
                ORDER BY gameweek DESC
                LIMIT 2
                """, Integer.class, LEAGUE, SEASON);
        if (recentGws.isEmpty()) {
            return Check.of("playedWithoutMembership", 0L, List.of());
        }

        String placeholders = String.join(",", Collections.nCopies(recentGws.size(), "?"));
        List<Object> params = new ArrayList<>();
        params.add(LEAGUE);
        params.add(SEASON);
        params.addAll(recentGws);

        String sql = """
                SELECT m.player_id,
                       p.name AS player_name,
                       m.club_id,
                       c.name AS club_name,
                       f.gameweek,
                       m.participation_status,
                       m.minutes,
                       (SELECT MAX(cm.end_date)
                        FROM manual_player_memberships cm
                        WHERE cm.league_id = m.league_id
                          AND cm.season_year = m.season_year
                          AND cm.player_id = m.player_id
                          AND cm.club_id = m.club_id
                          AND cm.end_date IS NOT NULL) AS last_closed_end_date
                FROM manual_fixture_player_stats m
                JOIN fixtures f
                  ON f.id = m.fixture_id
                 AND f.league_id = m.league_id
                 AND f.season_year = m.season_year
                JOIN players p ON p.id = m.player_id
                JOIN clubs c ON c.id = m.club_id
                WHERE m.league_id = ?
                  AND m.season_year = ?
                  AND f.status = 'FINISHED'
                  AND f.gameweek IN (%s)
                  AND m.participation_status IN ('PLAYED', 'DID_NOT_PLAY')
                  AND NOT EXISTS (
                      SELECT 1
                      FROM manual_player_memberships om
                      WHERE om.league_id = m.league_id
                        AND om.season_year = m.season_year
                        AND om.player_id = m.player_id
                        AND om.club_id = m.club_id
                        AND om.start_date <= f.match_date
                        AND (om.end_date IS NULL OR om.end_date > f.match_date)
                  )
                ORDER BY f.gameweek DESC, f.match_date DESC, m.fixture_id DESC, p.name ASC, m.player_id ASC
                """.formatted(placeholders);

        record Key(int playerId, int clubId) {
        }
        Map<Key, PlayedWithoutMembershipItem> latestByPair = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            int playerId = rs.getInt("player_id");
            int clubId = rs.getInt("club_id");
            Key key = new Key(playerId, clubId);
            if (!latestByPair.containsKey(key)) {
                Date closedDate = rs.getDate("last_closed_end_date");
                latestByPair.put(key, new PlayedWithoutMembershipItem(
                        playerId,
                        rs.getString("player_name"),
                        rs.getString("club_name"),
                        rs.getInt("gameweek"),
                        rs.getString("participation_status"),
                        rs.getObject("minutes", Integer.class),
                        closedDate == null ? null : closedDate.toLocalDate()
                ));
            }
        }, params.toArray());

        List<PlayedWithoutMembershipItem> all = new ArrayList<>(latestByPair.values());
        List<PlayedWithoutMembershipItem> capped = all.size() > ITEM_LIMIT ? all.subList(0, ITEM_LIMIT) : all;
        return Check.of("playedWithoutMembership", all.size(), capped);
    }

    private Check checkRosterNotSelectable(LocalDate rosterAsOf) {
        List<PlayerResponse> roster = footballQueries.players(SEASON, null, null, rosterAsOf);
        List<RosterNotSelectableItem> issues = new ArrayList<>();
        for (PlayerResponse player : roster) {
            List<String> missing = missingSelectableFields(player);
            if (!missing.isEmpty()) {
                issues.add(new RosterNotSelectableItem(
                        player.id(),
                        player.name(),
                        player.club(),
                        List.copyOf(missing)
                ));
            }
        }
        List<RosterNotSelectableItem> capped = issues.size() > ITEM_LIMIT ? issues.subList(0, ITEM_LIMIT) : issues;
        return Check.of("rosterNotSelectable", issues.size(), capped);
    }

    static List<String> missingSelectableFields(PlayerResponse player) {
        List<String> missing = new ArrayList<>(2);
        if (!FantasyLineupService.hasValidPosition(player)) {
            missing.add("POSITION");
        }
        if (!FantasyLineupService.hasValidOverall(player)) {
            missing.add(player == null || player.fc27Overall() == null ? "OVR" : "OVR_RANGE");
        }
        return missing;
    }

    private Check checkFplUnmapped() {
        long count = queryLong("""
                SELECT COUNT(*)
                FROM fpl_provisional_unmapped u
                JOIN fixtures f ON f.id = u.fixture_id
                WHERE f.league_id = ? AND f.season_year = ?
                """, LEAGUE, SEASON);
        if (count == 0L) {
            return Check.of("fplUnmapped", 0L, List.of());
        }
        List<FplUnmappedItem> items = jdbc.query("""
                SELECT u.fpl_element_id,
                       u.fpl_name,
                       COALESCE(u.club_name, uc.name) AS club_name,
                       f.gameweek,
                       hc.name AS home_club,
                       ac.name AS away_club,
                       u.minutes
                FROM fpl_provisional_unmapped u
                JOIN fixtures f ON f.id = u.fixture_id
                JOIN clubs hc ON hc.id = f.home_club_id
                JOIN clubs ac ON ac.id = f.away_club_id
                LEFT JOIN clubs uc ON uc.id = u.club_id
                WHERE f.league_id = ? AND f.season_year = ?
                ORDER BY f.gameweek DESC, u.fixture_id DESC, u.fpl_name ASC, u.fpl_element_id ASC
                LIMIT ?
                """, (rs, idx) -> new FplUnmappedItem(
                rs.getInt("fpl_element_id"),
                rs.getString("fpl_name"),
                rs.getString("club_name"),
                rs.getInt("gameweek"),
                rs.getString("home_club") + " vs " + rs.getString("away_club"),
                rs.getInt("minutes")
        ), LEAGUE, SEASON, ITEM_LIMIT);
        return Check.of("fplUnmapped", count, items);
    }

    private Check checkMultipleOpenMemberships() {
        long count = queryLong("""
                SELECT COUNT(*)
                FROM (
                    SELECT m.player_id
                    FROM manual_player_memberships m
                    WHERE m.league_id = ? AND m.season_year = ? AND m.end_date IS NULL
                    GROUP BY m.player_id
                    HAVING COUNT(*) >= 2
                ) dup
                """, LEAGUE, SEASON);
        if (count == 0L) {
            return Check.of("multipleOpenMemberships", 0L, List.of());
        }
        record Offender(int playerId, String playerName, int openCount) {
        }
        List<Offender> offenders = jdbc.query("""
                SELECT m.player_id, p.name AS player_name, COUNT(*) AS open_count
                FROM manual_player_memberships m
                JOIN players p ON p.id = m.player_id
                WHERE m.league_id = ? AND m.season_year = ? AND m.end_date IS NULL
                GROUP BY m.player_id, p.name
                HAVING COUNT(*) >= 2
                ORDER BY open_count DESC, p.name ASC, m.player_id ASC
                LIMIT ?
                """, (rs, idx) -> new Offender(
                rs.getInt("player_id"),
                rs.getString("player_name"),
                rs.getInt("open_count")
        ), LEAGUE, SEASON, ITEM_LIMIT);

        String placeholders = String.join(",", Collections.nCopies(offenders.size(), "?"));
        List<Object> params = new ArrayList<>();
        params.add(LEAGUE);
        params.add(SEASON);
        offenders.forEach(o -> params.add(o.playerId()));

        Map<Integer, List<String>> clubsByPlayer = new LinkedHashMap<>();
        jdbc.query("""
                SELECT m.player_id, c.name AS club_name
                FROM manual_player_memberships m
                JOIN clubs c ON c.id = m.club_id
                WHERE m.league_id = ? AND m.season_year = ? AND m.end_date IS NULL
                  AND m.player_id IN (%s)
                ORDER BY m.player_id ASC, m.start_date ASC, c.name ASC
                """.formatted(placeholders), rs -> {
            clubsByPlayer.computeIfAbsent(rs.getInt("player_id"), ignored -> new ArrayList<>())
                    .add(rs.getString("club_name"));
        }, params.toArray());

        List<MultipleOpenMembershipItem> items = offenders.stream()
                .map(o -> new MultipleOpenMembershipItem(
                        o.playerId(),
                        o.playerName(),
                        o.openCount(),
                        List.copyOf(clubsByPlayer.getOrDefault(o.playerId(), List.of()))
                ))
                .collect(Collectors.toList());
        return Check.of("multipleOpenMemberships", count, items);
    }

    private Check checkPlayedMissingCore() {
        long count = queryLong("""
                SELECT COUNT(*)
                FROM manual_fixture_player_stats m
                JOIN fixtures f
                  ON f.id = m.fixture_id
                 AND f.league_id = m.league_id
                 AND f.season_year = m.season_year
                LEFT JOIN fantasy_unrated_confirmations u
                  ON u.fixture_id = m.fixture_id
                 AND u.player_id = m.player_id
                WHERE m.league_id = ?
                  AND m.season_year = ?
                  AND f.gameweek >= 6
                  AND m.participation_status = 'PLAYED'
                  AND (
                      m.minutes IS NULL
                      OR m.goals IS NULL
                      OR m.assists IS NULL
                      OR (m.minutes > 10 AND m.rating IS NULL AND u.fixture_id IS NULL)
                  )
                """, LEAGUE, SEASON);
        if (count == 0L) {
            return Check.of("playedMissingCore", 0L, List.of());
        }
        List<PlayedMissingCoreItem> items = jdbc.query("""
                SELECT m.player_id,
                       p.name AS player_name,
                       c.name AS club_name,
                       f.gameweek,
                       hc.name AS home_club,
                       ac.name AS away_club,
                       m.minutes,
                       m.goals,
                       m.assists,
                       m.rating,
                       CASE WHEN u.fixture_id IS NOT NULL THEN 1 ELSE 0 END AS is_confirmed_unrated
                FROM manual_fixture_player_stats m
                JOIN fixtures f
                  ON f.id = m.fixture_id
                 AND f.league_id = m.league_id
                 AND f.season_year = m.season_year
                JOIN players p ON p.id = m.player_id
                JOIN clubs c ON c.id = m.club_id
                JOIN clubs hc ON hc.id = f.home_club_id
                JOIN clubs ac ON ac.id = f.away_club_id
                LEFT JOIN fantasy_unrated_confirmations u
                  ON u.fixture_id = m.fixture_id
                 AND u.player_id = m.player_id
                WHERE m.league_id = ?
                  AND m.season_year = ?
                  AND f.gameweek >= 6
                  AND m.participation_status = 'PLAYED'
                  AND (
                      m.minutes IS NULL
                      OR m.goals IS NULL
                      OR m.assists IS NULL
                      OR (m.minutes > 10 AND m.rating IS NULL AND u.fixture_id IS NULL)
                  )
                ORDER BY f.gameweek DESC, m.fixture_id DESC, p.name ASC, m.player_id ASC
                LIMIT ?
                """, (rs, idx) -> {
            Integer minutes = rs.getObject("minutes", Integer.class);
            Integer goals = rs.getObject("goals", Integer.class);
            Integer assists = rs.getObject("assists", Integer.class);
            Object rating = rs.getBigDecimal("rating");
            boolean confirmedUnrated = rs.getInt("is_confirmed_unrated") == 1;
            List<String> missing = new ArrayList<>(4);
            if (minutes == null) missing.add("MINUTES");
            if (goals == null) missing.add("GOALS");
            if (assists == null) missing.add("ASSISTS");
            if (minutes != null && minutes > 10 && rating == null && !confirmedUnrated) missing.add("RATING");
            return new PlayedMissingCoreItem(
                    rs.getInt("player_id"),
                    rs.getString("player_name"),
                    rs.getString("club_name"),
                    rs.getInt("gameweek"),
                    rs.getString("home_club") + " vs " + rs.getString("away_club"),
                    minutes,
                    List.copyOf(missing)
            );
        }, LEAGUE, SEASON, ITEM_LIMIT);
        return Check.of("playedMissingCore", count, items);
    }

    private long queryLong(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }
}
