-- Local/test schema. Production initialization remains disabled; no football data is seeded.
CREATE TABLE IF NOT EXISTS player_guess_selector (
    season INTEGER PRIMARY KEY CHECK (season = 2026),
    cycle_json TEXT NOT NULL
);
INSERT INTO player_guess_selector (season, cycle_json)
SELECT 2026, '{"number":0,"remaining":[],"lastPlayerId":null}'
WHERE NOT EXISTS (SELECT 1 FROM player_guess_selector WHERE season=2026);

CREATE TABLE IF NOT EXISTS player_guess_questions (
    id CHAR(36) PRIMARY KEY,
    season INTEGER NOT NULL CHECK (season = 2026),
    mode VARCHAR(8) NOT NULL CHECK (mode IN ('DAILY','PRACTICE')),
    question_date DATE NOT NULL,
    daily_date DATE,
    expires_at TIMESTAMP(6),
    snapshot_json TEXT NOT NULL,
    UNIQUE (season, daily_date),
    CHECK ((mode='DAILY' AND daily_date IS NOT NULL AND daily_date=question_date AND expires_at IS NOT NULL)
        OR (mode='PRACTICE' AND daily_date IS NULL AND expires_at IS NULL))
);
CREATE TABLE IF NOT EXISTS player_guess_games (
    id CHAR(36) PRIMARY KEY,
    game_sequence BIGINT AUTO_INCREMENT NOT NULL UNIQUE,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    question_id CHAR(36) NOT NULL REFERENCES player_guess_questions(id),
    season INTEGER NOT NULL CHECK (season=2026),
    mode VARCHAR(8) NOT NULL CHECK (mode IN ('DAILY','PRACTICE')),
    daily_date DATE,
    status VARCHAR(16) NOT NULL CHECK (status IN ('IN_PROGRESS','WON','LOST','EXPIRED')),
    current_score INTEGER NOT NULL CHECK (current_score BETWEEN 0 AND 100),
    final_score INTEGER CHECK (final_score BETWEEN 0 AND 100),
    guesses_used INTEGER NOT NULL CHECK (guesses_used BETWEEN 0 AND 3),
    revealed_hints INTEGER NOT NULL CHECK (revealed_hints BETWEEN 2 AND 8),
    version BIGINT NOT NULL CHECK (version>=0),
    created_at TIMESTAMP(6) NOT NULL,
    finished_at TIMESTAMP(6),
    UNIQUE (account_id, season, daily_date),
    CHECK ((mode='DAILY' AND daily_date IS NOT NULL) OR (mode='PRACTICE' AND daily_date IS NULL)),
    CHECK ((status='IN_PROGRESS' AND final_score IS NULL AND finished_at IS NULL AND guesses_used<3)
        OR (status='WON' AND final_score IS NOT NULL AND final_score=current_score AND finished_at IS NOT NULL AND guesses_used>=1)
        OR (status='LOST' AND final_score IS NOT NULL AND final_score=0 AND finished_at IS NOT NULL AND guesses_used=3)
        OR (status='EXPIRED' AND mode='DAILY' AND final_score IS NOT NULL AND final_score=0 AND finished_at IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS player_guess_practice_state (
    account_id BIGINT PRIMARY KEY REFERENCES accounts(id),
    game_id CHAR(36) NOT NULL REFERENCES player_guess_games(id)
);
CREATE TABLE IF NOT EXISTS player_guess_guesses (
    game_id CHAR(36) NOT NULL REFERENCES player_guess_games(id),
    guess_number INTEGER NOT NULL CHECK (guess_number BETWEEN 1 AND 3),
    player_id INTEGER NOT NULL,
    player_name VARCHAR(200) NOT NULL,
    correct BOOLEAN NOT NULL,
    penalty INTEGER NOT NULL CHECK (penalty IN (0,20)),
    auto_hint VARCHAR(24),
    PRIMARY KEY (game_id, guess_number),
    UNIQUE (game_id, player_id)
);
CREATE TABLE IF NOT EXISTS player_guess_actions (
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    action_id CHAR(36) NOT NULL,
    fingerprint VARCHAR(250) NOT NULL,
    game_id CHAR(36) NOT NULL REFERENCES player_guess_games(id),
    PRIMARY KEY (account_id, action_id)
);
CREATE TABLE IF NOT EXISTS player_guess_daily_results (
    game_id CHAR(36) PRIMARY KEY REFERENCES player_guess_games(id),
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    season INTEGER NOT NULL CHECK (season=2026),
    daily_date DATE NOT NULL,
    final_score INTEGER NOT NULL CHECK (final_score BETWEEN 0 AND 100),
    UNIQUE (account_id, season, daily_date)
);
CREATE INDEX IF NOT EXISTS player_guess_expiry_idx ON player_guess_games(mode,status,daily_date);
CREATE INDEX IF NOT EXISTS player_guess_owner_idx ON player_guess_games(account_id,mode,game_sequence);
