package com.premierhub.lineups;

import com.premierhub.web.dto.MatchLineupResponse;
import com.premierhub.web.dto.MatchPlayerStatResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.BadSqlGrammarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Read-only metadata; never uses minutes, ratings or eligibility to identify starters. */
public final class MatchLineupQueries {
    public record ProvisionalPlayerInput(int playerId, int minutes, int starts, String broadPosition) {
    }
    private static final Logger LOG = LoggerFactory.getLogger(MatchLineupQueries.class);
    private static final BundledMatchLineups BUNDLED = new BundledMatchLineups();
    private final JdbcTemplate jdbc;
    private final Set<String> reportedSchemaGaps = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public MatchLineupQueries(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public MatchLineupResponse read(int fixtureId, int clubId, List<MatchPlayerStatResponse> stats) {
        var fixture = optionalQuery("""
                SELECT formation,formation_verified_on,formation_source,roles_verified_on,roles_source
                FROM fixture_lineups WHERE league_id=39 AND season_year=2026 AND fixture_id=? AND club_id=?
                """, (rs, index) -> new Fixture(rs.getString(1), date(rs.getDate(2)), rs.getString(3),
                        date(rs.getDate(4)), rs.getString(5)), fixtureId, clubId).stream().findFirst().orElse(null);
        var defaults = optionalQuery("""
                SELECT default_formation,updated_on,scope_from_gw,scope_to_gw,verified_matches,formation_counts,fixture_ids,source_note
                FROM club_season_formations WHERE league_id=39 AND season_year=2026 AND club_id=?
                """, (rs, index) -> new Default(rs.getString(1), date(rs.getDate(2)), rs.getInt(3), rs.getInt(4),
                        rs.getInt(5), rs.getString(6), rs.getString(7), rs.getString(8)), clubId).stream().findFirst().orElse(null);
        if (defaults == null) defaults = optionalQuery("""
                SELECT default_formation,updated_on,scope_from_gw,scope_to_gw,verified_matches,formation_counts,fixture_ids
                FROM club_season_formations WHERE league_id=39 AND season_year=2026 AND club_id=?
                """, (rs, index) -> new Default(rs.getString(1), date(rs.getDate(2)), rs.getInt(3), rs.getInt(4),
                        rs.getInt(5), rs.getString(6), rs.getString(7), null), clubId).stream().findFirst().orElse(null);
        if (defaults == null || defaults.formation() == null) {
            var bundled = BUNDLED.clubDefault(clubId);
            if (bundled != null) defaults = new Default(bundled.formation(), bundled.updatedOn(), bundled.from(),
                    bundled.to(), bundled.count(), bundled.frequencies(), bundled.ids(), bundled.sourceNote());
        }
        List<MatchLineupResponse.Player> players = optionalQuery("""
                SELECT l.player_id,l.role,l.match_position,l.row_index,l.slot_index,
                    l.substitution_in_minute,l.substitution_out_minute,p.primary_position
                FROM fixture_lineup_players l
                LEFT JOIN player_specific_positions p ON p.player_id=l.player_id
                    AND p.league_id=39 AND p.season_year=2026
                WHERE l.fixture_id=? AND l.club_id=? ORDER BY l.player_id
                """, (rs, index) -> new MatchLineupResponse.Player(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getObject(4, Integer.class), rs.getObject(5, Integer.class),
                        rs.getObject(6, Integer.class), rs.getObject(7, Integer.class), rs.getString(8)), fixtureId, clubId);
        LocalDate rolesDate = fixture == null ? null : fixture.rolesDate();
        String rolesSource = fixture == null ? null : fixture.rolesSource();
        if (players.isEmpty()) {
            var bundled = BUNDLED.team(fixtureId, clubId);
            if (bundled != null) {
                var positions = optionalQuery("SELECT player_id,primary_position FROM player_specific_positions "
                        + "WHERE league_id=39 AND season_year=2026 AND primary_position IS NOT NULL", (rs, index) -> new SeasonPosition(rs.getInt(1), rs.getString(2)))
                        .stream().collect(Collectors.toMap(SeasonPosition::playerId, SeasonPosition::position));
                players = bundled.players().stream().map(player -> new MatchLineupResponse.Player(player.playerId(),
                        player.role(), null, null, null, null, null, positions.get(player.playerId()))).toList();
                rolesDate = bundled.verifiedOn();
                rolesSource = bundled.sourceNote();
            }
        }
        if (!players.isEmpty()) {
            var eligibility = optionalQuery("SELECT player_id,position_code FROM player_eligible_positions "
                    + "WHERE league_id=39 AND season_year=2026 ORDER BY player_id,position_code",
                    (rs, index) -> new SeasonPosition(rs.getInt(1), rs.getString(2))).stream()
                    .collect(Collectors.groupingBy(SeasonPosition::playerId,
                            Collectors.mapping(SeasonPosition::position, Collectors.toList())));
            players = players.stream().map(player -> new MatchLineupResponse.Player(player.playerId(), player.role(),
                    player.matchPosition(), player.rowIndex(), player.slotIndex(), player.substitutionInMinute(),
                    player.substitutionOutMinute(), player.seasonPosition(),
                    List.copyOf(eligibility.getOrDefault(player.playerId(), List.of())))).toList();
        }
        Set<Integer> sourceIds = players.stream().map(MatchLineupResponse.Player::playerId).collect(Collectors.toSet());
        Set<Integer> statIds = stats.stream().map(MatchPlayerStatResponse::playerId).collect(Collectors.toSet());
        var byPlayer = stats.stream().collect(Collectors.toMap(MatchPlayerStatResponse::playerId, player -> player, (first, second) -> first));
        boolean verified = rolesDate != null && rolesSource != null && !rolesSource.isBlank()
                && statIds.size() == stats.size() && sourceIds.size() == players.size()
                && sourceIds.equals(statIds) && players.stream().filter(player -> player.role().equals("STARTER")).count() == 11
                && players.stream().allMatch(player -> Set.of("STARTER", "SUB_USED", "SUB_UNUSED").contains(player.role())
                    && (player.role().equals("SUB_UNUSED") ? "DID_NOT_PLAY" : "PLAYED")
                        .equals(byPlayer.get(player.playerId()).participationStatus()));
        boolean actual = fixture != null && fixture.formation() != null && fixture.formationDate() != null && fixture.formationSource() != null;
        String formation = actual ? fixture.formation() : defaults == null ? null : defaults.formation();
        if (formation != null) Formation.lines(formation);
        return new MatchLineupResponse(formation, formation == null ? "MISSING" : actual ? "FIXTURE" : "CLUB_DEFAULT",
                actual ? fixture.formationDate() : defaults == null ? null : defaults.updatedOn(),
                actual ? fixture.formationSource() : defaults == null ? null : defaults.sourceNote(),
                defaults == null ? null : defaults.from(), defaults == null ? null : defaults.to(),
                defaults == null ? null : defaults.count(), defaults == null ? null : defaults.frequencies(),
                defaults == null ? null : defaults.ids(), verified ? "VERIFIED" : players.isEmpty() ? "MISSING" : "INCOMPLETE",
                rolesDate, rolesSource, players);
    }

    /**
     * Builds provisional lineup metadata from {@code fpl_provisional_player_stats} rows (minutes &gt; 0 only).
     * Never writes to any official lineup or manual table.
     */
    public MatchLineupResponse buildProvisional(int clubId, List<ProvisionalPlayerInput> provisionalRows) {
        List<ProvisionalPlayerInput> played = provisionalRows == null
                ? List.of()
                : provisionalRows.stream().filter(r -> r.minutes() > 0).toList();

        Map<Integer, String> primaryPositions = optionalQuery(
                "SELECT player_id, primary_position FROM player_specific_positions "
                        + "WHERE league_id=39 AND season_year=2026 AND primary_position IS NOT NULL",
                (rs, idx) -> new SeasonPosition(rs.getInt(1), rs.getString(2)))
                .stream()
                .collect(Collectors.toMap(SeasonPosition::playerId, SeasonPosition::position, (a, b) -> a));

        Map<Integer, List<String>> eligibility = optionalQuery(
                "SELECT player_id, position_code FROM player_eligible_positions "
                        + "WHERE league_id=39 AND season_year=2026 ORDER BY player_id, position_code",
                (rs, idx) -> new SeasonPosition(rs.getInt(1), rs.getString(2)))
                .stream()
                .collect(Collectors.groupingBy(SeasonPosition::playerId,
                        Collectors.mapping(SeasonPosition::position, Collectors.toList())));

        List<MatchLineupResponse.Player> players = new ArrayList<>(played.size());
        List<ProvisionalFormationResolver.StarterSpec> starterSpecs = new ArrayList<>();
        for (ProvisionalPlayerInput row : played) {
            boolean starter = row.starts() == 1;
            String role = starter ? "STARTER" : "SUB_USED";
            String primary = primaryPositions.get(row.playerId());
            List<String> eligible = List.copyOf(eligibility.getOrDefault(row.playerId(), List.of()));
            players.add(new MatchLineupResponse.Player(
                    row.playerId(), role, null, null, null, null, null, primary, eligible));
            if (starter) {
                starterSpecs.add(new ProvisionalFormationResolver.StarterSpec(
                        row.playerId(), primary, eligible, row.broadPosition()));
            }
        }

        String startersStatus = starterSpecs.size() == 11 ? "PROVISIONAL" : "PENDING_FPL";
        String clubUsualFormation = findClubUsualFormation(clubId);
        String resolvedFormation = "PROVISIONAL".equals(startersStatus)
                ? ProvisionalFormationResolver.resolveFormation(clubUsualFormation, starterSpecs)
                : clubUsualFormation;
        String formationSource = resolvedFormation != null ? "CLUB_USUAL" : "MISSING";

        return new MatchLineupResponse(
                resolvedFormation,
                formationSource,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                startersStatus,
                null,
                null,
                List.copyOf(players)
        );
    }

    private String findClubUsualFormation(int clubId) {
        record ObservedFormation(int fixtureId, int gameweek, LocalDate matchDate, String formation) { }
        List<ObservedFormation> rows = optionalQuery("""
                SELECT fl.fixture_id, f.gameweek, f.match_date, fl.formation
                FROM fixture_lineups fl
                JOIN fixtures f ON f.id = fl.fixture_id
                  AND f.league_id = fl.league_id
                  AND f.season_year = fl.season_year
                WHERE fl.league_id = 39
                  AND fl.season_year = 2026
                  AND fl.club_id = ?
                  AND fl.formation IS NOT NULL
                ORDER BY f.match_date ASC, f.gameweek ASC, fl.fixture_id ASC
                """, (rs, idx) -> new ObservedFormation(
                rs.getInt("fixture_id"),
                rs.getInt("gameweek"),
                date(rs.getDate("match_date")) == null ? LocalDate.MIN : date(rs.getDate("match_date")),
                rs.getString("formation").strip()
        ), clubId);

        if (!rows.isEmpty()) {
            Map<String, Integer> counts = new HashMap<>();
            Map<String, ObservedFormation> latest = new HashMap<>();
            Comparator<ObservedFormation> matchOrder = Comparator
                    .comparing(ObservedFormation::matchDate)
                    .thenComparingInt(ObservedFormation::gameweek)
                    .thenComparingInt(ObservedFormation::fixtureId);
            for (ObservedFormation row : rows) {
                counts.merge(row.formation(), 1, Integer::sum);
                latest.merge(row.formation(), row, (a, b) -> matchOrder.compare(a, b) >= 0 ? a : b);
            }
            return counts.keySet().stream()
                    .min(Comparator
                            .comparingInt((String fm) -> counts.get(fm)).reversed()
                            .thenComparing(latest::get, matchOrder.reversed())
                            .thenComparing(Comparator.naturalOrder()))
                    .orElse(null);
        }

        List<String> fallbackDb = optionalQuery(
                "SELECT default_formation FROM club_season_formations "
                        + "WHERE league_id=39 AND season_year=2026 AND club_id=? AND default_formation IS NOT NULL",
                (rs, idx) -> rs.getString(1), clubId);
        if (!fallbackDb.isEmpty()) {
            return fallbackDb.getFirst().strip();
        }
        var bundled = BUNDLED.clubDefault(clubId);
        return bundled == null ? null : bundled.formation();
    }

    private <T> List<T> optionalQuery(String sql, RowMapper<T> mapper, Object... arguments) {
        try {
            return jdbc.query(sql, mapper, arguments);
        } catch (BadSqlGrammarException failure) {
            var cause = failure.getSQLException();
            // Only absent optional metadata tables/columns are recoverable; other SQL failures stay visible.
            if (cause.getSQLState() == null || !Set.of("42S02", "42S04", "42S22").contains(cause.getSQLState())) throw failure;
            if (reportedSchemaGaps.add(sql)) LOG.warn("Optional match-lineup schema is incomplete (SQLState {}). Using available SQL and packaged evidence; no database writes performed.", cause.getSQLState());
            return List.of();
        }
    }

    private static LocalDate date(java.sql.Date value) { return value == null ? null : value.toLocalDate(); }
    private record Fixture(String formation, LocalDate formationDate, String formationSource,
                           LocalDate rolesDate, String rolesSource) { }
    private record Default(String formation, LocalDate updatedOn, int from, int to, int count,
                           String frequencies, String ids, String sourceNote) { }
    private record SeasonPosition(int playerId, String position) { }
}
