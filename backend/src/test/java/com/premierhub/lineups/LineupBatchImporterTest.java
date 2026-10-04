package com.premierhub.lineups;

import com.premierhub.service.FixtureEvidenceService;
import com.premierhub.service.FootballQueries;
import com.premierhub.service.MatchScoringService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirFactory;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LineupBatchImporterTest {
    @TempDir(factory = WorkspaceTempFactory.class) Path directory;
    static class WorkspaceTempFactory implements TempDirFactory {
        @Override public Path createTempDirectory(AnnotatedElementContext element, ExtensionContext context) throws java.io.IOException {
            Path root=Path.of("target","lineup-test-temp");
            Files.createDirectories(root);
            return Files.createTempDirectory(root,"csv-");
        }
    }
    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;
    private LineupBatchImporter importer;
    private FootballQueries queries;
    private String playersCsv;

    @BeforeEach void setup() throws Exception {
        database = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).addScript("schema.sql").build();
        jdbc = new JdbcTemplate(database);
        importer = new LineupBatchImporter(jdbc,new TransactionTemplate(new DataSourceTransactionManager(database)));
        queries = new FootballQueries(jdbc,new MatchScoringService(),new FixtureEvidenceService(jdbc,JsonMapper.builder().build()));
        jdbc.update("INSERT INTO seasons VALUES (39,2026),(39,2024)");
        jdbc.update("INSERT INTO clubs VALUES (1,'Home','City'),(2,'Away','City')");
        jdbc.update("INSERT INTO season_clubs VALUES (39,2026,1),(39,2026,2),(39,2024,1),(39,2024,2)");
        for (int fixture=100;fixture<=102;fixture++) {
            jdbc.update("INSERT INTO fixtures (id,league_id,season_year,home_club_id,away_club_id,gameweek,match_date,status,provider_status,payload_hash,synced_at) "
                    + "VALUES (?,39,2026,1,2,?,?,'FINISHED','FT',?,CURRENT_TIMESTAMP)",fixture,fixture-99,java.sql.Date.valueOf(LocalDate.of(2026,8,21).plusDays((fixture-100)*7)),"0".repeat(64));
        }
        var text = new StringBuilder();
        for (int club=1;club<=2;club++) for (int index=0;index<12;index++) {
            int id=(club-1)*100+index+1;
            String role=index<11 ? "STARTER" : club==1 ? "SUB_USED" : "SUB_UNUSED";
            jdbc.update("INSERT INTO players VALUES (?,?)",id,"Player "+id);
            jdbc.update("INSERT INTO player_season_stats (league_id,season_year,player_id,club_id,position) VALUES (39,2026,?,?,?)",id,club,index==0 ? "GOALKEEPER" : "MIDFIELDER");
            jdbc.update("INSERT INTO manual_player_memberships VALUES (39,2026,?,?,DATE '2026-08-01',NULL)",id,club);
            jdbc.update("INSERT INTO player_specific_positions VALUES (39,2026,?,?)",id,index==0 ? "GK" : "CM");
            jdbc.update("INSERT INTO player_eligible_positions VALUES (39,2026,?,?)",id,index==0 ? "GK" : "CM");
            // Bench player has the largest rating/minutes; neither identifies starters.
            jdbc.update("INSERT INTO manual_fixture_player_stats (fixture_id,player_id,league_id,season_year,club_id,participation_status,rating,minutes) VALUES (100,?,39,2026,?,?,?,?)",
                    id,club,role.equals("SUB_UNUSED") ? "DID_NOT_PLAY" : "PLAYED",index==11 && club==1 ? new java.math.BigDecimal("10.00") : null,index==11 && club==1 ? 90 : null);
            String matchPosition="";
            String line="",slot="";
            if (club==1 && index<11) {
                var lines=Formation.lines("4-2-3-1"); int offset=0;
                for (int row=0;row<lines.size();row++) {
                    if (index<offset+lines.get(row)) { line=String.valueOf(row);slot=String.valueOf(index-offset);matchPosition=row==0 ? "GK" : row==1 ? "CB" : row==4 ? "ST" : "CM"; break; }
                    offset+=lines.get(row);
                }
            }
            text.append(String.join(",","100",String.valueOf(club),"2026",String.valueOf(id),role,matchPosition,line,slot,"","","2026-10-04","existing-source-notes")).append('\n');
        }
        playersCsv=text.toString();
        write("clubs.csv",LineupBatchImporter.CLUBS,"1,2026,4-3-3,2026-10-04,1,5,2,4-2-3-1=1;4-3-3=1,100;101,OBSERVED,\n2,2026,3-4-2-1,2026-10-04,1,5,2,3-4-2-1=2,101;102,OBSERVED,\n");
        write("formations.csv",LineupBatchImporter.FORMATIONS,"100,1,2026,4-2-3-1,2026-10-04,https://www.sofascore.com/football/match/test-a\n101,1,2026,4-3-3,2026-10-04,https://www.sofascore.com/football/match/test-b\n101,2,2026,3-4-2-1,2026-10-04,https://www.sofascore.com/football/match/test-b\n102,2,2026,3-4-2-1,2026-10-04,https://www.sofascore.com/football/match/test-c\n");
        write("players.csv",LineupBatchImporter.PLAYERS,playersCsv);
    }
    @AfterEach void close() { database.shutdown(); }
    private void write(String name,String header,String content) throws Exception { Files.writeString(directory.resolve(name),header+"\n"+content); }

    @Test void importsIdempotentlyAndPrioritizesFixtureOverClubWithIndependentFormations() throws Exception {
        jdbc.update("INSERT INTO player_eligible_positions VALUES (39,2026,2,'RB')");
        var before=jdbc.queryForList("SELECT * FROM manual_fixture_player_stats ORDER BY player_id");
        var positions=jdbc.queryForList("SELECT * FROM player_eligible_positions ORDER BY player_id");
        var memberships=jdbc.queryForList("SELECT * FROM manual_player_memberships ORDER BY player_id");
        var first=importer.importDirectory(directory);
        assertEquals(2,first.clubChanges());assertEquals(4,first.verifiedFormations());assertEquals(24,first.playerChanges());
        var repeat=importer.importDirectory(directory);
        assertEquals(0,repeat.clubChanges());assertEquals(0,repeat.fixtureChanges());assertEquals(0,repeat.playerChanges());
        var detail=queries.matchDetail(100,2026).orElseThrow();
        assertEquals("4-2-3-1",detail.homeLineup().formation());assertEquals("FIXTURE",detail.homeLineup().formationSource());
        assertEquals("3-4-2-1",detail.awayLineup().formation());assertEquals("CLUB_DEFAULT",detail.awayLineup().formationSource());
        assertEquals("VERIFIED",detail.homeLineup().startersStatus());assertEquals("VERIFIED",detail.awayLineup().startersStatus());
        assertEquals(11,detail.homeLineup().players().stream().filter(player -> player.role().equals("STARTER")).count());
        assertEquals("SUB_USED",detail.homeLineup().players().stream().filter(player -> player.playerId()==12).findFirst().orElseThrow().role());
        assertEquals("CB",detail.homeLineup().players().get(1).matchPosition());assertEquals("CM",detail.homeLineup().players().get(1).seasonPosition());
        assertEquals(List.of("CM", "RB"), detail.homeLineup().players().get(1).seasonEligiblePositions());
        assertEquals(List.of("GK"), detail.homeLineup().players().getFirst().seasonEligiblePositions());
        assertEquals("4-3-3",jdbc.queryForObject("SELECT default_formation FROM club_season_formations WHERE club_id=1",String.class));
        assertEquals(before,jdbc.queryForList("SELECT * FROM manual_fixture_player_stats ORDER BY player_id"));
        assertEquals(positions,jdbc.queryForList("SELECT * FROM player_eligible_positions ORDER BY player_id"));
        assertEquals(memberships,jdbc.queryForList("SELECT * FROM manual_player_memberships ORDER BY player_id"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM club_season_formations WHERE season_year=2024",Integer.class));
    }

    @Test void refusesIncorrectCountsDuplicateSlotsAndParticipationWithoutPartialWrites() throws Exception {
        write("clubs.csv",LineupBatchImporter.CLUBS,"1,2026,4-2-3-1,2026-10-04,1,5,2,4-2-3-1=1;4-3-3=1,100;101,OBSERVED,\n2,2026,3-4-2-1,2026-10-04,1,5,2,3-4-2-1=2,101;102,OBSERVED,\n");
        assertThrows(IllegalArgumentException.class,()->importer.importDirectory(directory));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM club_season_formations",Integer.class));
        setupFiles();
        write("players.csv",LineupBatchImporter.PLAYERS,playersCsv.replace("100,1,2026,2,STARTER,CB,1,0", "100,1,2026,2,STARTER,CB,0,0"));
        assertThrows(IllegalArgumentException.class,()->importer.importDirectory(directory));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM fixture_lineups",Integer.class));
        write("players.csv",LineupBatchImporter.PLAYERS,playersCsv);
        jdbc.update("UPDATE manual_fixture_player_stats SET participation_status='DID_NOT_PLAY' WHERE player_id=1");
        assertThrows(IllegalArgumentException.class,()->importer.importDirectory(directory));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM club_season_formations",Integer.class));
    }

    private void setupFiles() throws Exception {
        write("clubs.csv",LineupBatchImporter.CLUBS,"1,2026,4-3-3,2026-10-04,1,5,2,4-2-3-1=1;4-3-3=1,100;101,OBSERVED,\n2,2026,3-4-2-1,2026-10-04,1,5,2,3-4-2-1=2,101;102,OBSERVED,\n");
    }

    @Test void missingRolesCannotBecomeVerifiedAndUnknownFormationsStayNull() throws Exception {
        write("clubs.csv",LineupBatchImporter.CLUBS,"1,2026,,2026-10-04,1,5,0,,,MISSING,\n2,2026,,2026-10-04,1,5,0,,,MISSING,\n");
        write("formations.csv",LineupBatchImporter.FORMATIONS,"");
        writePlayersWithoutCoordinates();
        importer.importDirectory(directory);
        var detail=queries.matchDetail(100,2026).orElseThrow();
        assertNull(detail.homeLineup().formation());assertEquals("MISSING",detail.homeLineup().formationSource());
        assertEquals("VERIFIED",detail.homeLineup().startersStatus());
        jdbc.update("DELETE FROM fixture_lineup_players WHERE player_id=1");
        assertEquals("INCOMPLETE",queries.matchDetail(100,2026).orElseThrow().homeLineup().startersStatus());
        assertEquals("MISSING",queries.matchDetail(102,2026).orElseThrow().homeLineup().startersStatus());
    }

    private void writePlayersWithoutCoordinates() throws Exception {
        // Remove coordinates along with the formation evidence; preserve roles.
        var plain=new StringBuilder();
        for (var line:playersCsv.split("\n")) {
            var row=line.split(",",-1);row[5]="";row[6]="";row[7]="";plain.append(String.join(",",row)).append('\n');
        }
        write("players.csv",LineupBatchImporter.PLAYERS,plain.toString());
    }

    @Test void userDefaultsKeepRealCountsAndRolesAndYieldToVerifiedFixtureFormation() throws Exception {
        String defaults = "1,2026,5-4-1,2026-10-04,1,5,0,,,USER,User decision 2026-10-04\n"
                + "2,2026,3-4-3,2026-10-04,1,5,0,,,USER,User decision 2026-10-04\n";
        write("clubs.csv",LineupBatchImporter.CLUBS,defaults);
        write("formations.csv",LineupBatchImporter.FORMATIONS,"");
        writePlayersWithoutCoordinates();
        var statistics = jdbc.queryForList("SELECT * FROM manual_fixture_player_stats ORDER BY player_id");
        importer.importDirectory(directory);
        var repeat = importer.importDirectory(directory);
        assertEquals(0,repeat.clubChanges());assertEquals(0,repeat.fixtureChanges());assertEquals(0,repeat.playerChanges());
        var detail = queries.matchDetail(100,2026).orElseThrow();
        assertEquals("5-4-1",detail.homeLineup().formation());
        assertEquals("3-4-3",detail.awayLineup().formation());
        assertEquals("CLUB_DEFAULT",detail.homeLineup().formationSource());
        assertEquals("User decision 2026-10-04",detail.homeLineup().formationSourceNote());
        assertEquals(0,detail.homeLineup().verifiedMatches());
        assertEquals("",detail.homeLineup().formationCounts());
        assertEquals("VERIFIED",detail.homeLineup().startersStatus());
        assertEquals("MISSING",queries.matchDetail(102,2026).orElseThrow().homeLineup().startersStatus());
        assertEquals("USER",jdbc.queryForObject("SELECT default_source FROM club_season_formations WHERE club_id=1",String.class));

        write("formations.csv",LineupBatchImporter.FORMATIONS,"100,1,2026,4-2-3-1,2026-10-04,https://www.sofascore.com/football/match/test-a\n");
        defaults = defaults.replace("5-4-1,2026-10-04,1,5,0,,,USER", "5-4-1,2026-10-04,1,5,1,4-2-3-1=1,100,USER");
        write("clubs.csv",LineupBatchImporter.CLUBS,defaults);
        importer.importDirectory(directory);
        detail = queries.matchDetail(100,2026).orElseThrow();
        assertEquals("4-2-3-1",detail.homeLineup().formation());
        assertEquals("FIXTURE",detail.homeLineup().formationSource());
        assertEquals("3-4-3",detail.awayLineup().formation());
        assertEquals("5-4-1",jdbc.queryForObject("SELECT default_formation FROM club_season_formations WHERE club_id=1",String.class));
        assertEquals(statistics,jdbc.queryForList("SELECT * FROM manual_fixture_player_stats ORDER BY player_id"));

        write("clubs.csv",LineupBatchImporter.CLUBS,defaults.replace("USER,User decision 2026-10-04", "USER,"));
        assertThrows(IllegalArgumentException.class,()->importer.importDirectory(directory));
        write("clubs.csv",LineupBatchImporter.CLUBS,defaults.replace("USER,User decision 2026-10-04", "OBSERVED,"));
        assertThrows(IllegalArgumentException.class,()->importer.importDirectory(directory));
    }

    @Test void frequencyUsesOnlyVerifiedMatchesAndLatestFixtureBreaksTies() {
        var observations=List.of(new FormationSummary.Observation(1,LocalDate.of(2026,8,21),"4-3-3"),
                new FormationSummary.Observation(2,LocalDate.of(2026,8,28),"3-4-2-1"));
        var summary=FormationSummary.from(observations);
        assertEquals("3-4-2-1",summary.defaultFormation());assertEquals(2,summary.verifiedMatches());
        assertEquals("3-4-2-1=1;4-3-3=1",summary.formationCounts());
        var frequent=new ArrayList<>(observations);frequent.add(new FormationSummary.Observation(3,LocalDate.of(2026,8,14),"4-3-3"));
        assertEquals("4-3-3",FormationSummary.from(frequent).defaultFormation());
        assertNull(FormationSummary.from(List.of()).defaultFormation());
        assertThrows(IllegalArgumentException.class,()->Formation.lines("4-3-4"));
        assertThrows(IllegalArgumentException.class,()->FormationSummary.from(List.of(observations.getFirst(),observations.getFirst())));
    }
}
