package com.premierhub.service;

import com.premierhub.lineups.Formation;
import com.premierhub.web.dto.ClubOverviewResponse;
import com.premierhub.web.dto.ClubOverviewResponse.FormationCount;
import com.premierhub.web.dto.ClubOverviewResponse.LeaderEntry;
import com.premierhub.web.dto.ClubOverviewResponse.RatingLeaderEntry;
import com.premierhub.web.dto.ClubOverviewResponse.XiPlayer;
import com.premierhub.web.dto.ClubStatisticsResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClubOverviewService {
    private static final int LEAGUE_ID = FootballQueries.LEAGUE_ID;
    private static final int TOP_LIMIT = 3;
    private static final int MIN_RATED_APPEARANCES = 5;

    private static final Set<String> SURNAME_PARTICLES = Set.of(
            "van", "von", "de", "del", "della", "di", "da", "do", "dos", "das",
            "el", "al", "le", "la", "les", "den", "der", "ten", "ter", "st", "san", "ben", "mac", "mc"
    );

    private static final Map<String, List<List<String>>> FORMATION_LABELS = Map.of(
            "4-2-3-1", List.of(List.of("GK"), List.of("LB", "CB", "CB", "RB"), List.of("CM", "CM"), List.of("LW", "CAM", "RW"), List.of("ST")),
            "5-4-1", List.of(List.of("GK"), List.of("LB", "CB", "CB", "CB", "RB"), List.of("LM", "CM", "CM", "RM"), List.of("ST")),
            "3-4-3", List.of(List.of("GK"), List.of("CB", "CB", "CB"), List.of("LM", "CM", "CM", "RM"), List.of("LW", "ST", "RW"))
    );

    private final JdbcTemplate jdbc;
    private final ClubStatisticsService clubStatisticsService;

    public ClubOverviewService(JdbcTemplate jdbc, ClubStatisticsService clubStatisticsService) {
        this.jdbc = jdbc;
        this.clubStatisticsService = clubStatisticsService;
    }

    @Transactional(readOnly = true)
    public ClubOverviewResponse overview(int clubId, int season) {
        FootballQueries.validateSeason(season);

        ClubStatsBundle statsBundle = loadClubStats(clubId, season);
        Map<Integer, Double> avgRatingByPlayer = statsBundle.players().stream()
                .filter(p -> p.averageRating() != null)
                .collect(Collectors.toMap(PlayerStatRow::playerId, PlayerStatRow::averageRating, (a, b) -> a));

        LineupSummary lineup = buildMostUsedLineup(clubId, season, avgRatingByPlayer);

        List<LeaderEntry> topScorers = rankIntLeaders(statsBundle.players(), true);
        List<LeaderEntry> topAssists = rankIntLeaders(statsBundle.players(), false);
        List<RatingLeaderEntry> topRatings = rankRatingLeaders(statsBundle.players());

        return new ClubOverviewResponse(
                clubId,
                season,
                lineup.formation(),
                lineup.formationCounts(),
                lineup.lineupMatches(),
                lineup.xi(),
                topScorers,
                topAssists,
                topRatings
        );
    }

    private record MatchKey(LocalDate matchDate, int gameweek, int fixtureId) implements Comparable<MatchKey> {
        @Override
        public int compareTo(MatchKey other) {
            int c = this.matchDate.compareTo(other.matchDate);
            if (c != 0) return c;
            c = Integer.compare(this.gameweek, other.gameweek);
            if (c != 0) return c;
            return Integer.compare(this.fixtureId, other.fixtureId);
        }
    }

    private record FixtureLineupRow(int fixtureId, int gameweek, LocalDate matchDate, String formation) {
        MatchKey matchKey() {
            return new MatchKey(matchDate, gameweek, fixtureId);
        }
    }

    private record StarterRow(
            int fixtureId,
            int gameweek,
            LocalDate matchDate,
            String formation,
            int playerId,
            String playerName,
            String matchPosition,
            Integer rowIndex,
            Integer slotIndex
    ) {
        MatchKey matchKey() {
            return new MatchKey(matchDate, gameweek, fixtureId);
        }
    }

    private record SlotDef(int rowIndex, int slotIndex, String position, double x, double y) {
    }

    private record CandidateScore(
            int playerId,
            String playerName,
            int starts,
            MatchKey latestMatch,
            Double avgRating
    ) {
    }

    private record LineupSummary(
            String formation,
            List<FormationCount> formationCounts,
            int lineupMatches,
            List<XiPlayer> xi
    ) {
    }

    private LineupSummary buildMostUsedLineup(int clubId, int season, Map<Integer, Double> avgRatingByPlayer) {
        List<FixtureLineupRow> lineupRows = jdbc.query("""
                SELECT fl.fixture_id, f.gameweek, f.match_date, fl.formation
                FROM fixture_lineups fl
                JOIN fixtures f ON f.id = fl.fixture_id
                  AND f.league_id = fl.league_id
                  AND f.season_year = fl.season_year
                WHERE fl.league_id = ?
                  AND fl.season_year = ?
                  AND fl.club_id = ?
                  AND fl.formation IS NOT NULL
                ORDER BY f.match_date ASC, f.gameweek ASC, fl.fixture_id ASC
                """, (rs, idx) -> {
            Date d = rs.getDate("match_date");
            return new FixtureLineupRow(
                    rs.getInt("fixture_id"),
                    rs.getInt("gameweek"),
                    d == null ? LocalDate.MIN : d.toLocalDate(),
                    rs.getString("formation").strip()
            );
        }, LEAGUE_ID, season, clubId);

        if (lineupRows.isEmpty()) {
            return new LineupSummary(null, List.of(), 0, List.of());
        }

        // Count formations & track latest match for tie-breaking
        Map<String, Integer> counts = new HashMap<>();
        Map<String, MatchKey> latestByFormation = new HashMap<>();
        for (FixtureLineupRow row : lineupRows) {
            counts.merge(row.formation(), 1, Integer::sum);
            latestByFormation.merge(row.formation(), row.matchKey(), (a, b) -> a.compareTo(b) >= 0 ? a : b);
        }

        List<String> sortedFormations = new ArrayList<>(counts.keySet());
        sortedFormations.sort(Comparator
                .comparingInt((String fm) -> counts.get(fm)).reversed()
                .thenComparing(fm -> latestByFormation.get(fm), Comparator.reverseOrder())
                .thenComparing(Comparator.naturalOrder()));

        String primaryFormation = sortedFormations.getFirst();
        List<FormationCount> formationCounts = sortedFormations.stream()
                .map(fm -> new FormationCount(fm, counts.get(fm)))
                .toList();
        int lineupMatches = lineupRows.size();

        List<StarterRow> starters = jdbc.query("""
                SELECT flp.fixture_id, f.gameweek, f.match_date, fl.formation,
                       flp.player_id, p.name AS player_name,
                       flp.match_position, flp.row_index, flp.slot_index
                FROM fixture_lineup_players flp
                JOIN fixture_lineups fl ON fl.fixture_id = flp.fixture_id AND fl.club_id = flp.club_id
                JOIN fixtures f ON f.id = flp.fixture_id
                  AND f.league_id = fl.league_id
                  AND f.season_year = fl.season_year
                JOIN players p ON p.id = flp.player_id
                WHERE fl.league_id = ?
                  AND fl.season_year = ?
                  AND flp.club_id = ?
                  AND fl.formation IS NOT NULL
                  AND flp.role = 'STARTER'
                """, (rs, idx) -> {
            Date d = rs.getDate("match_date");
            return new StarterRow(
                    rs.getInt("fixture_id"),
                    rs.getInt("gameweek"),
                    d == null ? LocalDate.MIN : d.toLocalDate(),
                    rs.getString("formation").strip(),
                    rs.getInt("player_id"),
                    rs.getString("player_name"),
                    rs.getString("match_position"),
                    rs.getObject("row_index", Integer.class),
                    rs.getObject("slot_index", Integer.class)
            );
        }, LEAGUE_ID, season, clubId);

        if (starters.isEmpty()) {
            return new LineupSummary(primaryFormation, formationCounts, lineupMatches, List.of());
        }

        List<SlotDef> slots = buildFormationSlots(primaryFormation, starters);
        List<XiPlayer> xi = selectXi(slots, primaryFormation, starters, avgRatingByPlayer);

        return new LineupSummary(primaryFormation, formationCounts, lineupMatches, xi);
    }

    private List<SlotDef> buildFormationSlots(String primaryFormation, List<StarterRow> starters) {
        List<Integer> lines = Formation.lines(primaryFormation);
        // Determine canonical match_position observed at each (rowIndex, slotIndex) in primaryFormation
        Map<String, Map<String, Integer>> posCountsBySlot = new HashMap<>();
        Map<String, Map<String, MatchKey>> posLatestBySlot = new HashMap<>();
        for (StarterRow s : starters) {
            if (!primaryFormation.equals(s.formation()) || s.rowIndex() == null || s.slotIndex() == null
                    || s.matchPosition() == null || s.matchPosition().isBlank()) {
                continue;
            }
            String slotKey = s.rowIndex() + ":" + s.slotIndex();
            String pos = s.matchPosition().strip();
            posCountsBySlot.computeIfAbsent(slotKey, k -> new HashMap<>()).merge(pos, 1, Integer::sum);
            posLatestBySlot.computeIfAbsent(slotKey, k -> new HashMap<>())
                    .merge(pos, s.matchKey(), (a, b) -> a.compareTo(b) >= 0 ? a : b);
        }

        int rowCount = lines.size();
        List<SlotDef> slots = new ArrayList<>(11);
        for (int r = 0; r < rowCount; r++) {
            int count = lines.get(r);
            for (int c = 0; c < count; c++) {
                String slotKey = r + ":" + c;
                String pos = null;
                Map<String, Integer> observed = posCountsBySlot.get(slotKey);
                if (observed != null && !observed.isEmpty()) {
                    Map<String, MatchKey> latestMap = posLatestBySlot.get(slotKey);
                    pos = observed.keySet().stream()
                            .min(Comparator
                                    .comparingInt((String p) -> observed.get(p)).reversed()
                                    .thenComparing(p -> latestMap.get(p), Comparator.reverseOrder())
                                    .thenComparing(Comparator.naturalOrder()))
                            .orElse(null);
                }
                if (pos == null) {
                    pos = fallbackSlotPosition(primaryFormation, lines, r, c);
                }
                double[] xy = verticalCoordinates(rowCount, lines, r, c);
                slots.add(new SlotDef(r, c, pos, xy[0], xy[1]));
            }
        }
        return slots;
    }

    private List<XiPlayer> selectXi(
            List<SlotDef> slots,
            String primaryFormation,
            List<StarterRow> starters,
            Map<Integer, Double> avgRatingByPlayer) {

        Map<Integer, String> playerNames = new HashMap<>();
        // Count starts per (position, playerId) across ALL lineup matches
        Map<String, Map<Integer, Integer>> posStarts = new HashMap<>();
        Map<String, Map<Integer, MatchKey>> posLatest = new HashMap<>();

        // Count starts per (slotKey in primaryFormation, playerId) for left/right placement of duplicate positions
        Map<String, Map<Integer, Integer>> slotStartsPrimary = new HashMap<>();
        Map<String, Map<Integer, MatchKey>> slotLatestPrimary = new HashMap<>();

        // Overall starts per player (fallback only if a position has zero remaining candidates)
        Map<Integer, Integer> totalStarts = new HashMap<>();
        Map<Integer, MatchKey> totalLatest = new HashMap<>();

        for (StarterRow s : starters) {
            playerNames.putIfAbsent(s.playerId(), s.playerName());
            totalStarts.merge(s.playerId(), 1, Integer::sum);
            totalLatest.merge(s.playerId(), s.matchKey(), (a, b) -> a.compareTo(b) >= 0 ? a : b);

            if (s.matchPosition() != null && !s.matchPosition().isBlank()) {
                String pos = s.matchPosition().strip();
                posStarts.computeIfAbsent(pos, k -> new HashMap<>()).merge(s.playerId(), 1, Integer::sum);
                posLatest.computeIfAbsent(pos, k -> new HashMap<>())
                        .merge(s.playerId(), s.matchKey(), (a, b) -> a.compareTo(b) >= 0 ? a : b);
            }
            if (primaryFormation.equals(s.formation()) && s.rowIndex() != null && s.slotIndex() != null) {
                String slotKey = s.rowIndex() + ":" + s.slotIndex();
                slotStartsPrimary.computeIfAbsent(slotKey, k -> new HashMap<>()).merge(s.playerId(), 1, Integer::sum);
                slotLatestPrimary.computeIfAbsent(slotKey, k -> new HashMap<>())
                        .merge(s.playerId(), s.matchKey(), (a, b) -> a.compareTo(b) >= 0 ? a : b);
            }
        }

        // Group slots by canonical position (e.g. two CB slots share "CB", two CM slots share "CM")
        Map<String, List<SlotDef>> slotsByPosition = new LinkedHashMap<>();
        for (SlotDef slot : slots) {
            slotsByPosition.computeIfAbsent(slot.position(), k -> new ArrayList<>()).add(slot);
        }

        // Process position groups with the strongest top candidate first so nailed-on players
        // are assigned to their main role before secondary roles can claim them.
        List<String> positionOrder = new ArrayList<>(slotsByPosition.keySet());
        positionOrder.sort(Comparator
                .comparingInt((String pos) -> maxStarts(posStarts.get(pos))).reversed()
                .thenComparingInt(pos -> slotsByPosition.get(pos).getFirst().rowIndex())
                .thenComparingInt(pos -> slotsByPosition.get(pos).getFirst().slotIndex()));

        Comparator<CandidateScore> candidateComparator = Comparator
                .comparingInt(CandidateScore::starts).reversed()
                .thenComparing(CandidateScore::latestMatch, Comparator.reverseOrder())
                .thenComparing(CandidateScore::avgRating, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingInt(CandidateScore::playerId);

        Set<Integer> usedPlayers = new HashSet<>();
        Map<SlotDef, Integer> assignedPlayer = new HashMap<>();
        Map<SlotDef, Integer> assignedStarts = new HashMap<>();

        for (String pos : positionOrder) {
            List<SlotDef> groupSlots = slotsByPosition.get(pos);
            Map<Integer, Integer> startsMap = posStarts.getOrDefault(pos, Map.of());
            Map<Integer, MatchKey> latestMap = posLatest.getOrDefault(pos, Map.of());

            List<CandidateScore> candidates = startsMap.entrySet().stream()
                    .filter(e -> !usedPlayers.contains(e.getKey()))
                    .map(e -> new CandidateScore(
                            e.getKey(),
                            playerNames.get(e.getKey()),
                            e.getValue(),
                            latestMap.get(e.getKey()),
                            avgRatingByPlayer.get(e.getKey())))
                    .sorted(candidateComparator)
                    .toList();

            List<Integer> picked = new ArrayList<>(groupSlots.size());
            for (CandidateScore candidate : candidates) {
                if (picked.size() >= groupSlots.size()) break;
                picked.add(candidate.playerId());
                usedPlayers.add(candidate.playerId());
            }

            // Fallback if fewer unused players started at this exact match_position than slots needed
            if (picked.size() < groupSlots.size()) {
                List<CandidateScore> fallback = totalStarts.entrySet().stream()
                        .filter(e -> !usedPlayers.contains(e.getKey()))
                        .map(e -> new CandidateScore(
                                e.getKey(),
                                playerNames.get(e.getKey()),
                                e.getValue(),
                                totalLatest.get(e.getKey()),
                                avgRatingByPlayer.get(e.getKey())))
                        .sorted(candidateComparator)
                        .toList();
                for (CandidateScore candidate : fallback) {
                    if (picked.size() >= groupSlots.size()) break;
                    picked.add(candidate.playerId());
                    usedPlayers.add(candidate.playerId());
                }
            }

            // Place the picked players into the exact (rowIndex, slotIndex) slots of this group
            // maximizing starts at each specific slot in the primary formation.
            List<Integer> orderedForSlots = alignPlayersToSlots(groupSlots, picked, slotStartsPrimary, slotLatestPrimary);
            for (int i = 0; i < Math.min(groupSlots.size(), orderedForSlots.size()); i++) {
                SlotDef slot = groupSlots.get(i);
                int pid = orderedForSlots.get(i);
                assignedPlayer.put(slot, pid);
                assignedStarts.put(slot, startsMap.getOrDefault(pid, 0));
            }
        }

        // Compute short names and disambiguate if two XI players collide
        Map<SlotDef, String> rawShortNames = new HashMap<>();
        Map<String, Integer> shortNameFreq = new HashMap<>();
        for (SlotDef slot : slots) {
            Integer pid = assignedPlayer.get(slot);
            if (pid == null) continue;
            String shortName = shortenPitchName(playerNames.get(pid));
            rawShortNames.put(slot, shortName);
            shortNameFreq.merge(shortName, 1, Integer::sum);
        }

        List<XiPlayer> result = new ArrayList<>(slots.size());
        for (SlotDef slot : slots) {
            Integer pid = assignedPlayer.get(slot);
            if (pid == null) continue;
            String fullName = playerNames.get(pid);
            String shortName = rawShortNames.get(slot);
            if (shortNameFreq.getOrDefault(shortName, 0) > 1) {
                shortName = disambiguatedShortName(fullName, shortName);
            }
            result.add(new XiPlayer(
                    pid,
                    fullName,
                    shortName,
                    slot.position(),
                    slot.rowIndex(),
                    slot.slotIndex(),
                    slot.x(),
                    slot.y(),
                    assignedStarts.getOrDefault(slot, 0)
            ));
        }
        return result;
    }

    private List<Integer> alignPlayersToSlots(
            List<SlotDef> groupSlots,
            List<Integer> picked,
            Map<String, Map<Integer, Integer>> slotStartsPrimary,
            Map<String, Map<Integer, MatchKey>> slotLatestPrimary) {
        if (picked.size() <= 1 || groupSlots.size() <= 1) {
            return picked;
        }
        int k = Math.min(groupSlots.size(), picked.size());
        List<Integer> subPicked = picked.subList(0, k);
        // Generate permutations (k is at most 3, e.g., 2 CBs or 3 CBs -> at most 6 permutations)
        List<List<Integer>> perms = new ArrayList<>();
        permute(new ArrayList<>(subPicked), 0, perms);

        List<Integer> best = subPicked;
        int bestScore = -1;
        for (List<Integer> perm : perms) {
            int score = 0;
            for (int i = 0; i < k; i++) {
                String slotKey = groupSlots.get(i).rowIndex() + ":" + groupSlots.get(i).slotIndex();
                score += slotStartsPrimary.getOrDefault(slotKey, Map.of()).getOrDefault(perm.get(i), 0);
            }
            if (score > bestScore) {
                bestScore = score;
                best = perm;
            }
        }
        return best;
    }

    private static void permute(List<Integer> list, int start, List<List<Integer>> out) {
        if (start == list.size() - 1) {
            out.add(List.copyOf(list));
            return;
        }
        for (int i = start; i < list.size(); i++) {
            java.util.Collections.swap(list, start, i);
            permute(list, start + 1, out);
            java.util.Collections.swap(list, start, i);
        }
    }

    private static int maxStarts(Map<Integer, Integer> map) {
        if (map == null || map.isEmpty()) return 0;
        return map.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    static double[] verticalCoordinates(int rowCount, List<Integer> lines, int rowIndex, int slotIndex) {
        int count = lines.get(rowIndex);
        double x = switch (count) {
            case 1 -> 50.0;
            case 2 -> slotIndex == 0 ? 36.0 : 64.0;
            case 3 -> slotIndex == 0 ? 16.0 : slotIndex == 1 ? 50.0 : 84.0;
            case 4 -> switch (slotIndex) {
                case 0 -> 14.0;
                case 1 -> 37.0;
                case 2 -> 63.0;
                default -> 86.0;
            };
            case 5 -> switch (slotIndex) {
                case 0 -> 11.0;
                case 1 -> 30.5;
                case 2 -> 50.0;
                case 3 -> 69.5;
                default -> 89.0;
            };
            default -> Math.round(((slotIndex + 1) * 100.0 / (count + 1)) * 10.0) / 10.0;
        };

        double y;
        if (rowIndex == 0) {
            y = 90.0;
        } else if (rowIndex == rowCount - 1) {
            y = 14.0;
        } else {
            double step = (74.0 - 14.0) / Math.max(1, rowCount - 2);
            y = 74.0 - (rowIndex - 1) * step;
            boolean isEdge = count >= 3 && (slotIndex == 0 || slotIndex == count - 1);
            if (isEdge) {
                y -= 3.5;
            }
        }
        return new double[]{
                Math.round(x * 10.0) / 10.0,
                Math.round(y * 10.0) / 10.0
        };
    }

    private static String fallbackSlotPosition(String formation, List<Integer> lines, int rowIndex, int slotIndex) {
        List<List<String>> mapped = FORMATION_LABELS.get(formation);
        if (mapped != null && rowIndex < mapped.size() && slotIndex < mapped.get(rowIndex).size()) {
            return mapped.get(rowIndex).get(slotIndex);
        }
        int count = lines.get(rowIndex);
        int last = lines.size() - 1;
        int lane = Integer.compare(slotIndex * 2, count - 1);
        boolean edge = count > 1 && (slotIndex == 0 || slotIndex == count - 1);
        String prefix = lane < 0 ? "L" : lane > 0 ? "R" : "";
        if (rowIndex == 0) return "GK";
        if (rowIndex == 1) return (count >= 4 && edge) ? prefix + (count == 5 ? "WB" : "B") : "CB";
        if (rowIndex == last) return (count >= 3 && edge) ? prefix + "W" : "ST";
        if (count >= 4 && edge) return prefix + (lines.get(1) == 3 ? "WB" : "M");
        if (lines.size() >= 5 && rowIndex == last - 1 && count <= 3) return "CAM";
        return "CM";
    }

    static String shortenPitchName(String fullName) {
        if (fullName == null) return "";
        String trimmed = fullName.strip();
        if (trimmed.isEmpty()) return "";
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 1) return parts[0];
        int start = parts.length - 1;
        while (start > 1 && SURNAME_PARTICLES.contains(parts[start - 1].toLowerCase(Locale.ROOT))) {
            start--;
        }
        return String.join(" ", java.util.Arrays.copyOfRange(parts, start, parts.length));
    }

    private static String disambiguatedShortName(String fullName, String shortName) {
        String[] parts = fullName.strip().split("\\s+");
        if (parts.length < 2) return shortName;
        int firstCp = parts[0].codePointAt(0);
        String initial = new String(Character.toChars(firstCp));
        return initial + ". " + shortName;
    }

    private record PlayerStatRow(
            int playerId,
            String name,
            int appearances,
            Integer goals,
            Integer assists,
            Double averageRating,
            int ratedAppearances
    ) {
    }

    private record ClubStatsBundle(List<PlayerStatRow> players) {
    }

    private ClubStatsBundle loadClubStats(int clubId, int season) {
        if (season == 2026) {
            ClubStatisticsResponse stats = clubStatisticsService.statistics(clubId, 2026);
            if (stats.players().isEmpty()) {
                return new ClubStatsBundle(List.of());
            }
            Map<Integer, String> names = loadPlayerNames(stats.players().stream()
                    .map(ClubStatisticsResponse.PlayerStatistics::playerId)
                    .toList());
            List<PlayerStatRow> rows = stats.players().stream()
                    .filter(p -> names.containsKey(p.playerId()))
                    .map(p -> new PlayerStatRow(
                            p.playerId(),
                            names.get(p.playerId()),
                            p.appearances(),
                            p.goals(),
                            p.assists(),
                            p.averageRating(),
                            p.ratedAppearances()))
                    .toList();
            return new ClubStatsBundle(rows);
        }

        // Legacy seasons (e.g., 2024/25): read club-specific player_season_stats
        List<PlayerStatRow> rows = jdbc.query("""
                SELECT ps.player_id, p.name,
                       COALESCE(ps.appearances, 0) AS appearances,
                       ps.goals, ps.assists
                FROM player_season_stats ps
                JOIN players p ON p.id = ps.player_id
                WHERE ps.league_id = ? AND ps.season_year = ? AND ps.club_id = ?
                ORDER BY ps.player_id
                """, (rs, idx) -> new PlayerStatRow(
                rs.getInt("player_id"),
                rs.getString("name"),
                rs.getInt("appearances"),
                rs.getObject("goals", Integer.class),
                rs.getObject("assists", Integer.class),
                null,
                0
        ), LEAGUE_ID, season, clubId);
        return new ClubStatsBundle(rows);
    }

    private Map<Integer, String> loadPlayerNames(List<Integer> playerIds) {
        if (playerIds.isEmpty()) return Map.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(playerIds.size(), "?"));
        Map<Integer, String> map = new HashMap<>();
        jdbc.query("SELECT id, name FROM players WHERE id IN (" + placeholders + ")", rs -> {
            map.put(rs.getInt("id"), rs.getString("name"));
        }, playerIds.toArray());
        return map;
    }

    private static List<LeaderEntry> rankIntLeaders(List<PlayerStatRow> players, boolean goals) {
        List<PlayerStatRow> sorted = players.stream()
                .filter(p -> {
                    Integer val = goals ? p.goals() : p.assists();
                    return val != null && val > 0;
                })
                .sorted(Comparator
                        .comparingInt((PlayerStatRow p) -> goals ? p.goals() : p.assists()).reversed()
                        .thenComparingInt(PlayerStatRow::appearances)
                        .thenComparing(PlayerStatRow::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(PlayerStatRow::playerId))
                .limit(TOP_LIMIT)
                .toList();

        List<LeaderEntry> result = new ArrayList<>(sorted.size());
        int currentRank = 0;
        int prevVal = Integer.MIN_VALUE;
        for (int i = 0; i < sorted.size(); i++) {
            PlayerStatRow p = sorted.get(i);
            int val = goals ? p.goals() : p.assists();
            if (i == 0 || val != prevVal) {
                currentRank = i + 1;
                prevVal = val;
            }
            result.add(new LeaderEntry(currentRank, p.playerId(), p.name(), p.appearances(), val));
        }
        return result;
    }

    private static List<RatingLeaderEntry> rankRatingLeaders(List<PlayerStatRow> players) {
        record Rounded(PlayerStatRow row, double rounded) {
        }
        List<Rounded> sorted = players.stream()
                .filter(p -> p.ratedAppearances() >= MIN_RATED_APPEARANCES
                        && p.averageRating() != null && p.averageRating() > 0.0)
                .map(p -> new Rounded(p, BigDecimal.valueOf(p.averageRating())
                        .setScale(2, RoundingMode.HALF_UP).doubleValue()))
                .sorted(Comparator
                        .comparingDouble(Rounded::rounded).reversed()
                        .thenComparingInt(r -> r.row().appearances())
                        .thenComparing(r -> r.row().name(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(r -> r.row().playerId()))
                .limit(TOP_LIMIT)
                .toList();

        List<RatingLeaderEntry> result = new ArrayList<>(sorted.size());
        int currentRank = 0;
        double prevVal = Double.NaN;
        for (int i = 0; i < sorted.size(); i++) {
            Rounded item = sorted.get(i);
            if (i == 0 || Double.compare(item.rounded(), prevVal) != 0) {
                currentRank = i + 1;
                prevVal = item.rounded();
            }
            result.add(new RatingLeaderEntry(
                    currentRank,
                    item.row().playerId(),
                    item.row().name(),
                    item.row().appearances(),
                    item.row().ratedAppearances(),
                    item.rounded()
            ));
        }
        return result;
    }
}
