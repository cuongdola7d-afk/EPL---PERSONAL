package com.premierhub.fantasy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class FantasyResultRepository {
    public record Fixture(int id, int gameweek, LocalDate date, String status, int home, int away) { }
    public record Stat(int fixtureId, int playerId, int clubId, String status, BigDecimal rating,
                       Integer minutes, Integer goals, Integer assists, Integer yellowCards, Integer redCards) { }
    public record Unrated(int fixtureId, int playerId, String sourceRef, String reason) { }
    public record Evidence(int fixtureId, String sourceHash, String sourceRef) { }
    public record Role(int fixtureId, int clubId, int playerId, String role) { }
    public record Lineup(int fixtureId, int clubId, String formation, LocalDate formationVerified,
                         LocalDate rolesVerified, String formationSource, String rolesSource) { }
    public record Member(int playerId, int clubId, LocalDate start, LocalDate end) {
        public boolean applies(Fixture fixture) {
            return !fixture.date().isBefore(start) && (end==null || fixture.date().isBefore(end))
                    && (clubId==fixture.home() || clubId==fixture.away());
        }
    }
    public record Participant(long accountId, String formation, long submittedVersion, Instant submittedAt) { }
    public record Publication(int version, String sourceHash, Instant publishedAt, String action, String reason, long publishedBy) { }
    public record Standing(long accountId, String displayName, BigDecimal totalPoints, int gameweeksPlayed) { }
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public FantasyResultRepository(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc=jdbc; this.json=json; }
    public List<Fixture> fixtures(int gw, boolean lock) {
        return jdbc.query("SELECT id,gameweek,match_date,status,home_club_id,away_club_id FROM fixtures WHERE league_id=39 AND season_year=2026 AND gameweek=? ORDER BY id"+(lock?" FOR UPDATE":""),
                (r,n)->new Fixture(r.getInt(1),r.getInt(2),r.getDate(3).toLocalDate(),r.getString(4),r.getInt(5),r.getInt(6)),gw);
    }
    public Fixture fixture(int id) {
        return jdbc.query("SELECT id,gameweek,match_date,status,home_club_id,away_club_id FROM fixtures WHERE league_id=39 AND season_year=2026 AND id=?",
                (r,n)->new Fixture(r.getInt(1),r.getInt(2),r.getDate(3).toLocalDate(),r.getString(4),r.getInt(5),r.getInt(6)),id)
                .stream().findFirst().orElseThrow(()->new IllegalArgumentException("Unknown 2026 fixture "+id));
    }
    public List<Stat> stats(int id, boolean lock) {
        return jdbc.query("SELECT * FROM manual_fixture_player_stats WHERE fixture_id=? AND league_id=39 AND season_year=2026 ORDER BY player_id"+(lock?" FOR UPDATE":""),
                (r,n)->new Stat(id,r.getInt("player_id"),r.getInt("club_id"),r.getString("participation_status"),r.getBigDecimal("rating"),
                        r.getObject("minutes",Integer.class),r.getObject("goals",Integer.class),r.getObject("assists",Integer.class),
                        r.getObject("yellow_cards",Integer.class),r.getObject("red_cards",Integer.class)),id);
    }
    public List<Unrated> unrated(int id, boolean lock) {
        return jdbc.query("SELECT * FROM fantasy_unrated_confirmations WHERE fixture_id=? ORDER BY player_id"+(lock?" FOR UPDATE":""),
                (r,n)->new Unrated(id,r.getInt("player_id"),r.getString("source_ref"),r.getString("reason")),id);
    }
    public Optional<Evidence> evidence(int id, boolean lock) {
        return jdbc.query("SELECT * FROM fantasy_fixture_confirmations WHERE fixture_id=?"+(lock?" FOR UPDATE":""),
                (r,n)->new Evidence(id,r.getString("source_hash"),r.getString("source_ref")),id).stream().findFirst();
    }
    public List<Lineup> lineups(int id, boolean lock) {
        return jdbc.query("SELECT * FROM fixture_lineups WHERE fixture_id=? AND league_id=39 AND season_year=2026 ORDER BY club_id"+(lock?" FOR UPDATE":""),
                (r,n)->new Lineup(id,r.getInt("club_id"),r.getString("formation"),r.getObject("formation_verified_on",LocalDate.class),
                        r.getObject("roles_verified_on",LocalDate.class),r.getString("formation_source"),r.getString("roles_source")),id);
    }
    public List<Role> roles(int id, boolean lock) {
        return jdbc.query("SELECT * FROM fixture_lineup_players WHERE fixture_id=? ORDER BY club_id,player_id"+(lock?" FOR UPDATE":""),
                (r,n)->new Role(id,r.getInt("club_id"),r.getInt("player_id"),r.getString("role")),id);
    }
    public List<Member> members() {
        return jdbc.query("SELECT player_id,club_id,start_date,end_date FROM manual_player_memberships WHERE league_id=39 AND season_year=2026 ORDER BY player_id,club_id,start_date",
                (r,n)->new Member(r.getInt(1),r.getInt(2),r.getDate(3).toLocalDate(),r.getObject(4,LocalDate.class)));
    }
    public List<Participant> participants(int gw, boolean lock) {
        return jdbc.query("SELECT account_id,submitted_formation,submitted_version,submitted_at FROM fantasy_entries WHERE season=2026 AND gameweek=? AND submitted_at IS NOT NULL ORDER BY account_id"+(lock?" FOR UPDATE":""),
                (r,n)->new Participant(r.getLong(1),r.getString(2),r.getLong(3),r.getTimestamp(4).toLocalDateTime().toInstant(ZoneOffset.UTC)),gw);
    }
    public Optional<Publication> latest(int gw) {
        return jdbc.query("SELECT * FROM fantasy_result_publications WHERE season=2026 AND gameweek=? ORDER BY version DESC LIMIT 1",
                (r,n)->new Publication(r.getInt("version"),r.getString("source_hash"),r.getTimestamp("published_at").toLocalDateTime().toInstant(ZoneOffset.UTC),
                        r.getString("action"),r.getString("reason"),r.getLong("published_by")),gw).stream().findFirst();
    }
    public List<Publication> history(int gw) {
        return jdbc.query("SELECT * FROM fantasy_result_publications WHERE season=2026 AND gameweek=? ORDER BY version DESC",
                (r,n)->new Publication(r.getInt("version"),r.getString("source_hash"),r.getTimestamp("published_at").toLocalDateTime().toInstant(ZoneOffset.UTC),
                        r.getString("action"),r.getString("reason"),r.getLong("published_by")),gw);
    }
    public List<Standing> standings(Integer gameweek) {
        return jdbc.query("""
                SELECT r.account_id,a.display_name,SUM(r.total_points) AS total_points,COUNT(*) AS gameweeks_played
                FROM fantasy_team_results r JOIN accounts a ON a.id=r.account_id
                JOIN fantasy_gameweeks g ON g.season=r.season AND g.gameweek=r.gameweek
                WHERE r.season=2026 AND g.workflow_status='PUBLISHED' AND g.results_published_at IS NOT NULL
                  AND r.version=(SELECT MAX(p.version) FROM fantasy_result_publications p
                                 WHERE p.season=r.season AND p.gameweek=r.gameweek)
                """+(gameweek==null ? "" : " AND r.gameweek=?")+" GROUP BY r.account_id,a.display_name ORDER BY total_points DESC,r.account_id LIMIT 200",
                (r,n)->new Standing(r.getLong("account_id"),r.getString("display_name"),r.getBigDecimal("total_points"),r.getInt("gameweeks_played")),
                gameweek==null ? new Object[0] : new Object[]{gameweek});
    }
    public int publishedGameweeks() {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM fantasy_gameweeks g WHERE g.season=2026 AND g.workflow_status='PUBLISHED'
                AND g.results_published_at IS NOT NULL AND EXISTS
                  (SELECT 1 FROM fantasy_result_publications p WHERE p.season=g.season AND p.gameweek=g.gameweek)
                """,Integer.class);
    }
    public void publication(int gw, int version, String hash, Instant now, long actor, String action, String reason) {
        jdbc.update("INSERT INTO fantasy_result_publications VALUES (2026,?,?,?,?,?,?,?)",gw,version,hash,GameweekRepository.timestamp(now),actor,action,reason);
    }
    public void team(int gw,int version,Participant participant,FantasyResultService.TeamScore score) {
        jdbc.update("INSERT INTO fantasy_team_results VALUES (?,2026,?,?,?,?,?)",participant.accountId(),gw,version,participant.submittedVersion(),
                score.totalPoints(),json.writeValueAsString(score));
    }
    public Optional<FantasyResultService.TeamScore> team(long account,int gw,int version) {
        return jdbc.query("SELECT breakdown_json FROM fantasy_team_results WHERE account_id=? AND season=2026 AND gameweek=? AND version=?",
                (r,n)->json.readValue(r.getString(1),FantasyResultService.TeamScore.class),account,gw,version).stream().findFirst();
    }
    public void markPublished(int gw,Instant now) {
        jdbc.update("UPDATE fantasy_gameweeks SET workflow_status='PUBLISHED',results_published_at=?,updated_at=? WHERE season=2026 AND gameweek=?",
                GameweekRepository.timestamp(now),GameweekRepository.timestamp(now),gw);
    }
    public boolean saveUnrated(Unrated row,Instant now) {
        var old=unrated(row.fixtureId(),false).stream().filter(r->r.playerId()==row.playerId()).findFirst();
        if(old.isPresent() && old.get().equals(row)) return false;
        if(old.isPresent()) throw new IllegalArgumentException("Conflicting unrated evidence fixture="+row.fixtureId()+" player="+row.playerId());
        jdbc.update("INSERT INTO fantasy_unrated_confirmations VALUES (?,?,?,?,?)",row.fixtureId(),row.playerId(),row.sourceRef(),row.reason(),GameweekRepository.timestamp(now));
        return true;
    }
    public boolean confirm(int id,String hash,String source,Instant now) {
        var old=evidence(id,false);
        if(old.isPresent() && old.get().sourceHash().equals(hash) && old.get().sourceRef().equals(source)) return false;
        if(old.isPresent()) jdbc.update("UPDATE fantasy_fixture_confirmations SET source_hash=?,source_ref=?,confirmed_at=? WHERE fixture_id=?",hash,source,GameweekRepository.timestamp(now),id);
        else jdbc.update("INSERT INTO fantasy_fixture_confirmations VALUES (?,?,?,?)",id,hash,source,GameweekRepository.timestamp(now));
        return true;
    }
}
