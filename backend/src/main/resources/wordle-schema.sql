-- Local/test schema for Minigame Wordle (2026/27). Production initialization remains disabled.
CREATE TABLE IF NOT EXISTS wordle_name_overrides (
    player_id INTEGER PRIMARY KEY REFERENCES players(id),
    word VARCHAR(60) NOT NULL,
    note VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS wordle_selector (
    season INTEGER PRIMARY KEY CHECK (season = 2026),
    cycle_json TEXT NOT NULL
);
INSERT INTO wordle_selector (season, cycle_json)
SELECT 2026, '{"number":0,"remaining":[],"lastPlayerId":null}'
WHERE NOT EXISTS (SELECT 1 FROM wordle_selector WHERE season = 2026);

CREATE TABLE IF NOT EXISTS wordle_puzzles (
    id CHAR(36) PRIMARY KEY,
    puzzle_date DATE NOT NULL UNIQUE,
    puzzle_number INTEGER NOT NULL UNIQUE CHECK (puzzle_number >= 1),
    answer_player_id INTEGER NOT NULL REFERENCES players(id),
    answer_word VARCHAR(60) NOT NULL,
    answer_letters VARCHAR(60) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS wordle_games (
    id CHAR(36) PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    puzzle_id CHAR(36) NOT NULL REFERENCES wordle_puzzles(id),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PLAYING', 'WON', 'LOST')),
    hint_used BOOLEAN NOT NULL DEFAULT FALSE,
    started_at TIMESTAMP(6) NOT NULL,
    finished_at TIMESTAMP(6),
    UNIQUE (account_id, puzzle_id),
    CHECK ((status = 'PLAYING' AND finished_at IS NULL)
        OR (status IN ('WON', 'LOST') AND finished_at IS NOT NULL))
);

CREATE TABLE IF NOT EXISTS wordle_guesses (
    game_id CHAR(36) NOT NULL REFERENCES wordle_games(id),
    turn INTEGER NOT NULL CHECK (turn BETWEEN 1 AND 6),
    guessed_word VARCHAR(60) NOT NULL,
    guessed_letters VARCHAR(60) NOT NULL,
    player_id INTEGER NOT NULL,
    feedback VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (game_id, turn),
    UNIQUE (game_id, guessed_letters)
);

CREATE INDEX IF NOT EXISTS wordle_games_account_idx ON wordle_games(account_id, status);
CREATE INDEX IF NOT EXISTS wordle_games_puzzle_idx ON wordle_games(puzzle_id);
