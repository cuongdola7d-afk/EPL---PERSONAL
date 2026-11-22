package com.premierhub.service;

import com.premierhub.web.dto.MatchPlayerStatResponse;
import com.premierhub.web.dto.PlayerMatchResponse;
import com.premierhub.web.dto.PlayerResponse;
import com.premierhub.web.dto.PlayerSeasonProfileResponse;
import com.premierhub.web.dto.PlayerSeasonProfileResponse.CleanSheets;
import com.premierhub.web.dto.PlayerSeasonProfileResponse.PercentileMetric;
import com.premierhub.web.dto.PlayerSeasonProfileResponse.TeamContribution;
import com.premierhub.web.dto.PlayerSeasonProfileResponse.Totals;
import com.premierhub.web.error.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
public class PlayerSeasonProfileService {
    public static final int MIN_PERCENTILE_MINUTES = 450;
    private static final double EPSILON = 1e-9;

    private final JdbcTemplate jdbc;
    private final FootballQueries footballQueries;

    public PlayerSeasonProfileService(JdbcTemplate jdbc, FootballQueries footballQueries) {
        this.jdbc = jdbc;
        this.footballQueries = footballQueries;
    }

    public PlayerSeasonProfileResponse seasonProfile(int playerId, int season) {
        FootballQueries.validateSeason(season);
        if (season == 2026) {
            return seasonProfile2026(playerId);
        }
        return seasonProfileLegacy(playerId, season);
    }

