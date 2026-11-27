package com.premierhub.minigame.wordle;

import com.premierhub.minigame.wordle.WordleData.AnswerView;
import com.premierhub.minigame.wordle.WordleData.Candidate;
import com.premierhub.minigame.wordle.WordleData.Cycle;
import com.premierhub.minigame.wordle.WordleData.Game;
import com.premierhub.minigame.wordle.WordleData.GuessRow;
import com.premierhub.minigame.wordle.WordleData.Puzzle;
import com.premierhub.minigame.wordle.WordleData.Status;
import com.premierhub.minigame.wordle.WordleStatsCalculator.GameOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "premierhub.wordle.enabled", havingValue = "true")
public class WordleRepository {
    private final JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();

    public WordleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void lockAccount(long accountId) {
        if (jdbc.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE", Long.class, accountId).isEmpty()) {
            throw new IllegalStateException("Session account unavailable");
        }
    }

    public Cycle lockCycle() {
        String raw = jdbc.queryForObject(
                "SELECT cycle_json FROM wordle_selector WHERE season=2026 FOR UPDATE",
                String.class);
        return json.readValue(raw, Cycle.class);
    }

    public void saveCycle(Cycle cycle) {
        jdbc.update("UPDATE wordle_selector SET cycle_json=? WHERE season=2026", json.writeValueAsString(cycle));
    }

    public List<Candidate> candidates(LocalDate date) {
        return jdbc.query("""
                SELECT p.id, p.name, c.id AS club_id, c.name AS club_name,
                       f.nationality, f.birth_date, f.shirt_number, f.fc27_overall,
                       pos.primary_position, ps.position AS broad_position,
                       o.word AS override_word,
                       (SELECT COUNT(*) FROM manual_player_memberships overlap
                        WHERE overlap.league_id=39 AND overlap.season_year=2026 AND overlap.player_id=p.id
                          AND overlap.start_date<=? AND (overlap.end_date IS NULL OR overlap.end_date>?)) AS membership_count
                FROM manual_player_memberships m
                JOIN season_clubs sc ON sc.league_id=m.league_id AND sc.season_year=m.season_year AND sc.club_id=m.club_id
                JOIN players p ON p.id=m.player_id
                JOIN clubs c ON c.id=m.club_id
                LEFT JOIN player_season_stats ps ON ps.league_id=m.league_id AND ps.season_year=m.season_year
                    AND ps.player_id=m.player_id AND ps.club_id=m.club_id
                LEFT JOIN player_season_profiles f ON f.league_id=m.league_id AND f.season_year=m.season_year
                    AND f.player_id=m.player_id AND f.club_id=m.club_id
                LEFT JOIN player_specific_positions pos ON pos.league_id=m.league_id AND pos.season_year=m.season_year
                    AND pos.player_id=m.player_id
                LEFT JOIN wordle_name_overrides o ON o.player_id=p.id
                WHERE m.league_id=39 AND m.season_year=2026
                  AND m.start_date<=? AND (m.end_date IS NULL OR m.end_date>?)
                ORDER BY p.id
                """, (rs, row) -> new Candidate(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("club_id"),
                rs.getString("club_name"),
                rs.getString("nationality"),
                date(rs, "birth_date"),
                rs.getObject("shirt_number", Integer.class),
                rs.getObject("fc27_overall", Integer.class),
                rs.getString("primary_position"),
                rs.getString("broad_position"),
                validatedOverride(rs.getInt("id"), rs.getString("override_word")),
                rs.getInt("membership_count")
        ), date, date, date, date);
    }

    private static String validatedOverride(int playerId, String overrideWord) {
        if (overrideWord == null) {
            return null;
        }
        if (!WordleNames.isValidOverrideWord(overrideWord)) {
            throw new IllegalStateException(
                    "Invalid wordle_name_overrides.word for player_id=" + playerId + ": '" + overrideWord + "'");
        }
        return overrideWord;
    }

    public Optional<Puzzle> findPuzzleByDate(LocalDate date) {
        return jdbc.query("SELECT * FROM wordle_puzzles WHERE puzzle_date=?",
                (rs, row) -> puzzle(rs), date).stream().findFirst();
    }

    public Optional<Puzzle> findPuzzleById(String id) {
        return jdbc.query("SELECT * FROM wordle_puzzles WHERE id=?",
                (rs, row) -> puzzle(rs), id).stream().findFirst();
    }

    public int maxPuzzleNumber() {
        Integer value = jdbc.queryForObject("SELECT COALESCE(MAX(puzzle_number), 0) FROM wordle_puzzles", Integer.class);
        return value == null ? 0 : value;
    }

