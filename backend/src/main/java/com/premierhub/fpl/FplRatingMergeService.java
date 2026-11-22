package com.premierhub.fpl;

import com.premierhub.manualstats.ManualMatchStatsCsvReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Merges SofaScore screenshot ratings + matchday squad list with FPL match statistics CSV
 * into GW10-compatible manual-match-stats, confirmed-unrated, missing, and anomalies CSVs.
 */
@Service
public class FplRatingMergeService {
    private static final int LEAGUE_ID = 39;
    private static final int SEASON_YEAR = 2026;
    public static final String ANOMALIES_HEADER = "fixture_id,player_id,code,details";
    public static final String UNRATED_HEADER = "fixture_id,player_id,name,minutes,rating,fantasy_points";
    public static final String MISSING_HEADER = "fixture_id,club,player_id,name,field";
    private static final Set<String> UNRATED_MARKERS = Set.of(
            "NO_RATING_SHOWN", "UNRATED", "NO_RATING", "KHONG_CO_RATING"
    );

    private final JdbcTemplate jdbc;

    public FplRatingMergeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Result merge(int season, int gameweek, Path fplDir, Path ratingsFile,
                        Path lineupPlayersFile, Path outputDir) throws IOException {
        return merge(season, gameweek, fplDir, ratingsFile, lineupPlayersFile, outputDir, Set.of());
    }