    private PlayerSeasonProfileResponse seasonProfile2026(int playerId) {
        if (!footballQueries.hasPlayerInSeason(playerId, 2026)) {
            throw new ResourceNotFoundException("Player not found: " + playerId);
        }

        String broadPosition = jdbc.query("""
                SELECT ps.position
                FROM player_season_stats ps
                LEFT JOIN manual_player_memberships mem
                  ON mem.league_id = ps.league_id
                 AND mem.season_year = ps.season_year
                 AND mem.player_id = ps.player_id
                 AND mem.club_id = ps.club_id
                WHERE ps.league_id = ? AND ps.season_year = ? AND ps.player_id = ?
                ORDER BY CASE WHEN mem.end_date IS NULL THEN 0 ELSE 1 END,
                         mem.start_date DESC, ps.club_id
                """, (rs, row) -> rs.getString("position"), FootballQueries.LEAGUE_ID, 2026, playerId)
                .stream().findFirst().orElse(null);

        String primaryPosition = jdbc.query("""
                SELECT primary_position
                FROM player_specific_positions
                WHERE league_id = ? AND season_year = ? AND player_id = ?
                """, (rs, row) -> rs.getString("primary_position"), FootballQueries.LEAGUE_ID, 2026, playerId)
                .stream().findFirst().orElse(null);

        String roleGroup = PlayerRoleGroup.roleGroup(primaryPosition, broadPosition);
        String comparisonGroup = PlayerRoleGroup.comparisonGroup(broadPosition, primaryPosition);
        String comparisonGroupLabel = PlayerRoleGroup.comparisonGroupLabel(comparisonGroup);

        ClubContext clubContext = jdbc.query("""
                SELECT COUNT(*) AS finished_matches,
                       COALESCE(SUM(CASE
                           WHEN f.home_goals IS NULL OR f.away_goals IS NULL THEN 0
                           WHEN mem.club_id = f.home_club_id THEN f.home_goals
                           ELSE f.away_goals
                       END), 0) AS team_goals
                FROM fixtures f
                JOIN manual_player_memberships mem
                  ON mem.league_id = f.league_id
                 AND mem.season_year = f.season_year
                 AND mem.club_id IN (f.home_club_id, f.away_club_id)
                 AND mem.start_date <= f.match_date
                 AND (mem.end_date IS NULL OR mem.end_date > f.match_date)
                WHERE f.league_id = ? AND f.season_year = ?
                  AND f.status = 'FINISHED'
                  AND mem.player_id = ?
                """, (rs, row) -> new ClubContext(toInt(rs.getObject("finished_matches")),
                toInt(rs.getObject("team_goals"))), FootballQueries.LEAGUE_ID, 2026, playerId)
                .stream().findFirst().orElse(new ClubContext(0, 0));

        List<PlayedRow> playedRows = jdbc.query("""
                SELECT m.minutes, m.goals, m.assists, m.yellow_cards, m.red_cards, m.rating,
                       e.fixture_id AS extra_fixture_id,
                       e.expected_goals, e.expected_assists, e.tackles,
                       e.clearances_blocks_interceptions, e.recoveries,
                       e.saves, e.goals_conceded, e.clean_sheets, e.bps, e.bonus
                FROM manual_fixture_player_stats m
                JOIN fixtures f
                  ON f.id = m.fixture_id
                 AND f.league_id = m.league_id
                 AND f.season_year = m.season_year
                JOIN manual_player_memberships mem
                  ON mem.league_id = m.league_id
                 AND mem.season_year = m.season_year
                 AND mem.player_id = m.player_id
                 AND mem.club_id = m.club_id
                 AND mem.start_date <= f.match_date
                 AND (mem.end_date IS NULL OR mem.end_date > f.match_date)
                LEFT JOIN manual_fixture_player_extra_stats e
                  ON e.league_id = m.league_id
                 AND e.season_year = m.season_year
                 AND e.fixture_id = m.fixture_id
                 AND e.player_id = m.player_id
                WHERE m.league_id = ? AND m.season_year = ?
                  AND m.player_id = ?
                  AND f.status = 'FINISHED'
                  AND m.participation_status = 'PLAYED'
                  AND m.club_id IN (f.home_club_id, f.away_club_id)
                """, (rs, row) -> new PlayedRow(
                rs.getObject("minutes", Integer.class),
                rs.getObject("goals", Integer.class),
                rs.getObject("assists", Integer.class),
                rs.getObject("yellow_cards", Integer.class),
                rs.getObject("red_cards", Integer.class),
                rs.getBigDecimal("rating"),
                rs.getObject("extra_fixture_id", Integer.class) != null,
                rs.getBigDecimal("expected_goals"),
                rs.getBigDecimal("expected_assists"),
                rs.getObject("tackles", Integer.class),
                rs.getObject("clearances_blocks_interceptions", Integer.class),
                rs.getObject("recoveries", Integer.class),
                rs.getObject("saves", Integer.class),
                rs.getObject("goals_conceded", Integer.class),
                rs.getObject("clean_sheets", Integer.class),
                rs.getObject("bps", Integer.class),
                rs.getObject("bonus", Integer.class)
        ), FootballQueries.LEAGUE_ID, 2026, playerId);

        Totals totals = aggregateTotals(playedRows, clubContext.finishedMatches());

        boolean isAttackOrMixed = PlayerRoleGroup.ATTACK.equals(roleGroup) || PlayerRoleGroup.MIXED.equals(roleGroup);
        TeamContribution teamContribution = isAttackOrMixed
                ? new TeamContribution(clubContext.teamGoals(), totals.goals(), totals.assists())
                : null;
        CleanSheets cleanSheets = !isAttackOrMixed
                ? new CleanSheets(totals.cleanSheets(), totals.appearances())
                : null;

        List<PercentileMetric> percentiles = null;
        if (totals.minutes() != null && totals.minutes() >= MIN_PERCENTILE_MINUTES) {
            percentiles = computePercentiles(playerId, roleGroup, comparisonGroup, totals);
        }

        return new PlayerSeasonProfileResponse(
                playerId,
                2026,
                roleGroup,
                comparisonGroup,
                comparisonGroupLabel,
                totals,
                teamContribution,
                cleanSheets,
                percentiles
        );
    }