    public void insertPuzzle(Puzzle puzzle) {
        jdbc.update("""
                INSERT INTO wordle_puzzles (id, puzzle_date, puzzle_number, answer_player_id,
                    answer_word, answer_letters, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, puzzle.id(), puzzle.puzzleDate(), puzzle.puzzleNumber(), puzzle.answerPlayerId(),
                puzzle.answerWord(), puzzle.answerLetters(), timestamp(puzzle.createdAt()));
    }

    public Optional<Game> findGame(long accountId, String puzzleId, boolean lock) {
        return jdbc.query("SELECT * FROM wordle_games WHERE account_id=? AND puzzle_id=?" + (lock ? " FOR UPDATE" : ""),
                (rs, row) -> game(rs), accountId, puzzleId).stream().findFirst();
    }

    public Optional<Game> findGameById(long accountId, String gameId, boolean lock) {
        return jdbc.query("SELECT * FROM wordle_games WHERE account_id=? AND id=?" + (lock ? " FOR UPDATE" : ""),
                (rs, row) -> game(rs), accountId, gameId).stream().findFirst();
    }

    public void insertGame(Game game) {
        jdbc.update("""
                INSERT INTO wordle_games (id, account_id, puzzle_id, status, hint_used, started_at, finished_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, game.id(), game.accountId(), game.puzzleId(), game.status().name(),
                game.hintUsed(), timestamp(game.startedAt()), timestamp(game.finishedAt()));
    }

    public void updateGame(Game game) {
        jdbc.update("""
                UPDATE wordle_games SET status=?, hint_used=?, finished_at=? WHERE id=?
                """, game.status().name(), game.hintUsed(), timestamp(game.finishedAt()), game.id());
    }

    public List<GuessRow> guesses(String gameId, LocalDate puzzleDate) {
        return jdbc.query("""
                SELECT g.turn, g.guessed_word, g.guessed_letters, g.player_id, g.feedback,
                       p.name AS player_name,
                       (SELECT c.name FROM manual_player_memberships m
                        JOIN clubs c ON c.id=m.club_id
                        WHERE m.league_id=39 AND m.season_year=2026 AND m.player_id=g.player_id
                          AND m.start_date<=? AND (m.end_date IS NULL OR m.end_date>?)
                        ORDER BY m.start_date DESC LIMIT 1) AS club_name
                FROM wordle_guesses g
                JOIN players p ON p.id=g.player_id
                WHERE g.game_id=?
                ORDER BY g.turn
                """, (rs, row) -> new GuessRow(
                rs.getInt("turn"),
                rs.getString("guessed_word"),
                rs.getString("guessed_letters"),
                rs.getInt("player_id"),
                rs.getString("feedback"),
                rs.getString("player_name"),
                rs.getString("club_name")
        ), puzzleDate, puzzleDate, gameId);
    }

    public void insertGuess(String gameId, int turn, String word, String letters,
                            int playerId, String feedback, Instant createdAt) {
        jdbc.update("""
                INSERT INTO wordle_guesses (game_id, turn, guessed_word, guessed_letters,
                    player_id, feedback, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, gameId, turn, word, letters, playerId, feedback, timestamp(createdAt));
    }

    public List<GameOutcome> outcomes(long accountId) {
        return jdbc.query("""
                SELECT p.puzzle_date, g.status,
                       (SELECT COUNT(*) FROM wordle_guesses gu WHERE gu.game_id=g.id) AS turns_used
                FROM wordle_games g
                JOIN wordle_puzzles p ON p.id=g.puzzle_id
                WHERE g.account_id=?
                ORDER BY p.puzzle_date
                """, (rs, row) -> new GameOutcome(
                date(rs, "puzzle_date"),
                Status.valueOf(rs.getString("status")),
                rs.getInt("turns_used")
        ), accountId);
    }

    public Optional<AnswerView> answerDetails(Puzzle puzzle) {
        LocalDate date = puzzle.puzzleDate();
        List<AnswerView> rows = jdbc.query("""
                SELECT p.id, p.name, c.id AS club_id, c.name AS club_name,
                       f.shirt_number, pos.primary_position, ps.position AS broad_position,
                       f.nationality, f.birth_date, f.fc27_overall
                FROM players p
                JOIN manual_player_memberships m ON m.player_id=p.id AND m.league_id=39 AND m.season_year=2026
                JOIN clubs c ON c.id=m.club_id
                LEFT JOIN player_season_stats ps ON ps.league_id=m.league_id AND ps.season_year=m.season_year
                    AND ps.player_id=m.player_id AND ps.club_id=m.club_id
                LEFT JOIN player_season_profiles f ON f.league_id=m.league_id AND f.season_year=m.season_year
                    AND f.player_id=m.player_id AND f.club_id=m.club_id
                LEFT JOIN player_specific_positions pos ON pos.league_id=m.league_id AND pos.season_year=m.season_year
                    AND pos.player_id=m.player_id
                WHERE p.id=?
                ORDER BY CASE WHEN m.start_date<=? AND (m.end_date IS NULL OR m.end_date>?) THEN 0 ELSE 1 END,
                         m.start_date DESC
                LIMIT 1
                """, (rs, row) -> {
            LocalDate birthDate = date(rs, "birth_date");
            Integer age = birthDate != null && !birthDate.isAfter(date)
                    ? Period.between(birthDate, date).getYears()
                    : null;
            String pos = rs.getString("primary_position");
            if (pos == null || pos.isBlank()) {
                pos = rs.getString("broad_position");
            }
            return new AnswerView(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("club_id"),
                    rs.getString("club_name"),
                    rs.getObject("shirt_number", Integer.class),
                    pos,
                    rs.getString("nationality"),
                    age,
                    rs.getObject("fc27_overall", Integer.class),
                    puzzle.answerWord()
            );
        }, puzzle.answerPlayerId(), date, date);
        return rows.stream().findFirst();
    }

    private static Puzzle puzzle(ResultSet rs) throws SQLException {
        return new Puzzle(
                rs.getString("id"),
                date(rs, "puzzle_date"),
                rs.getInt("puzzle_number"),
                rs.getInt("answer_player_id"),
                rs.getString("answer_word"),
                rs.getString("answer_letters"),
                instant(rs, "created_at")
        );
    }

    private static Game game(ResultSet rs) throws SQLException {
        return new Game(
                rs.getString("id"),
                rs.getLong("account_id"),
                rs.getString("puzzle_id"),
                Status.valueOf(rs.getString("status")),
                rs.getBoolean("hint_used"),
                instant(rs, "started_at"),
                instant(rs, "finished_at")
        );
    }

    private static LocalDate date(ResultSet rs, String column) throws SQLException {
        var value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime().toInstant(ZoneOffset.UTC);
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.valueOf(instant.atOffset(ZoneOffset.UTC).toLocalDateTime());
    }
}
