package com.premierhub.diagnostics;

import com.premierhub.fantasy.FantasyEntryService;
import com.premierhub.service.FootballQueries;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Operator-only SELECT workloads. No account/game creation, expiry, locks or data in the response. */
final class ReadLatencyProbe {
    private final JdbcTemplate jdbc;
    private final FootballQueries football;
    private final FantasyEntryService fantasy;
    private final TransactionTemplate readOnly;

    ReadLatencyProbe(JdbcTemplate jdbc, FootballQueries football, FantasyEntryService fantasy,
                     PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.football = football;
        this.fantasy = fantasy;
        readOnly = new TransactionTemplate(transactions);
        readOnly.setReadOnly(true);
        readOnly.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        readOnly.setTimeout(15);
    }
    boolean run(String workload) {
        switch (workload) {
            case "select1" -> jdbc.queryForObject("SELECT 1", Integer.class);
            case "clubs" -> football.clubs(2026, null);
            case "players" -> football.players(2026, null, null);
            case "fantasy" -> {
                return Boolean.TRUE.equals(readOnly.execute(status -> {
                    var sample = jdbc.query("SELECT account_id,gameweek FROM fantasy_entries WHERE season=2026 "
                            + "AND gameweek BETWEEN 6 AND 38 ORDER BY account_id,gameweek LIMIT 1",
                            (rs, row) -> new long[]{rs.getLong(1), rs.getInt(2)});
                    if (sample.isEmpty()) return false;
                    fantasy.read(sample.getFirst()[0], (int) sample.getFirst()[1]);
                    return true;
                }));
            }
            case "minigame" -> {
                return Boolean.TRUE.equals(readOnly.execute(status -> {
                    var sample = jdbc.query("SELECT account_id,id FROM player_guess_games WHERE season=2026 "
                            + "ORDER BY created_at DESC LIMIT 1",
                            (rs, row) -> new GameSample(rs.getLong(1), rs.getString(2)));
                    if (sample.isEmpty()) return false;
                    GameSample game = sample.getFirst();
                    // Mirrors stored progress reads without current()'s FOR UPDATE/expiry writes.
                    jdbc.queryForList("SELECT id FROM accounts WHERE id=?", game.owner());
                    var progress = jdbc.queryForList("SELECT * FROM player_guess_games "
                            + "WHERE account_id=? AND id=?", game.owner(), game.id());
                    if (progress.isEmpty()) return false;
                    jdbc.queryForList("SELECT * FROM player_guess_questions WHERE id=?", progress.getFirst().get("question_id"));
                    jdbc.queryForList("SELECT * FROM player_guess_guesses WHERE game_id=? ORDER BY guess_number", game.id());
                    return true;
                }));
            }
            default -> throw new IllegalArgumentException("Unknown read workload");
        }
        return true;
    }
    private record GameSample(long owner, String id) { }
}
