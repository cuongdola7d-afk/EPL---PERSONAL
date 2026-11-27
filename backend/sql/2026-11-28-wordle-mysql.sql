-- MySQL 8.0.17+ (not MariaDB). Apply manually after backup and schema review.
-- Additive: creates five wordle_* tables and seeds one empty selector row; does not touch existing tables.
-- Requires accounts.id BIGINT signed, players.id INTEGER, InnoDB.
-- DDL commits implicitly. All DATETIME(6) values are UTC; runtime init must stay disabled.
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS wordle_name_overrides (
    player_id INTEGER NOT NULL,
    word VARCHAR(60) NOT NULL,
    note VARCHAR(255) NULL,
    PRIMARY KEY (player_id),
    CONSTRAINT wd_override_player_fk FOREIGN KEY (player_id) REFERENCES players(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS wordle_selector (
    season INTEGER NOT NULL,
    cycle_json TEXT NOT NULL,
    PRIMARY KEY (season),
    CONSTRAINT wd_selector_season CHECK (season = 2026),
    CONSTRAINT wd_selector_json CHECK (JSON_VALID(cycle_json))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS wordle_puzzles (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    puzzle_date DATE NOT NULL,
    puzzle_number INTEGER NOT NULL,
    answer_player_id INTEGER NOT NULL,
    answer_word VARCHAR(60) NOT NULL,
    answer_letters VARCHAR(60) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY wd_puzzle_date (puzzle_date),
    UNIQUE KEY wd_puzzle_number (puzzle_number),
    KEY wd_puzzle_player_idx (answer_player_id),
    CONSTRAINT wd_puzzle_player_fk FOREIGN KEY (answer_player_id) REFERENCES players(id),
    CONSTRAINT wd_puzzle_number_pos CHECK (puzzle_number >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS wordle_games (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    account_id BIGINT NOT NULL,
    puzzle_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(16) NOT NULL,
    hint_used TINYINT(1) NOT NULL DEFAULT 0,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY wd_game_account_puzzle (account_id, puzzle_id),
    KEY wordle_games_account_idx (account_id, status),
    KEY wordle_games_puzzle_idx (puzzle_id),
    CONSTRAINT wd_game_account_fk FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT wd_game_puzzle_fk FOREIGN KEY (puzzle_id) REFERENCES wordle_puzzles(id),
    CONSTRAINT wd_game_status CHECK (status IN ('PLAYING', 'WON', 'LOST')),
    CONSTRAINT wd_game_hint CHECK (hint_used IN (0, 1)),
    CONSTRAINT wd_game_finished CHECK (
        (status = 'PLAYING' AND finished_at IS NULL)
        OR (status IN ('WON', 'LOST') AND finished_at IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS wordle_guesses (
    game_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    turn INTEGER NOT NULL,
    guessed_word VARCHAR(60) NOT NULL,
    guessed_letters VARCHAR(60) NOT NULL,
    player_id INTEGER NOT NULL,
    feedback VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (game_id, turn),
    UNIQUE KEY wd_guess_letters (game_id, guessed_letters),
    CONSTRAINT wd_guess_game_fk FOREIGN KEY (game_id) REFERENCES wordle_games(id),
    CONSTRAINT wd_guess_turn CHECK (turn BETWEEN 1 AND 6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

-- Preserve a nonempty cycle on rerun.
INSERT INTO wordle_selector (season, cycle_json)
SELECT 2026, '{"number":0,"remaining":[],"lastPlayerId":null}'
WHERE NOT EXISTS (SELECT 1 FROM wordle_selector WHERE season = 2026);
