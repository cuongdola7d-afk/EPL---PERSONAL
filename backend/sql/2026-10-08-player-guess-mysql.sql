-- MySQL 8.0.17+ (not MariaDB). Apply manually after backup and schema review.
-- Additive: only seven player_guess_* tables and one empty selector; no sample answer/account.
-- Requires accounts.id BIGINT signed, InnoDB. IF NOT EXISTS does not repair incompatible tables.
-- DDL commits implicitly. All DATETIME(6) values are UTC; runtime init must stay disabled.
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS player_guess_selector (
    season INTEGER NOT NULL,
    cycle_json TEXT NOT NULL,
    PRIMARY KEY (season),
    CONSTRAINT pg_selector_season CHECK (season=2026),
    CONSTRAINT pg_selector_json CHECK (JSON_VALID(cycle_json))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS player_guess_questions (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    season INTEGER NOT NULL,
    mode VARCHAR(8) NOT NULL,
    question_date DATE NOT NULL,
    daily_date DATE NULL,
    expires_at DATETIME(6) NULL,
    snapshot_json MEDIUMTEXT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY pg_question_daily (season,daily_date),
    CONSTRAINT pg_question_season CHECK (season=2026),
    CONSTRAINT pg_question_mode CHECK (mode IN ('DAILY','PRACTICE')),
    CONSTRAINT pg_question_dates CHECK (
        (mode='DAILY' AND daily_date IS NOT NULL AND daily_date=question_date AND expires_at IS NOT NULL)
        OR (mode='PRACTICE' AND daily_date IS NULL AND expires_at IS NULL)),
    CONSTRAINT pg_question_json CHECK (JSON_VALID(snapshot_json))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS player_guess_games (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    game_sequence BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    question_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    season INTEGER NOT NULL,
    mode VARCHAR(8) NOT NULL,
    daily_date DATE NULL,
    status VARCHAR(16) NOT NULL,
    current_score INTEGER NOT NULL,
    final_score INTEGER NULL,
    guesses_used INTEGER NOT NULL,
    revealed_hints INTEGER NOT NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY pg_game_sequence (game_sequence),
    UNIQUE KEY pg_game_daily (account_id,season,daily_date),
    KEY player_guess_expiry_idx (mode,status,daily_date),
    KEY player_guess_owner_idx (account_id,mode,game_sequence),
    KEY pg_game_question_idx (question_id),
    CONSTRAINT pg_game_account_fk FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT pg_game_question_fk FOREIGN KEY (question_id) REFERENCES player_guess_questions(id),
    CONSTRAINT pg_game_season CHECK (season=2026),
    CONSTRAINT pg_game_mode CHECK (mode IN ('DAILY','PRACTICE')),
    CONSTRAINT pg_game_status CHECK (status IN ('IN_PROGRESS','WON','LOST','EXPIRED')),
    CONSTRAINT pg_game_score CHECK (current_score BETWEEN 0 AND 100),
    CONSTRAINT pg_game_final_score CHECK (final_score BETWEEN 0 AND 100),
    CONSTRAINT pg_game_guesses CHECK (guesses_used BETWEEN 0 AND 3),
    -- Keep persisted games from the previous two-hint rule readable; new games start at three.
    CONSTRAINT pg_game_hints CHECK (revealed_hints BETWEEN 2 AND 8),
    CONSTRAINT pg_game_version CHECK (version>=0),
    CONSTRAINT pg_game_dates CHECK (
        (mode='DAILY' AND daily_date IS NOT NULL) OR (mode='PRACTICE' AND daily_date IS NULL)),
    CONSTRAINT pg_game_result CHECK (
        (status='IN_PROGRESS' AND final_score IS NULL AND finished_at IS NULL AND guesses_used<3)
        OR (status='WON' AND final_score IS NOT NULL AND final_score=current_score AND finished_at IS NOT NULL AND guesses_used>=1)
        OR (status='LOST' AND final_score IS NOT NULL AND final_score=0 AND finished_at IS NOT NULL AND guesses_used=3)
        OR (status='EXPIRED' AND mode='DAILY' AND final_score IS NOT NULL AND final_score=0 AND finished_at IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS player_guess_practice_state (
    account_id BIGINT NOT NULL,
    game_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (account_id),
    KEY pg_practice_game_idx (game_id),
    CONSTRAINT pg_practice_account_fk FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT pg_practice_game_fk FOREIGN KEY (game_id) REFERENCES player_guess_games(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS player_guess_guesses (
    game_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    guess_number INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    player_name VARCHAR(200) NOT NULL,
    correct BOOLEAN NOT NULL,
    penalty INTEGER NOT NULL,
    auto_hint VARCHAR(24) NULL,
    PRIMARY KEY (game_id,guess_number),
    UNIQUE KEY pg_guess_player (game_id,player_id),
    CONSTRAINT pg_guess_game_fk FOREIGN KEY (game_id) REFERENCES player_guess_games(id),
    CONSTRAINT pg_guess_number CHECK (guess_number BETWEEN 1 AND 3),
    CONSTRAINT pg_guess_correct CHECK (correct IN (0,1)),
    CONSTRAINT pg_guess_penalty CHECK (penalty IN (0,20))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS player_guess_actions (
    account_id BIGINT NOT NULL,
    action_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    fingerprint VARCHAR(250) NOT NULL,
    game_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (account_id,action_id),
    KEY pg_action_game_idx (game_id),
    CONSTRAINT pg_action_account_fk FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT pg_action_game_fk FOREIGN KEY (game_id) REFERENCES player_guess_games(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS player_guess_daily_results (
    game_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    account_id BIGINT NOT NULL,
    season INTEGER NOT NULL,
    daily_date DATE NOT NULL,
    final_score INTEGER NOT NULL,
    PRIMARY KEY (game_id),
    UNIQUE KEY pg_result_daily (account_id,season,daily_date),
    CONSTRAINT pg_result_game_fk FOREIGN KEY (game_id) REFERENCES player_guess_games(id),
    CONSTRAINT pg_result_account_fk FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT pg_result_season CHECK (season=2026),
    CONSTRAINT pg_result_score CHECK (final_score BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

-- Preserve a nonempty cycle on rerun. Run one migration operator at a time.
INSERT INTO player_guess_selector (season,cycle_json)
SELECT 2026, '{"number":0,"remaining":[],"lastPlayerId":null}'
WHERE NOT EXISTS (SELECT 1 FROM player_guess_selector WHERE season=2026);
