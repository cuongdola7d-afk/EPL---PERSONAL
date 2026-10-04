package com.premierhub.lineups;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class LineupBatchImporter {
    public static final String CLUBS = "club_id,season_year,default_formation,updated_on,scope_from_gw,scope_to_gw,verified_matches,formation_counts,fixture_ids";
    public static final String FORMATIONS = "fixture_id,club_id,season_year,formation,verified_on,source_url";
    public static final String PLAYERS = "fixture_id,club_id,season_year,player_id,role,match_position,row_index,slot_index,substitution_in_minute,substitution_out_minute,verified_on,source_note";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public LineupBatchImporter(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    public record Result(int clubs, int clubChanges, int verifiedFormations, int fixtureChanges, int players, int playerChanges) { }
    private record Team(int fixture, int club) { }
    private record Fixture(int week, LocalDate date, int home, int away) { }
    private record Observed(String formation, LocalDate date, String source) { }

    public Result importDirectory(Path directory) throws IOException {
        var clubs = csv(directory.resolve("clubs.csv"), CLUBS);
        var formations = csv(directory.resolve("formations.csv"), FORMATIONS);
        var players = csv(directory.resolve("players.csv"), PLAYERS);
        return transactions.execute(status -> apply(clubs, formations, players));
    }

    private Result apply(List<String[]> clubs, List<String[]> formations, List<String[]> players) {
        var seasonClubs = new HashSet<>(jdbc.queryForList("SELECT club_id FROM season_clubs "
                + "WHERE league_id=39 AND season_year=2026 ORDER BY club_id FOR UPDATE", Integer.class));
        var clubRows = new LinkedHashMap<Integer, String[]>();
        for (var row : clubs) {
            season(row[1]); int id = positive(row[0]);
            require(seasonClubs.contains(id) && clubRows.putIfAbsent(id, row) == null, "Unknown or repeated club_id=" + id);
            date(row[3]); int from = positive(row[4]), to = positive(row[5]);
            require(from <= to && to <= 38 && integer(row[6]) >= 0, "Invalid gameweek scope or count for club " + id);
            if (!row[2].isEmpty()) Formation.lines(row[2]);
        }
        require(!clubRows.isEmpty(), "clubs.csv is empty");
        var fixtures = new HashMap<Integer, Fixture>();
        var observations = new LinkedHashMap<Team, Observed>();
        for (var row : formations) {
            season(row[2]); var key = team(row, clubRows, fixtures);
            Formation.lines(row[3]); var verified = date(row[4]);
            require(!verified.isBefore(fixtures.get(key.fixture()).date()), "Formation verification predates fixture " + key);
            require(row[5].matches("https://(?:www\\.|api\\.)?sofascore\\.com/[^\\s]+"), "New formation evidence must use SofaScore: " + key);
            require(observations.putIfAbsent(key, new Observed(row[3], verified, row[5])) == null, "Repeated formation " + key);
        }
        var groups = new LinkedHashMap<Team, List<String[]>>();
        var seen = new HashSet<String>();
        for (var row : players) {
            season(row[2]); var key = team(row, clubRows, fixtures); int player = positive(row[3]);
            require(seen.add(key.fixture() + ":" + player), "Repeated player in fixture: " + player);
            require(Set.of("STARTER", "SUB_USED", "SUB_UNUSED").contains(row[4]), "Invalid role for player " + player);
            require(row[5].isEmpty() || row[5].matches("[A-Z][A-Z0-9_-]{0,19}"), "Invalid match position for player " + player);
            Integer line = optionalInteger(row[6]), slot = optionalInteger(row[7]);
            require((line == null) == (slot == null), "Both row_index and slot_index must be present or blank");
            if (line != null) {
                var evidence = observations.get(key);
                require(evidence != null && row[4].equals("STARTER") && !row[5].isEmpty(), "Pitch coordinates need verified fixture formation and match position");
                var lines = Formation.lines(evidence.formation());
                require(line >= 0 && line < lines.size() && slot >= 0 && slot < lines.get(line), "Invalid pitch slot for player " + player);
            }
            Integer entered = minute(row[8]), exited = minute(row[9]);
            require(entered == null || row[4].equals("SUB_USED"), "Substitution-in requires SUB_USED");
            require(exited == null || !row[4].equals("SUB_UNUSED"), "Unused substitute cannot have substitution-out");
            require(entered == null || exited == null || entered <= exited, "Substitution-out precedes substitution-in");
            var verified = date(row[10]);
            require(!verified.isBefore(fixtures.get(key.fixture()).date()) && !row[11].isBlank() && row[11].length() <= 500,
                    "Missing or invalid role evidence for player " + player);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }
        // Validate every group and summary before the first write.
        for (var entry : groups.entrySet()) {
            var key = entry.getKey(); var rows = entry.getValue();
            require(rows.stream().filter(row -> row[4].equals("STARTER")).count() == 11, "Need exactly 11 confirmed starters for " + key);
            var occupied = new HashSet<String>();
            for (var row : rows) {
                require(row[10].equals(rows.getFirst()[10]) && row[11].equals(rows.getFirst()[11]), "Team role evidence must use one date/source");
                if (!row[6].isEmpty()) require(occupied.add(row[6] + ":" + row[7]), "Two starters in the same verified pitch slot");
            }
            var savedStats = jdbc.queryForList("SELECT player_id,participation_status FROM manual_fixture_player_stats "
                    + "WHERE fixture_id=? AND club_id=? AND league_id=39 AND season_year=2026", key.fixture(), key.club());
            var statRoles = new HashMap<Integer, String>();
            savedStats.forEach(row -> statRoles.put(((Number) row.get("player_id")).intValue(), (String) row.get("participation_status")));
            require(statRoles.keySet().equals(rows.stream().map(row -> positive(row[3])).collect(java.util.stream.Collectors.toSet())),
                    "Matchday IDs do not match stored statistics for " + key);
            for (var row : rows) require(Objects.equals(statRoles.get(positive(row[3])), row[4].equals("SUB_UNUSED") ? "DID_NOT_PLAY" : "PLAYED"),
                    "Confirmed role conflicts with participation for player " + row[3]);
        }
        var summaries = new HashMap<Integer, FormationSummary>();
        for (var entry : clubRows.entrySet()) {
            int club = entry.getKey(); var row = entry.getValue();
            var summary = FormationSummary.from(observations.entrySet().stream().filter(item -> item.getKey().club() == club)
                    .map(item -> new FormationSummary.Observation(item.getKey().fixture(), fixtures.get(item.getKey().fixture()).date(), item.getValue().formation())).toList());
            require(Objects.equals(blank(row[2]), summary.defaultFormation()) && integer(row[6]) == summary.verifiedMatches()
                    && row[7].equals(summary.formationCounts()) && row[8].equals(summary.fixtureIds()),
                    "Default/counts/fixture scope must match verified observations for club " + club);
            for (var item : observations.entrySet()) if (item.getKey().club() == club) {
                int week = fixtures.get(item.getKey().fixture()).week();
                require(week >= positive(row[4]) && week <= positive(row[5]) && !date(row[3]).isBefore(item.getValue().date()), "Observation outside club sample " + club);
            }
            summaries.put(club, summary);
        }
        int clubChanges = 0, fixtureChanges = 0, playerChanges = 0;
        for (var entry : clubRows.entrySet()) {
            var row = entry.getValue(); var summary = summaries.get(entry.getKey());
            clubChanges += upsert("club_season_formations", "league_id=? AND season_year=? AND club_id=?", new Object[]{39,2026,entry.getKey()},
                    "league_id,season_year,club_id,default_formation,updated_on,scope_from_gw,scope_to_gw,verified_matches,formation_counts,fixture_ids",
                    new Object[]{39,2026,entry.getKey(),summary.defaultFormation(),Date.valueOf(date(row[3])),positive(row[4]),positive(row[5]),summary.verifiedMatches(),summary.formationCounts(),summary.fixtureIds()});
        }
        var allTeams = new HashSet<Team>(observations.keySet()); allTeams.addAll(groups.keySet());
        for (var key : allTeams) {
            var actual = observations.get(key); var roles = groups.get(key);
            // Preserve the other kind of evidence if this batch does not contain it.
            ensureFixture(key);
            if (actual != null) fixtureChanges += upsert("fixture_lineups", "fixture_id=? AND club_id=?", new Object[]{key.fixture(),key.club()},
                    "fixture_id,club_id,formation,formation_verified_on,formation_source",
                    new Object[]{key.fixture(),key.club(),actual.formation(),Date.valueOf(actual.date()),actual.source()});
            if (roles != null) {
                fixtureChanges += upsert("fixture_lineups", "fixture_id=? AND club_id=?", new Object[]{key.fixture(),key.club()},
                        "fixture_id,club_id,roles_verified_on,roles_source", new Object[]{key.fixture(),key.club(),Date.valueOf(date(roles.getFirst()[10])),roles.getFirst()[11]});
                for (var row : roles) playerChanges += upsert("fixture_lineup_players", "fixture_id=? AND club_id=? AND player_id=?", new Object[]{key.fixture(),key.club(),positive(row[3])},
                        "fixture_id,club_id,player_id,role,match_position,row_index,slot_index,substitution_in_minute,substitution_out_minute",
                        new Object[]{key.fixture(),key.club(),positive(row[3]),row[4],blank(row[5]),optionalInteger(row[6]),optionalInteger(row[7]),minute(row[8]),minute(row[9])});
            }
        }
        return new Result(clubs.size(), clubChanges, observations.size(), fixtureChanges, players.size(), playerChanges);
    }

    private Team team(String[] row, Map<Integer,String[]> clubs, Map<Integer,Fixture> fixtures) {
        var key = new Team(positive(row[0]),positive(row[1]));
        require(clubs.containsKey(key.club()), "Evidence club missing from clubs.csv");
        var fixture = fixtures.computeIfAbsent(key.fixture(), id -> jdbc.query("SELECT gameweek,match_date,home_club_id,away_club_id FROM fixtures "
                + "WHERE id=? AND league_id=39 AND season_year=2026 AND status='FINISHED'",
                (rs, index) -> new Fixture(rs.getInt(1),rs.getDate(2).toLocalDate(),rs.getInt(3),rs.getInt(4)), id).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unverified/missing 2026 fixture " + id)));
        require(key.club() == fixture.home() || key.club() == fixture.away(), "Club does not participate in fixture " + key);
        return key;
    }

    private void ensureFixture(Team key) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM fixture_lineups WHERE fixture_id=? AND club_id=?", Integer.class, key.fixture(), key.club()) == 0) {
            jdbc.update("INSERT INTO fixture_lineups (fixture_id,club_id,league_id,season_year) VALUES (?,?,39,2026)",key.fixture(),key.club());
        }
    }

    private int upsert(String table, String where, Object[] keys, String columns, Object[] incoming) {
        var names = columns.split(",");
        var saved = jdbc.queryForList("SELECT " + columns + " FROM " + table + " WHERE " + where,keys);
        if (!saved.isEmpty()) {
            boolean changed = false;
            for (int index=0; index<names.length; index++) {
                Object current = saved.getFirst().get(names[index]);
                require(current == null || incoming[index] != null, "Blank evidence would erase verified " + table + "." + names[index]);
                changed |= !Objects.equals(current,incoming[index]);
            }
            if (!changed) return 0;
            Object[] values = Arrays.copyOf(incoming,incoming.length+keys.length);
            System.arraycopy(keys,0,values,incoming.length,keys.length);
            jdbc.update("UPDATE " + table + " SET " + Arrays.stream(names).map(name -> name+"=?").collect(java.util.stream.Collectors.joining(",")) + " WHERE " + where,values);
        } else jdbc.update("INSERT INTO " + table + " (" + columns + ") VALUES (" + "?,".repeat(names.length-1) + "?)",incoming);
        return 1;
    }

    private static List<String[]> csv(Path file, String header) throws IOException {
        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        require(!lines.isEmpty() && lines.getFirst().replaceFirst("^\\uFEFF", "").equals(header), "Invalid CSV header: " + file);
        var result = new ArrayList<String[]>(); int count=header.split(",").length;
        for (int index=1; index<lines.size(); index++) {
            String line=lines.get(index); if (line.isBlank()) continue;
            String[] fields=line.split(",",-1);
            require(fields.length==count && !line.contains("\"") && !line.contains("\t"), "Expected " + count + " unquoted fields: " + file + ":" + (index+1));
            require(Arrays.stream(fields).noneMatch(field -> field.equals("NULL")), "Use blank cells for NULL: " + file);
            result.add(fields);
        }
        return result;
    }
    private static void season(String value) { require(value.equals("2026"), "Only season 2026/27 is supported"); }
    private static String blank(String value) { return value.isEmpty() ? null : value; }
    private static int integer(String value) { try { return Integer.parseInt(value); } catch (NumberFormatException error) { throw new IllegalArgumentException("Invalid integer: " + value,error); } }
    private static int positive(String value) { int number=integer(value); require(number>0,"Expected positive ID/number: "+value); return number; }
    private static Integer optionalInteger(String value) { return value.isEmpty() ? null : integer(value); }
    private static Integer minute(String value) { var number=optionalInteger(value); require(number==null || number>=0 && number<=130,"Invalid substitution minute"); return number; }
    private static LocalDate date(String value) {
        LocalDate date=LocalDate.parse(value);
        require(!date.isBefore(LocalDate.of(2026,7,1)) && !date.isAfter(LocalDate.of(2027,6,30)),"Verification date outside 2026/27");
        return date;
    }
    private static void require(boolean valid, String message) { if (!valid) throw new IllegalArgumentException(message); }
}