    public Result merge(int season, int gameweek, Path fplDir, Path ratingsFile,
                        Path lineupPlayersFile, Path outputDir, Set<Integer> fixtureIds) throws IOException {
        if (season != SEASON_YEAR) {
            throw new IllegalArgumentException("Only season 2026 is supported, got " + season);
        }
        if (gameweek < 11 || gameweek > 38) {
            throw new IllegalArgumentException(
                    "Rating merge is only allowed from GW11 to GW38 (GW1-GW10 manual_fixture_player_stats are frozen); got GW"
                            + gameweek);
        }
        Set<Integer> selectedFixtureIds = fixtureIds == null ? Set.of() : fixtureIds;
        boolean filteredMode = !selectedFixtureIds.isEmpty();

        String gwUpper = String.format("GW%02d", gameweek);
        Path fixtureMapFile = fplDir.resolve("fixture-map.csv");
        Path fplStatsFile = fplDir.resolve("fpl-match-stats-2026-" + gwUpper + ".csv");
        if (!Files.exists(fixtureMapFile)) {
            throw new IOException("Missing fixture-map.csv: " + fixtureMapFile);
        }
        if (!Files.exists(fplStatsFile)) {
            throw new IOException("Missing FPL match stats CSV (resolve unmapped.csv first): " + fplStatsFile);
        }

        Map<Integer, FixtureScoreCheck> allFixtureMap = readFixtureMap(fixtureMapFile);
        Map<Integer, FixtureScoreCheck> fixtureMap;
        if (filteredMode) {
            fixtureMap = new LinkedHashMap<>();
            for (int id : selectedFixtureIds) {
                FixtureScoreCheck found = allFixtureMap.get(id);
                if (found == null) {
                    throw new IllegalArgumentException("Requested fixture_id=" + id
                            + " is not present in " + fixtureMapFile + " for GW" + gameweek);
                }
                fixtureMap.put(id, found);
            }
        } else {
            fixtureMap = allFixtureMap;
        }

        Map<FixturePlayerKey, FplStatRow> fplStats = filterByFixtures(readFplMatchStats(fplStatsFile), selectedFixtureIds);
        Map<FixturePlayerKey, BenchCardRow> benchCards = filterByFixtures(
                readBenchCards(fplDir.resolve("bench-cards.csv")), selectedFixtureIds);
        Map<Integer, Set<Integer>> lineupSquadByFixture = lineupPlayersFile != null
                ? filterFixtureSets(readLineupSquad(lineupPlayersFile), selectedFixtureIds) : Map.of();

        List<RatingEntry> ratings = readRatings(ratingsFile, fixtureMap.keySet(), filteredMode);
        Map<Integer, Map<Integer, RatingEntry>> ratingsByFixture = new LinkedHashMap<>();
        for (RatingEntry entry : ratings) {
            Map<Integer, RatingEntry> perFixture = ratingsByFixture.computeIfAbsent(
                    entry.fixtureId(), ignored -> new LinkedHashMap<>());
            if (perFixture.putIfAbsent(entry.playerId(), entry) != null) {
                throw new IllegalArgumentException("Duplicate (fixture_id, player_id) in ratings file: "
                        + entry.fixtureId() + "/" + entry.playerId());
            }
        }

        List<Anomaly> anomalies = new ArrayList<>();
        Set<Integer> blockedFixtures = new HashSet<>();

        // 1. Check FPL score vs DB fixtures score
        for (FixtureScoreCheck check : fixtureMap.values()) {
            DbScore dbScore = loadDbScore(check.fixtureId());
            if (dbScore == null || dbScore.homeGoals() == null || dbScore.awayGoals() == null
                    || check.fplHomeGoals() == null || check.fplAwayGoals() == null
                    || !Objects.equals(check.fplHomeGoals(), dbScore.homeGoals())
                    || !Objects.equals(check.fplAwayGoals(), dbScore.awayGoals())) {
                blockedFixtures.add(check.fixtureId());
                anomalies.add(new Anomaly(
                        check.fixtureId(), null, "SCORE_MISMATCH",
                        "FPL score " + check.fplHomeGoals() + "-" + check.fplAwayGoals()
                        + " differs from DB fixtures score "
                        + (dbScore == null ? "MISSING" : dbScore.homeGoals() + "-" + dbScore.awayGoals())));
            }
        }

        // 2. Check FPL played players against matchday squad & ratings
        Map<Integer, List<FplStatRow>> fplByFixture = new HashMap<>();
        for (FplStatRow stat : fplStats.values()) {
            fplByFixture.computeIfAbsent(stat.fixtureId(), ignored -> new ArrayList<>()).add(stat);
            Map<Integer, RatingEntry> fixtureRatings = ratingsByFixture.getOrDefault(stat.fixtureId(), Map.of());
            boolean inRatings = fixtureRatings.containsKey(stat.playerId());
            boolean inLineup = lineupSquadByFixture.isEmpty()
                    || lineupSquadByFixture.getOrDefault(stat.fixtureId(), Set.of()).contains(stat.playerId());
            if (!inRatings || !inLineup) {
                blockedFixtures.add(stat.fixtureId());
                anomalies.add(new Anomaly(
                        stat.fixtureId(), stat.playerId(), "FPL_PLAYED_NOT_IN_SQUAD",
                        "FPL reports minutes=" + stat.minutes()
                        + " but player is missing from "
                        + (!inRatings ? "ratings/screenshot" : "lineup squad")));
            }
        }

        // 3. Check every rating/squad entry against FPL
        List<MergedRow> mergedRows = new ArrayList<>();
        List<UnratedRow> unratedRows = new ArrayList<>();
        List<MissingRow> missingRows = new ArrayList<>();

        Set<Integer> allFixtures = new LinkedHashSet<>(fixtureMap.keySet());
        allFixtures.addAll(ratingsByFixture.keySet());

        for (int fixtureId : allFixtures) {
            Map<Integer, RatingEntry> fixtureRatings = ratingsByFixture.getOrDefault(fixtureId, Map.of());
            Set<Integer> benchCardChecked = new HashSet<>();
            List<MergedRow> fixtureMerged = new ArrayList<>();
            List<UnratedRow> fixtureUnrated = new ArrayList<>();
            List<MissingRow> fixtureMissing = new ArrayList<>();

            for (RatingEntry entry : fixtureRatings.values()) {
                FixturePlayerKey key = new FixturePlayerKey(fixtureId, entry.playerId());
                FplStatRow fpl = fplStats.get(key);
                boolean fplPlayed = fpl != null && fpl.minutes() > 0;

                if (!fplPlayed) {
                    boolean entryAnomaly = false;
                    if (entry.rating() != null) {
                        blockedFixtures.add(fixtureId);
                        anomalies.add(new Anomaly(
                                fixtureId, entry.playerId(), "RATING_WITH_ZERO_MINUTES",
                                "Screenshot has rating=" + entry.rating().toPlainString()
                                + " but FPL reports 0 minutes"));
                        entryAnomaly = true;
                    }
                    BenchCardRow benchCard = benchCards.get(key);
                    if (benchCard != null && (benchCard.yellowCards() > 0 || benchCard.redCards() > 0)) {
                        blockedFixtures.add(fixtureId);
                        anomalies.add(new Anomaly(
                                fixtureId, entry.playerId(), "BENCH_CARD",
                                "FPL reports minutes=0 with yellow_cards=" + benchCard.yellowCards()
                                + " red_cards=" + benchCard.redCards()));
                        benchCardChecked.add(entry.playerId());
                        entryAnomaly = true;
                    }
                    if (entryAnomaly) {
                        continue;
                    }
                }

                if (fplPlayed) {
                    if (entry.rating() != null) {
                        fixtureMerged.add(new MergedRow(
                                SEASON_YEAR, fixtureId, entry.playerId(), "PLAYED", entry.rating(),
                                fpl.minutes(), fpl.goals(), fpl.assists(), fpl.yellowCards(), fpl.redCards()));
                    } else if (entry.explicitlyUnrated()) {
                        fixtureMerged.add(new MergedRow(
                                SEASON_YEAR, fixtureId, entry.playerId(), "PLAYED", null,
                                fpl.minutes(), fpl.goals(), fpl.assists(), fpl.yellowCards(), fpl.redCards()));
                        fixtureUnrated.add(new UnratedRow(
                                fixtureId, entry.playerId(), entry.playerName(), fpl.minutes()));
                    } else {
                        fixtureMerged.add(new MergedRow(
                                SEASON_YEAR, fixtureId, entry.playerId(), "PLAYED", null,
                                fpl.minutes(), fpl.goals(), fpl.assists(), fpl.yellowCards(), fpl.redCards()));
                        fixtureMissing.add(new MissingRow(
                                fixtureId, entry.clubName(), entry.playerId(), entry.playerName(), "rating"));
                    }
                } else {
                    fixtureMerged.add(new MergedRow(
                            SEASON_YEAR, fixtureId, entry.playerId(), "DID_NOT_PLAY", null,
                            0, 0, 0, 0, 0));
                }
            }

            // Also flag bench cards for players present in lineup squad even if absent from ratings file.
            for (int squadPlayerId : lineupSquadByFixture.getOrDefault(fixtureId, Set.of())) {
                if (benchCardChecked.contains(squadPlayerId)) {
                    continue;
                }
                FixturePlayerKey key = new FixturePlayerKey(fixtureId, squadPlayerId);
                FplStatRow fpl = fplStats.get(key);
                if (fpl != null && fpl.minutes() > 0) {
                    continue;
                }
                BenchCardRow benchCard = benchCards.get(key);
                if (benchCard != null && (benchCard.yellowCards() > 0 || benchCard.redCards() > 0)) {
                    blockedFixtures.add(fixtureId);
                    anomalies.add(new Anomaly(
                            fixtureId, squadPlayerId, "BENCH_CARD",
                            "FPL reports minutes=0 with yellow_cards=" + benchCard.yellowCards()
                            + " red_cards=" + benchCard.redCards()));
                }
            }

            if (!blockedFixtures.contains(fixtureId)) {
                mergedRows.addAll(fixtureMerged);
                unratedRows.addAll(fixtureUnrated);
                missingRows.addAll(fixtureMissing);
            }
        }

        Path anomaliesFile = outputDir.resolve("anomalies.csv");
        Path missingFile = outputDir.resolve("missing.csv");
        Path unratedFile = outputDir.resolve("confirmed-unrated.csv");
        Path manualStatsFile = outputDir.resolve("manual-match-stats-2026-" + gwUpper + ".csv");

        // Validate/merge with existing CSVs before writing anything when --fixture-ids is active
        List<MergedRow> finalMergedRows = filteredMode
                ? mergeManualStats(manualStatsFile, mergedRows)
                : new ArrayList<>(mergedRows);
        List<UnratedRow> finalUnratedRows = filteredMode
                ? mergeUnrated(unratedFile, unratedRows)
                : new ArrayList<>(unratedRows);
        List<MissingRow> finalMissingRows = filteredMode
                ? mergeMissing(missingFile, missingRows, selectedFixtureIds)
                : new ArrayList<>(missingRows);
        List<Anomaly> finalAnomalies = filteredMode
                ? mergeAnomalies(anomaliesFile, anomalies, selectedFixtureIds)
                : new ArrayList<>(anomalies);

        Files.createDirectories(outputDir);
        finalAnomalies.sort(Comparator.comparingInt(Anomaly::fixtureId)
                .thenComparing(a -> a.playerId() == null ? 0 : a.playerId())
                .thenComparing(Anomaly::code));
        writeAnomalies(anomaliesFile, finalAnomalies);

        finalMissingRows.sort(Comparator.comparingInt(MissingRow::fixtureId)
                .thenComparingInt(MissingRow::playerId));
        writeMissing(missingFile, finalMissingRows);

        finalUnratedRows.sort(Comparator.comparingInt(UnratedRow::fixtureId)
                .thenComparingInt(UnratedRow::playerId));
        writeUnrated(unratedFile, finalUnratedRows);

        if (finalMergedRows.isEmpty()) {
            Files.deleteIfExists(manualStatsFile);
        } else {
            finalMergedRows.sort(Comparator.comparingInt(MergedRow::fixtureId)
                    .thenComparingInt(MergedRow::playerId));
            writeManualMatchStats(manualStatsFile, finalMergedRows);
        }

        int includedFixtures = (int) allFixtures.stream()
                .filter(id -> !blockedFixtures.contains(id) && ratingsByFixture.containsKey(id))
                .count();
        return new Result(gameweek, includedFixtures, blockedFixtures.size(),
                mergedRows.size(), unratedRows.size(), missingRows.size(), anomalies.size());
    }