    private PlayerSeasonProfileResponse seasonProfileLegacy(int playerId, int season) {
        PlayerResponse player = footballQueries.player(playerId, season)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + playerId));
        String roleGroup = PlayerRoleGroup.roleGroup(player.primaryPosition(), player.position());
        String comparisonGroup = PlayerRoleGroup.comparisonGroup(player.position(), player.primaryPosition());
        String comparisonGroupLabel = PlayerRoleGroup.comparisonGroupLabel(comparisonGroup);

        List<PlayerMatchResponse> matches = footballQueries.playerMatches(playerId, season);
        List<PlayerMatchResponse> finished = matches.stream()
                .filter(row -> "FINISHED".equals(row.match().status()))
                .toList();

        List<PlayedRow> playedRows = new ArrayList<>();
        for (PlayerMatchResponse row : finished) {
            if (!isLegacyPlayed(row)) {
                continue;
            }
            Integer minutes = effectiveLegacyStat(row, MatchPlayerStatResponse::minutes, "minutes");
            Integer goals = effectiveLegacyStat(row, MatchPlayerStatResponse::goals, "goals");
            Integer assists = effectiveLegacyStat(row, MatchPlayerStatResponse::assists, "assists");
            Integer yellowCards = row.stats().yellowCards();
            Integer redCards = row.stats().redCards();
            BigDecimal rating = parseRating(row.stats().rating());
            playedRows.add(new PlayedRow(minutes, goals, assists, yellowCards, redCards, rating,
                    false, null, null, null, null, null, null, null, null, null, null));
        }

        Totals basic = aggregateTotals(playedRows, finished.size());
        Totals legacyTotals = new Totals(
                basic.appearances(),
                basic.clubFinishedMatches(),
                basic.minutes(),
                basic.goals(),
                basic.assists(),
                basic.yellowCards(),
                basic.redCards(),
                basic.averageRating(),
                basic.ratedMatches(),
                null, null, null, null, null, null, null, null, null, null
        );

        return new PlayerSeasonProfileResponse(
                playerId,
                season,
                roleGroup,
                comparisonGroup,
                comparisonGroupLabel,
                legacyTotals,
                null,
                null,
                null
        );
    }

    private Totals aggregateTotals(List<PlayedRow> played, int clubFinishedMatches) {
        int appearances = played.size();
        Integer minutes = sumInt(played, PlayedRow::minutes);
        Integer goals = sumInt(played, PlayedRow::goals);
        Integer assists = sumInt(played, PlayedRow::assists);
        Integer yellowCards = sumInt(played, PlayedRow::yellowCards);
        Integer redCards = sumInt(played, PlayedRow::redCards);

        int ratedMatches = 0;
        double ratingSum = 0.0;
        for (PlayedRow row : played) {
            if (row.rating() != null) {
                ratedMatches++;
                ratingSum += row.rating().doubleValue();
            }
        }
        Double averageRating = ratedMatches == 0 ? null : round(ratingSum / ratedMatches, 2);

        BigDecimal expectedGoals = sumExtraDecimal(played, PlayedRow::expectedGoals);
        BigDecimal expectedAssists = sumExtraDecimal(played, PlayedRow::expectedAssists);
        Integer tackles = sumExtraInt(played, PlayedRow::tackles);
        Integer cbi = sumExtraInt(played, PlayedRow::cbi);
        Integer recoveries = sumExtraInt(played, PlayedRow::recoveries);
        Integer saves = sumExtraInt(played, PlayedRow::saves);
        Integer goalsConceded = sumExtraInt(played, PlayedRow::goalsConceded);
        Integer cleanSheets = sumExtraInt(played, PlayedRow::cleanSheets);
        Integer bps = sumExtraInt(played, PlayedRow::bps);
        Integer bonus = sumExtraInt(played, PlayedRow::bonus);

        return new Totals(
                appearances,
                clubFinishedMatches,
                minutes,
                goals,
                assists,
                yellowCards,
                redCards,
                averageRating,
                ratedMatches,
                expectedGoals,
                expectedAssists,
                tackles,
                cbi,
                recoveries,
                saves,
                goalsConceded,
                cleanSheets,
                bps,
                bonus
        );
    }

    private List<PercentileMetric> computePercentiles(int playerId, String roleGroup,
                                                      String comparisonGroup, Totals playerTotals) {
        String broadEnum = PlayerRoleGroup.broadPositionEnum(comparisonGroup);
        List<PoolEntry> pool = jdbc.query("""
                SELECT m.player_id,
                       COUNT(*) AS appearances,
                       SUM(m.minutes) AS minutes,
                       SUM(CASE WHEN m.goals IS NULL THEN 1 ELSE 0 END) AS missing_goals,
                       SUM(m.goals) AS goals,
                       SUM(CASE WHEN m.assists IS NULL THEN 1 ELSE 0 END) AS missing_assists,
                       SUM(m.assists) AS assists,
                       SUM(CASE WHEN e.fixture_id IS NULL OR e.expected_goals IS NULL THEN 1 ELSE 0 END) AS missing_xg,
                       SUM(e.expected_goals) AS expected_goals,
                       SUM(CASE WHEN e.fixture_id IS NULL OR e.expected_assists IS NULL THEN 1 ELSE 0 END) AS missing_xa,
                       SUM(e.expected_assists) AS expected_assists,
                       SUM(CASE WHEN e.fixture_id IS NULL OR e.tackles IS NULL THEN 1 ELSE 0 END) AS missing_tackles,
                       SUM(e.tackles) AS tackles,
                       SUM(CASE WHEN e.fixture_id IS NULL OR e.clearances_blocks_interceptions IS NULL THEN 1 ELSE 0 END) AS missing_cbi,
                       SUM(e.clearances_blocks_interceptions) AS cbi,
                       SUM(CASE WHEN e.fixture_id IS NULL OR e.recoveries IS NULL THEN 1 ELSE 0 END) AS missing_recoveries,
                       SUM(e.recoveries) AS recoveries,
                       SUM(CASE WHEN e.fixture_id IS NULL OR e.saves IS NULL THEN 1 ELSE 0 END) AS missing_saves,
                       SUM(e.saves) AS saves,
                       SUM(CASE WHEN e.fixture_id IS NULL OR e.goals_conceded IS NULL THEN 1 ELSE 0 END) AS missing_gc,
                       SUM(e.goals_conceded) AS goals_conceded,
                       SUM(CASE WHEN e.fixture_id IS NULL OR e.clean_sheets IS NULL THEN 1 ELSE 0 END) AS missing_cs,
                       SUM(e.clean_sheets) AS clean_sheets,
                       SUM(CASE WHEN e.fixture_id IS NULL OR e.bps IS NULL THEN 1 ELSE 0 END) AS missing_bps,
                       SUM(e.bps) AS bps
                FROM manual_fixture_player_stats m
                JOIN fixtures f
                  ON f.id = m.fixture_id
                 AND f.league_id = m.league_id
                 AND f.season_year = m.season_year
                JOIN manual_player_memberships mem
                  ON mem.league_id = m.league_id
                 AND mem.season_year = m.season_year
                 AND mem.player_id = m.player_id
                 AND mem.club_id = m.club_id
                 AND mem.start_date <= f.match_date
                 AND (mem.end_date IS NULL OR mem.end_date > f.match_date)
                JOIN player_season_stats ps
                  ON ps.league_id = m.league_id
                 AND ps.season_year = m.season_year
                 AND ps.player_id = m.player_id
                 AND ps.club_id = m.club_id
                LEFT JOIN manual_fixture_player_extra_stats e
                  ON e.league_id = m.league_id
                 AND e.season_year = m.season_year
                 AND e.fixture_id = m.fixture_id
                 AND e.player_id = m.player_id
                WHERE m.league_id = ? AND m.season_year = ?
                  AND f.status = 'FINISHED'
                  AND m.participation_status = 'PLAYED'
                  AND m.club_id IN (f.home_club_id, f.away_club_id)
                  AND ps.position = ?
                GROUP BY m.player_id
                HAVING SUM(CASE WHEN m.minutes IS NULL THEN 1 ELSE 0 END) = 0
                   AND SUM(m.minutes) >= ?
                """, (rs, row) -> readPoolEntry(rs), FootballQueries.LEAGUE_ID, 2026, broadEnum, MIN_PERCENTILE_MINUTES);

        PoolEntry self = new PoolEntry(
                playerId,
                playerTotals.appearances(),
                playerTotals.minutes(),
                playerTotals.goals() == null ? null : playerTotals.goals().doubleValue(),
                playerTotals.assists() == null ? null : playerTotals.assists().doubleValue(),
                playerTotals.expectedGoals() == null ? null : playerTotals.expectedGoals().doubleValue(),
                playerTotals.expectedAssists() == null ? null : playerTotals.expectedAssists().doubleValue(),
                playerTotals.tackles() == null ? null : playerTotals.tackles().doubleValue(),
                playerTotals.clearancesBlocksInterceptions() == null ? null : playerTotals.clearancesBlocksInterceptions().doubleValue(),
                playerTotals.recoveries() == null ? null : playerTotals.recoveries().doubleValue(),
                playerTotals.saves() == null ? null : playerTotals.saves().doubleValue(),
                playerTotals.goalsConceded() == null ? null : playerTotals.goalsConceded().doubleValue(),
                playerTotals.cleanSheets() == null ? null : playerTotals.cleanSheets().doubleValue(),
                playerTotals.bps() == null ? null : playerTotals.bps().doubleValue()
        );

        List<MetricSpec> specs = metricSpecsForRole(roleGroup);
        List<PercentileMetric> results = new ArrayList<>(specs.size());
        for (MetricSpec spec : specs) {
            results.add(evaluateMetric(spec, self, pool));
        }
        return results;
    }

    private static List<MetricSpec> metricSpecsForRole(String roleGroup) {
        return switch (roleGroup) {
            case PlayerRoleGroup.DEFEND -> List.of(
                    MetricSpec.per90("tacklesPer90", PoolEntry::tackles, false, 2),
                    MetricSpec.per90("cbiPer90", PoolEntry::cbi, false, 2),
                    MetricSpec.per90("recoveriesPer90", PoolEntry::recoveries, false, 2),
                    MetricSpec.per90("defendingPer90", PoolEntry::defendingTotal, false, 2),
                    MetricSpec.per90("assistsPer90", PoolEntry::assists, false, 2),
                    MetricSpec.per90("xaPer90", PoolEntry::expectedAssists, false, 2),
                    MetricSpec.per90("bpsPer90", PoolEntry::bps, false, 1)
            );
            case PlayerRoleGroup.GK -> List.of(
                    MetricSpec.per90("savesPer90", PoolEntry::saves, false, 2),
                    MetricSpec.per90("goalsConcededPer90", PoolEntry::goalsConceded, true, 2),
                    new MetricSpec("cleanSheetRate", entry ->
                            entry.cleanSheets() == null || entry.appearances() <= 0
                                    ? null : entry.cleanSheets() / entry.appearances(), false, 4),
                    MetricSpec.per90("recoveriesPer90", PoolEntry::recoveries, false, 2),
                    MetricSpec.per90("bpsPer90", PoolEntry::bps, false, 1)
            );
            default -> List.of(
                    MetricSpec.per90("goalsPer90", PoolEntry::goals, false, 2),
                    MetricSpec.per90("xgPer90", PoolEntry::expectedGoals, false, 2),
                    MetricSpec.per90("assistsPer90", PoolEntry::assists, false, 2),
                    MetricSpec.per90("xaPer90", PoolEntry::expectedAssists, false, 2),
                    MetricSpec.per90("tacklesPer90", PoolEntry::tackles, false, 2),
                    MetricSpec.per90("defendingPer90", PoolEntry::defendingTotal, false, 2),
                    MetricSpec.per90("bpsPer90", PoolEntry::bps, false, 1)
            );
        };
    }

    private static PercentileMetric evaluateMetric(MetricSpec spec, PoolEntry self, List<PoolEntry> pool) {
        Double selfVal = spec.extractor().apply(self);
        int poolSize = 0;
        int othersCount = 0;
        int betterThan = 0;
        for (PoolEntry candidate : pool) {
            Double value = spec.extractor().apply(candidate);
            if (value == null) {
                continue;
            }
            poolSize++;
            if (candidate.playerId() == self.playerId()) {
                continue;
            }
            othersCount++;
            if (selfVal != null) {
                if (spec.lowerIsBetter()) {
                    if (value - selfVal > EPSILON) {
                        betterThan++;
                    }
                } else {
                    if (selfVal - value > EPSILON) {
                        betterThan++;
                    }
                }
            }
        }
        if (selfVal == null) {
            return new PercentileMetric(spec.key(), null, null, poolSize);
        }
        Integer percentile = othersCount == 0
                ? null
                : (int) Math.round(100.0 * betterThan / othersCount);
        return new PercentileMetric(spec.key(), round(selfVal, spec.scale()), percentile, poolSize);
    }

    private static PoolEntry readPoolEntry(ResultSet rs) throws SQLException {
        return new PoolEntry(
                rs.getInt("player_id"),
                toInt(rs.getObject("appearances")),
                toInt(rs.getObject("minutes")),
                nullableDouble(rs, "goals", "missing_goals"),
                nullableDouble(rs, "assists", "missing_assists"),
                nullableDouble(rs, "expected_goals", "missing_xg"),
                nullableDouble(rs, "expected_assists", "missing_xa"),
                nullableDouble(rs, "tackles", "missing_tackles"),
                nullableDouble(rs, "cbi", "missing_cbi"),
                nullableDouble(rs, "recoveries", "missing_recoveries"),
                nullableDouble(rs, "saves", "missing_saves"),
                nullableDouble(rs, "goals_conceded", "missing_gc"),
                nullableDouble(rs, "clean_sheets", "missing_cs"),
                nullableDouble(rs, "bps", "missing_bps")
        );
    }

    private static Double nullableDouble(ResultSet rs, String sumCol, String missingCol) throws SQLException {
        if (toInt(rs.getObject(missingCol)) > 0) {
            return null;
        }
        Object raw = rs.getObject(sumCol);
        return raw == null ? null : ((Number) raw).doubleValue();
    }

    private static Integer sumInt(List<PlayedRow> rows, Function<PlayedRow, Integer> getter) {
        int total = 0;
        for (PlayedRow row : rows) {
            Integer v = getter.apply(row);
            if (v == null) {
                return null;
            }
            total += v;
        }
        return total;
    }

    private static Integer sumExtraInt(List<PlayedRow> rows, Function<PlayedRow, Integer> getter) {
        int total = 0;
        for (PlayedRow row : rows) {
            if (!row.hasExtra()) {
                return null;
            }
            Integer v = getter.apply(row);
            if (v == null) {
                return null;
            }
            total += v;
        }
        return total;
    }

    private static BigDecimal sumExtraDecimal(List<PlayedRow> rows, Function<PlayedRow, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (PlayedRow row : rows) {
            if (!row.hasExtra()) {
                return null;
            }
            BigDecimal v = getter.apply(row);
            if (v == null) {
                return null;
            }
            total = total.add(v);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean isLegacyPlayed(PlayerMatchResponse row) {
        MatchPlayerStatResponse stats = row.stats();
        if (stats == null) {
            return false;
        }
        if ("PLAYED".equals(stats.participationStatus())) {
            return true;
        }
        if ("DID_NOT_PLAY".equals(stats.participationStatus())) {
            return false;
        }
        Integer minutes = effectiveLegacyStat(row, MatchPlayerStatResponse::minutes, "minutes");
        return minutes != null && minutes > 0;
    }

    private static Integer effectiveLegacyStat(PlayerMatchResponse row,
                                               Function<MatchPlayerStatResponse, Integer> getter,
                                               String field) {
        MatchPlayerStatResponse stats = row.stats();
        if (stats == null) {
            return null;
        }
        Integer raw = getter.apply(stats);
        if (raw != null) {
            return raw;
        }
        if ("VERIFIED".equals(row.evidenceStatus()) && stats.inferred() != null) {
            return switch (field) {
                case "minutes" -> stats.inferred().minutes();
                case "goals" -> stats.inferred().goals();
                case "assists" -> stats.inferred().assists();
                default -> null;
            };
        }
        return null;
    }

    private static BigDecimal parseRating(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(raw.strip());
            return parsed.compareTo(BigDecimal.ZERO) >= 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private record ClubContext(int finishedMatches, int teamGoals) {
    }

    private record PlayedRow(
            Integer minutes,
            Integer goals,
            Integer assists,
            Integer yellowCards,
            Integer redCards,
            BigDecimal rating,
            boolean hasExtra,
            BigDecimal expectedGoals,
            BigDecimal expectedAssists,
            Integer tackles,
            Integer cbi,
            Integer recoveries,
            Integer saves,
            Integer goalsConceded,
            Integer cleanSheets,
            Integer bps,
            Integer bonus
    ) {
    }

    private record PoolEntry(
            int playerId,
            int appearances,
            int minutes,
            Double goals,
            Double assists,
            Double expectedGoals,
            Double expectedAssists,
            Double tackles,
            Double cbi,
            Double recoveries,
            Double saves,
            Double goalsConceded,
            Double cleanSheets,
            Double bps
    ) {
        Double defendingTotal() {
            if (tackles == null || cbi == null || recoveries == null) {
                return null;
            }
            return tackles + cbi + recoveries;
        }
    }

    private record MetricSpec(String key, Function<PoolEntry, Double> extractor, boolean lowerIsBetter, int scale) {
        static MetricSpec per90(String key, Function<PoolEntry, Double> totalExtractor, boolean lowerIsBetter, int scale) {
            return new MetricSpec(key, entry -> {
                Double total = totalExtractor.apply(entry);
                return total == null || entry.minutes() <= 0 ? null : (total * 90.0) / entry.minutes();
            }, lowerIsBetter, scale);
        }
    }
}
