package com.premierhub.minigame;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import static com.premierhub.minigame.PlayerGuessData.*;
import static com.premierhub.minigame.PlayerGuessRules.*;

@Repository
@ConditionalOnProperty(name = "premierhub.minigame.enabled", havingValue = "true")
public class PlayerGuessRepository {
    private final JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();

    public PlayerGuessRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void lockAccount(long accountId) {
        if (jdbc.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE", Long.class, accountId).isEmpty())
            throw new IllegalStateException("Session account unavailable");
    }

    public List<Candidate> candidates(LocalDate date) {
        // No goals, assists, appearances or season totals: profiles and dated memberships only.
        return jdbc.query("""
                SELECT p.id,p.name,c.id AS club_id,c.name AS club_name,
                       f.nationality,f.birth_date,f.height_cm,f.preferred_foot,f.shirt_number,f.fc27_overall,
                       pos.primary_position,
                       (SELECT COUNT(*) FROM manual_player_memberships overlap
                        WHERE overlap.league_id=39 AND overlap.season_year=2026 AND overlap.player_id=p.id
                        AND overlap.start_date<=? AND (overlap.end_date IS NULL OR overlap.end_date>?)) AS membership_count
                FROM manual_player_memberships m
                JOIN season_clubs sc ON sc.league_id=m.league_id AND sc.season_year=m.season_year AND sc.club_id=m.club_id
                JOIN players p ON p.id=m.player_id JOIN clubs c ON c.id=m.club_id
                LEFT JOIN player_season_profiles f ON f.league_id=m.league_id AND f.season_year=m.season_year
                    AND f.player_id=m.player_id AND f.club_id=m.club_id
                LEFT JOIN player_specific_positions pos ON pos.league_id=m.league_id AND pos.season_year=m.season_year
                    AND pos.player_id=m.player_id
                WHERE m.league_id=39 AND m.season_year=2026 AND m.start_date<=?
                    AND (m.end_date IS NULL OR m.end_date>?) ORDER BY p.id
                """, (rs, row) -> new Candidate(rs.getInt("id"), rs.getString("name"), rs.getInt("club_id"),
                rs.getString("club_name"), rs.getString("nationality"), date(rs, "birth_date"),
                rs.getObject("height_cm", Integer.class), rs.getString("preferred_foot"),
                rs.getObject("shirt_number", Integer.class), rs.getObject("fc27_overall", Integer.class),
                rs.getString("primary_position"), rs.getInt("membership_count")), date, date, date, date);
    }

    public Cycle lockCycle() {
        return json.readValue(jdbc.queryForObject("SELECT cycle_json FROM player_guess_selector WHERE season=2026 FOR UPDATE",
                String.class), Cycle.class);
    }

    public void saveCycle(Cycle cycle) {
        jdbc.update("UPDATE player_guess_selector SET cycle_json=? WHERE season=2026", json.writeValueAsString(cycle));
    }

    public Optional<Question> daily(LocalDate date) {
        return jdbc.query("SELECT * FROM player_guess_questions WHERE season=2026 AND daily_date=?",
                (rs, row) -> question(rs), date).stream().findFirst();
    }

    public Question question(String id) {
        return jdbc.query("SELECT * FROM player_guess_questions WHERE id=?", (rs, row) -> question(rs), id)
                .stream().findFirst().orElseThrow();
    }

    public void insertQuestion(Question question) {
        jdbc.update("""
                INSERT INTO player_guess_questions (id,season,mode,question_date,daily_date,expires_at,snapshot_json)
                VALUES (?,2026,?,?,?,?,?)
                """, question.id(), question.mode().name(), question.date(),
                question.mode() == Mode.DAILY ? question.date() : null, timestamp(question.expiresAt()),
                json.writeValueAsString(question.snapshot()));
    }

    public Optional<Game> game(long owner, String id, boolean lock) {
        return jdbc.query("SELECT * FROM player_guess_games WHERE account_id=? AND id=?" + (lock ? " FOR UPDATE" : ""),
                (rs, row) -> game(rs), owner, id).stream().findFirst();
    }

    public Optional<Game> dailyGame(long owner, LocalDate date) {
        return jdbc.query("SELECT * FROM player_guess_games WHERE account_id=? AND season=2026 AND daily_date=? FOR UPDATE",
                (rs, row) -> game(rs), owner, date).stream().findFirst();
    }

    public Optional<Game> practice(long owner) {
        return jdbc.query("""
                SELECT g.* FROM player_guess_games g JOIN player_guess_practice_state s ON s.game_id=g.id
                WHERE s.account_id=? FOR UPDATE
                """, (rs, row) -> game(rs), owner).stream().findFirst();
    }

    public List<Integer> recentPractice(long owner) {
        return jdbc.query("""
                SELECT q.snapshot_json FROM player_guess_games g JOIN player_guess_questions q ON q.id=g.question_id
                WHERE g.account_id=? AND g.mode='PRACTICE' ORDER BY g.game_sequence DESC LIMIT 5
                """, (rs, row) -> json.readValue(rs.getString(1), Snapshot.class).playerId(), owner);
    }

    public void pointPractice(long owner, String id) {
        if (jdbc.update("UPDATE player_guess_practice_state SET game_id=? WHERE account_id=?", id, owner) == 0)
            jdbc.update("INSERT INTO player_guess_practice_state (account_id,game_id) VALUES (?,?)", owner, id);
    }

    public void insertGame(Game game) {
        jdbc.update("""
                INSERT INTO player_guess_games (id,account_id,question_id,season,mode,daily_date,status,current_score,
                    final_score,guesses_used,revealed_hints,version,created_at,finished_at)
                VALUES (?,?,?,2026,?,?,?,?,?,?,?,?,?,?)
                """, game.id(), game.accountId(), game.questionId(), game.mode().name(), game.dailyDate(),
                game.state().status().name(), game.state().score(), game.state().finalScore(), game.state().guessesUsed(),
                game.state().revealedHints(), game.version(), timestamp(game.createdAt()), timestamp(game.finishedAt()));
    }

    public void updateGame(Game game) {
        jdbc.update("""
                UPDATE player_guess_games SET status=?,current_score=?,final_score=?,guesses_used=?,revealed_hints=?,
                    version=?,finished_at=? WHERE id=?
                """, game.state().status().name(), game.state().score(), game.state().finalScore(), game.state().guessesUsed(),
                game.state().revealedHints(), game.version(), timestamp(game.finishedAt()), game.id());
    }

    public List<Guess> guesses(String id) {
        return jdbc.query("SELECT * FROM player_guess_guesses WHERE game_id=? ORDER BY guess_number",
                (rs, row) -> new Guess(rs.getInt("guess_number"), rs.getInt("player_id"), rs.getString("player_name"),
                rs.getBoolean("correct"), rs.getInt("penalty"), rs.getString("auto_hint")), id);
    }

    public void insertGuess(String gameId, Guess guess) {
        jdbc.update("""
                INSERT INTO player_guess_guesses (game_id,guess_number,player_id,player_name,correct,penalty,auto_hint)
                VALUES (?,?,?,?,?,?,?)
                """, gameId, guess.number(), guess.playerId(), guess.name(), guess.correct(), guess.penalty(), guess.autoRevealedHint());
    }

    public Optional<Action> action(long owner, String actionId) {
        return jdbc.query("SELECT fingerprint,game_id FROM player_guess_actions WHERE account_id=? AND action_id=?",
                (rs, row) -> new Action(rs.getString(1), rs.getString(2)), owner, actionId).stream().findFirst();
    }

    public void insertAction(long owner, String actionId, String fingerprint, String gameId) {
        jdbc.update("INSERT INTO player_guess_actions (account_id,action_id,fingerprint,game_id) VALUES (?,?,?,?)",
                owner, actionId, fingerprint, gameId);
    }

    public void recordResult(Game game) {
        if (game.mode() != Mode.DAILY || game.state().finalScore() == null) return;
        // Caller holds the game row lock. Completion and the ledger are in the same transaction.
        if (jdbc.queryForObject("SELECT COUNT(*) FROM player_guess_daily_results WHERE game_id=?", Integer.class, game.id()) == 0)
            jdbc.update("INSERT INTO player_guess_daily_results (game_id,account_id,season,daily_date,final_score) VALUES (?,?,2026,?,?)",
                    game.id(), game.accountId(), game.dailyDate(), game.state().finalScore());
    }

    public List<Long> expiredOwners(LocalDate today) {
        return jdbc.queryForList("""
                SELECT DISTINCT account_id FROM player_guess_games
                WHERE mode='DAILY' AND status='IN_PROGRESS' AND daily_date<? ORDER BY account_id
                """, Long.class, today);
    }

    public List<Game> expiredGames(LocalDate today, long owner) {
        return jdbc.query("""
                SELECT * FROM player_guess_games WHERE mode='DAILY' AND status='IN_PROGRESS' AND daily_date<?
                    AND account_id=? ORDER BY id FOR UPDATE
                """, (rs, row) -> game(rs), today, owner);
    }

    public List<Game> history(long owner, int offset, int limit) {
        return jdbc.query("""
                SELECT * FROM player_guess_games WHERE account_id=? AND mode='DAILY'
                ORDER BY daily_date DESC LIMIT ? OFFSET ?
                """, (rs, row) -> game(rs), owner, limit, offset);
    }

    public List<Ranked> standings(int offset, int limit) {
        return jdbc.query("""
                SELECT totals.*,RANK() OVER (ORDER BY total_points DESC) AS player_rank,
                    COUNT(*) OVER (PARTITION BY total_points) AS tied_count
                FROM (SELECT a.id AS account_id,a.display_name,SUM(r.final_score) AS total_points,COUNT(*) AS daily_games
                    FROM player_guess_daily_results r JOIN accounts a ON a.id=r.account_id
                    WHERE r.season=2026 GROUP BY a.id,a.display_name) totals
                ORDER BY total_points DESC,account_id LIMIT ? OFFSET ?
                """, (rs, row) -> new Ranked(rs.getLong("player_rank"), rs.getLong("account_id"), rs.getString("display_name"),
                rs.getLong("total_points"), rs.getLong("daily_games"), rs.getLong("tied_count") > 1), limit, offset);
    }

    private Question question(ResultSet rs) throws SQLException {
        return new Question(rs.getString("id"), Mode.valueOf(rs.getString("mode")), date(rs, "question_date"),
                instant(rs, "expires_at"), json.readValue(rs.getString("snapshot_json"), Snapshot.class));
    }

    private static Game game(ResultSet rs) throws SQLException {
        return new Game(rs.getString("id"), rs.getLong("account_id"), rs.getString("question_id"),
                Mode.valueOf(rs.getString("mode")), date(rs, "daily_date"), new State(Status.valueOf(rs.getString("status")),
                rs.getInt("current_score"), rs.getInt("guesses_used"), rs.getInt("revealed_hints"),
                rs.getObject("final_score", Integer.class)), rs.getLong("version"), instant(rs, "created_at"), instant(rs, "finished_at"));
    }

    private static LocalDate date(ResultSet rs, String column) throws SQLException {
        var value = rs.getDate(column); return value == null ? null : value.toLocalDate();
    }
    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column); return value == null ? null : value.toLocalDateTime().toInstant(ZoneOffset.UTC);
    }
    public static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.valueOf(instant.atOffset(ZoneOffset.UTC).toLocalDateTime());
    }
}