    private DbScore loadDbScore(int fixtureId) {
        List<DbScore> rows = jdbc.query("""
                SELECT home_goals, away_goals FROM fixtures
                WHERE id = ? AND league_id = ? AND season_year = ?
                """, (rs, index) -> new DbScore(
                rs.getObject("home_goals", Integer.class),
                rs.getObject("away_goals", Integer.class)), fixtureId, LEAGUE_ID, SEASON_YEAR);
        return rows.size() == 1 ? rows.getFirst() : null;
    }

    private List<RatingEntry> readRatings(Path file, Set<Integer> knownFixtures, boolean filterToKnownFixtures) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("Ratings CSV is empty: " + file);
            }
            String[] cols = headerLine.replaceFirst("^\uFEFF", "").strip().split(",", -1);
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < cols.length; i++) {
                idx.put(cols[i].strip().toLowerCase(Locale.ROOT), i);
            }
            if (!idx.containsKey("fixture_id") || !idx.containsKey("rating")) {
                throw new IllegalArgumentException("Ratings CSV must include fixture_id and rating columns");
            }
            boolean hasPlayerId = idx.containsKey("player_id");
            if (!hasPlayerId && (!idx.containsKey("club_id") || !idx.containsKey("shirt_number"))) {
                throw new IllegalArgumentException("Ratings CSV without player_id requires club_id and shirt_number");
            }

            Map<Integer, LocalDate> fixtureDates = loadFixtureDates(knownFixtures);
            List<RatingEntry> entries = new ArrayList<>();
            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;
                String[] fields = line.split(",", -1);
                if (fields.length != cols.length) {
                    throw new IllegalArgumentException("Ratings CSV line " + lineNum
                            + ": expected " + cols.length + " columns, got " + fields.length);
                }
                int fixtureId = Integer.parseInt(fields[idx.get("fixture_id")].strip());
                if (filterToKnownFixtures && !knownFixtures.contains(fixtureId)) {
                    continue;
                }
                String clubName = idx.containsKey("club") ? fields[idx.get("club")].strip() : "";
                String displayName = idx.containsKey("name") ? fields[idx.get("name")].strip()
                        : idx.containsKey("display_name") ? fields[idx.get("display_name")].strip() : "";
                String rawRating = fields[idx.get("rating")].strip();
                String symbols = idx.containsKey("visible_symbols") ? fields[idx.get("visible_symbols")].strip()
                        : idx.containsKey("unrated") ? fields[idx.get("unrated")].strip() : "";

                int playerId;
                String playerName;
                if (hasPlayerId) {
                    playerId = Integer.parseInt(fields[idx.get("player_id")].strip());
                    playerName = displayName.isEmpty() ? lookupPlayerName(playerId) : displayName;
                } else {
                    int clubId = Integer.parseInt(fields[idx.get("club_id")].strip());
                    int shirtNumber = Integer.parseInt(fields[idx.get("shirt_number")].strip());
                    LocalDate matchDate = fixtureDates.get(fixtureId);
                    if (matchDate == null) {
                        throw new IllegalArgumentException("Ratings CSV line " + lineNum
                                + ": fixture_id " + fixtureId + " not found in DB");
                    }
                    ResolvedPlayer resolved = resolveByClubAndShirt(clubId, shirtNumber, matchDate, lineNum);
                    playerId = resolved.playerId();
                    playerName = resolved.name();
                }

                boolean unrated = isMarkedUnrated(rawRating, symbols);
                BigDecimal rating = unrated || rawRating.isEmpty() ? null : parseRating(rawRating, lineNum);
                entries.add(new RatingEntry(fixtureId, playerId, playerName, clubName, rating, unrated));
            }
            return entries;
        }
    }

    private Map<Integer, LocalDate> loadFixtureDates(Set<Integer> fixtureIds) {
        Map<Integer, LocalDate> result = new HashMap<>();
        for (int id : fixtureIds) {
            List<LocalDate> dates = jdbc.query(
                    "SELECT match_date FROM fixtures WHERE id = ? AND league_id = ? AND season_year = ?",
                    (rs, i) -> rs.getDate("match_date").toLocalDate(), id, LEAGUE_ID, SEASON_YEAR);
            if (dates.size() == 1) {
                result.put(id, dates.getFirst());
            }
        }
        return result;
    }

    private ResolvedPlayer resolveByClubAndShirt(int clubId, int shirtNumber, LocalDate matchDate, int lineNum) {
        List<ResolvedPlayer> matches = jdbc.query("""
                SELECT p.id, p.name
                FROM manual_player_memberships m
                JOIN player_season_profiles pr ON pr.league_id = m.league_id
                  AND pr.season_year = m.season_year AND pr.player_id = m.player_id AND pr.club_id = m.club_id
                JOIN players p ON p.id = m.player_id
                WHERE m.league_id = ? AND m.season_year = ? AND m.club_id = ?
                  AND pr.shirt_number = ? AND m.start_date <= ?
                  AND (m.end_date IS NULL OR m.end_date > ?)
                """, (rs, i) -> new ResolvedPlayer(rs.getInt("id"), rs.getString("name")),
                LEAGUE_ID, SEASON_YEAR, clubId, shirtNumber, Date.valueOf(matchDate), Date.valueOf(matchDate));
        if (matches.size() != 1) {
            throw new IllegalArgumentException("Ratings CSV line " + lineNum
                    + ": expected unique player for club_id=" + clubId + " shirt_number=" + shirtNumber
                    + " on " + matchDate + ", found " + matches.size());
        }
        return matches.getFirst();
    }

    private String lookupPlayerName(int playerId) {
        List<String> names = jdbc.queryForList("SELECT name FROM players WHERE id = ?", String.class, playerId);
        return names.isEmpty() ? ("Player " + playerId) : names.getFirst();
    }

    private static boolean isMarkedUnrated(String rawRating, String symbols) {
        if (!rawRating.isEmpty() && UNRATED_MARKERS.contains(rawRating.toUpperCase(Locale.ROOT))) {
            return true;
        }
        if (symbols.isEmpty()) return false;
        String upper = symbols.toUpperCase(Locale.ROOT);
        if (upper.equals("TRUE") || upper.equals("YES")) return true;
        for (String marker : UNRATED_MARKERS) {
            if (upper.contains(marker)) return true;
        }
        return false;
    }

    private static BigDecimal parseRating(String raw, int lineNum) {
        if (!raw.matches("(?:[0-9]|10)(?:\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException("Ratings CSV line " + lineNum + ": invalid rating '" + raw + "'");
        }
        BigDecimal value = new BigDecimal(raw);
        if (value.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("Ratings CSV line " + lineNum + ": rating exceeds 10: '" + raw + "'");
        }
        return value;
    }

    private static Map<Integer, FixtureScoreCheck> readFixtureMap(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                throw new IllegalArgumentException("fixture-map.csv: unexpected header");
            }
            String cleanHeader = header.replaceFirst("^\uFEFF", "").strip();
            if (!cleanHeader.equals(FplCollectorService.FIXTURE_MAP_HEADER)
                    && !cleanHeader.equals(FplCollectorService.FIXTURE_MAP_LEGACY_HEADER)) {
                throw new IllegalArgumentException("fixture-map.csv: unexpected header");
            }
            Map<Integer, FixtureScoreCheck> map = new LinkedHashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] f = line.split(",", -1);
                int fixtureId = Integer.parseInt(f[1].strip());
                Integer fplH = f[5].isBlank() ? null : Integer.parseInt(f[5].strip());
                Integer fplA = f[6].isBlank() ? null : Integer.parseInt(f[6].strip());
                map.put(fixtureId, new FixtureScoreCheck(fixtureId, fplH, fplA));
            }
            return map;
        }
    }

    private static <V> Map<FixturePlayerKey, V> filterByFixtures(
            Map<FixturePlayerKey, V> map, Set<Integer> selectedFixtureIds) {
        if (selectedFixtureIds.isEmpty()) {
            return map;
        }
        Map<FixturePlayerKey, V> filtered = new LinkedHashMap<>();
        for (var entry : map.entrySet()) {
            if (selectedFixtureIds.contains(entry.getKey().fixtureId())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private static Map<Integer, Set<Integer>> filterFixtureSets(
            Map<Integer, Set<Integer>> map, Set<Integer> selectedFixtureIds) {
        if (selectedFixtureIds.isEmpty()) {
            return map;
        }
        Map<Integer, Set<Integer>> filtered = new HashMap<>();
        for (var entry : map.entrySet()) {
            if (selectedFixtureIds.contains(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private static List<MergedRow> mergeManualStats(Path file, List<MergedRow> incoming) throws IOException {
        Map<FixturePlayerKey, MergedRow> merged = new LinkedHashMap<>();
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String header = reader.readLine();
                if (header == null || !header.replaceFirst("^\uFEFF", "").strip()
                        .equals(ManualMatchStatsCsvReader.HEADER)) {
                    throw new IllegalArgumentException("Existing manual-match-stats CSV has unexpected header: " + file);
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] f = line.split(",", -1);
                    if (f.length != 10) {
                        throw new IllegalArgumentException("Expected 10 columns in existing manual-match-stats CSV: " + file);
                    }
                    BigDecimal rating = f[4].isBlank() ? null : new BigDecimal(f[4].strip());
                    MergedRow row = new MergedRow(
                            Integer.parseInt(f[0].strip()), Integer.parseInt(f[1].strip()),
                            Integer.parseInt(f[2].strip()), f[3].strip(), rating,
                            Integer.parseInt(f[5].strip()), Integer.parseInt(f[6].strip()),
                            Integer.parseInt(f[7].strip()), Integer.parseInt(f[8].strip()),
                            Integer.parseInt(f[9].strip()));
                    merged.put(new FixturePlayerKey(row.fixtureId(), row.playerId()), row);
                }
            }
        }
        for (MergedRow row : incoming) {
            FixturePlayerKey key = new FixturePlayerKey(row.fixtureId(), row.playerId());
            MergedRow prev = merged.get(key);
            if (prev != null && !mergedRowsEqual(prev, row)) {
                throw new IllegalStateException("Conflict in " + file.getFileName()
                        + " for (fixture_id=" + row.fixtureId() + ", player_id=" + row.playerId()
                        + "): existing=" + prev + " vs new=" + row);
            }
            merged.put(key, row);
        }
        return new ArrayList<>(merged.values());
    }

    private static boolean mergedRowsEqual(MergedRow a, MergedRow b) {
        boolean ratingEq = (a.rating() == null && b.rating() == null)
                || (a.rating() != null && b.rating() != null && a.rating().compareTo(b.rating()) == 0);
        return a.season() == b.season()
                && a.fixtureId() == b.fixtureId()
                && a.playerId() == b.playerId()
                && Objects.equals(a.status(), b.status())
                && ratingEq
                && a.minutes() == b.minutes()
                && a.goals() == b.goals()
                && a.assists() == b.assists()
                && a.yellowCards() == b.yellowCards()
                && a.redCards() == b.redCards();
    }

    private static List<UnratedRow> mergeUnrated(Path file, List<UnratedRow> incoming) throws IOException {
        Map<FixturePlayerKey, UnratedRow> merged = new LinkedHashMap<>();
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String header = reader.readLine();
                if (header == null || !header.replaceFirst("^\uFEFF", "").strip().equals(UNRATED_HEADER)) {
                    throw new IllegalArgumentException("Existing confirmed-unrated.csv has unexpected header: " + file);
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] f = line.split(",", -1);
                    UnratedRow row = new UnratedRow(
                            Integer.parseInt(f[0].strip()), Integer.parseInt(f[1].strip()),
                            f[2].strip(), Integer.parseInt(f[3].strip()));
                    merged.put(new FixturePlayerKey(row.fixtureId(), row.playerId()), row);
                }
            }
        }
        for (UnratedRow row : incoming) {
            FixturePlayerKey key = new FixturePlayerKey(row.fixtureId(), row.playerId());
            UnratedRow prev = merged.get(key);
            if (prev != null && !prev.equals(row)) {
                throw new IllegalStateException("Conflict in confirmed-unrated.csv for (fixture_id="
                        + row.fixtureId() + ", player_id=" + row.playerId()
                        + "): existing=" + prev + " vs new=" + row);
            }
            merged.put(key, row);
        }
        return new ArrayList<>(merged.values());
    }

    private static List<MissingRow> mergeMissing(Path file, List<MissingRow> incoming, Set<Integer> selectedFixtureIds)
            throws IOException {
        List<MissingRow> merged = new ArrayList<>();
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String header = reader.readLine();
                if (header == null || !header.replaceFirst("^\uFEFF", "").strip().equals(MISSING_HEADER)) {
                    throw new IllegalArgumentException("Existing missing.csv has unexpected header: " + file);
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] f = line.split(",", -1);
                    int fixtureId = Integer.parseInt(f[0].strip());
                    if (selectedFixtureIds.contains(fixtureId)) {
                        continue;
                    }
                    merged.add(new MissingRow(fixtureId, f[1].strip(), Integer.parseInt(f[2].strip()),
                            f[3].strip(), f[4].strip()));
                }
            }
        }
        merged.addAll(incoming);
        return merged;
    }

    private static List<Anomaly> mergeAnomalies(Path file, List<Anomaly> incoming, Set<Integer> selectedFixtureIds)
            throws IOException {
        List<Anomaly> merged = new ArrayList<>();
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String header = reader.readLine();
                if (header == null || !header.replaceFirst("^\uFEFF", "").strip().equals(ANOMALIES_HEADER)) {
                    throw new IllegalArgumentException("Existing anomalies.csv has unexpected header: " + file);
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] f = line.split(",", -1);
                    int fixtureId = Integer.parseInt(f[0].strip());
                    if (selectedFixtureIds.contains(fixtureId)) {
                        continue;
                    }
                    Integer playerId = f[1].isBlank() ? null : Integer.valueOf(f[1].strip());
                    merged.add(new Anomaly(fixtureId, playerId, f[2].strip(), f[3].strip()));
                }
            }
        }
        merged.addAll(incoming);
        return merged;
    }

    private static Map<FixturePlayerKey, FplStatRow> readFplMatchStats(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null || !header.replaceFirst("^\uFEFF", "").strip()
                    .equals(FplCollectorService.MATCH_STATS_HEADER)) {
                throw new IllegalArgumentException("FPL match stats CSV: unexpected header");
            }
            Map<FixturePlayerKey, FplStatRow> map = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] f = line.split(",", -1);
                int fixtureId = Integer.parseInt(f[0].strip());
                int playerId = Integer.parseInt(f[1].strip());
                FplStatRow row = new FplStatRow(
                        fixtureId, playerId,
                        Integer.parseInt(f[2].strip()),
                        Integer.parseInt(f[3].strip()),
                        Integer.parseInt(f[4].strip()),
                        Integer.parseInt(f[5].strip()),
                        Integer.parseInt(f[6].strip()));
                map.put(new FixturePlayerKey(fixtureId, playerId), row);
            }
            return map;
        }
    }

    private static Map<Integer, Set<Integer>> readLineupSquad(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                throw new IllegalArgumentException("Lineup players CSV is empty: " + file);
            }
            String[] cols = header.replaceFirst("^\uFEFF", "").strip().split(",", -1);
            int fixtureIdx = -1;
            int playerIdx = -1;
            for (int i = 0; i < cols.length; i++) {
                String c = cols[i].strip().toLowerCase(Locale.ROOT);
                if (c.equals("fixture_id")) fixtureIdx = i;
                if (c.equals("player_id")) playerIdx = i;
            }
            if (fixtureIdx < 0 || playerIdx < 0) {
                throw new IllegalArgumentException("Lineup players CSV requires fixture_id and player_id");
            }
            Map<Integer, Set<Integer>> map = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] f = line.split(",", -1);
                map.computeIfAbsent(Integer.parseInt(f[fixtureIdx].strip()), ignored -> new HashSet<>())
                        .add(Integer.parseInt(f[playerIdx].strip()));
            }
            return map;
        }
    }

    private static Map<FixturePlayerKey, BenchCardRow> readBenchCards(Path file) throws IOException {
        if (!Files.exists(file)) {
            return Map.of();
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null || !header.replaceFirst("^\uFEFF", "").strip()
                    .equals(FplCollectorService.BENCH_CARDS_HEADER)) {
                throw new IllegalArgumentException("Invalid bench-cards.csv header in " + file);
            }
            Map<FixturePlayerKey, BenchCardRow> map = new LinkedHashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] f = line.split(",", -1);
                if (f.length != 4) {
                    throw new IllegalArgumentException("Expected 4 columns in bench-cards.csv");
                }
                int fixtureId = Integer.parseInt(f[0].strip());
                int playerId = Integer.parseInt(f[1].strip());
                int yc = Integer.parseInt(f[2].strip());
                int rc = Integer.parseInt(f[3].strip());
                map.put(new FixturePlayerKey(fixtureId, playerId), new BenchCardRow(fixtureId, playerId, yc, rc));
            }
            return map;
        }
    }

    private static void writeAnomalies(Path file, List<Anomaly> rows) throws IOException {
        StringBuilder out = new StringBuilder(ANOMALIES_HEADER).append('\n');
        for (Anomaly a : rows) {
            out.append(a.fixtureId()).append(',')
                    .append(a.playerId() == null ? "" : a.playerId()).append(',')
                    .append(a.code()).append(',')
                    .append(a.details().replace(',', ';')).append('\n');
        }
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
    }

    private static void writeMissing(Path file, List<MissingRow> rows) throws IOException {
        StringBuilder out = new StringBuilder(MISSING_HEADER).append('\n');
        for (MissingRow r : rows) {
            out.append(r.fixtureId()).append(',')
                    .append(r.club().replace(',', ' ')).append(',')
                    .append(r.playerId()).append(',')
                    .append(r.name().replace(',', ' ')).append(',')
                    .append(r.field()).append('\n');
        }
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
    }

    private static void writeUnrated(Path file, List<UnratedRow> rows) throws IOException {
        StringBuilder out = new StringBuilder(UNRATED_HEADER).append('\n');
        for (UnratedRow r : rows) {
            out.append(r.fixtureId()).append(',')
                    .append(r.playerId()).append(',')
                    .append(r.name().replace(',', ' ')).append(',')
                    .append(r.minutes()).append(",,\n");
        }
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
    }

    private static void writeManualMatchStats(Path file, List<MergedRow> rows) throws IOException {
        StringBuilder out = new StringBuilder(ManualMatchStatsCsvReader.HEADER).append('\n');
        for (MergedRow r : rows) {
            out.append(r.season()).append(',')
                    .append(r.fixtureId()).append(',')
                    .append(r.playerId()).append(',')
                    .append(r.status()).append(',')
                    .append(r.rating() == null ? "" : r.rating().toPlainString()).append(',')
                    .append(r.minutes()).append(',')
                    .append(r.goals()).append(',')
                    .append(r.assists()).append(',')
                    .append(r.yellowCards()).append(',')
                    .append(r.redCards()).append('\n');
        }
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
    }

    private record FixtureScoreCheck(int fixtureId, Integer fplHomeGoals, Integer fplAwayGoals) {
    }

    private record DbScore(Integer homeGoals, Integer awayGoals) {
    }

    private record FixturePlayerKey(int fixtureId, int playerId) {
    }

    private record FplStatRow(int fixtureId, int playerId, int minutes, int goals, int assists,
                              int yellowCards, int redCards) {
    }

    private record BenchCardRow(int fixtureId, int playerId, int yellowCards, int redCards) {
    }

    private record ResolvedPlayer(int playerId, String name) {
    }

    private record RatingEntry(int fixtureId, int playerId, String playerName, String clubName,
                               BigDecimal rating, boolean explicitlyUnrated) {
    }

    private record MergedRow(int season, int fixtureId, int playerId, String status, BigDecimal rating,
                             int minutes, int goals, int assists, int yellowCards, int redCards) {
    }

    private record UnratedRow(int fixtureId, int playerId, String name, int minutes) {
    }

    private record MissingRow(int fixtureId, String club, int playerId, String name, String field) {
    }

    public record Anomaly(int fixtureId, Integer playerId, String code, String details) {
    }

    public record Result(int gameweek, int fixturesIncluded, int fixturesBlocked,
                         int statRowsWritten, int unratedCount, int missingCount, int anomalyCount) {
    }
}
