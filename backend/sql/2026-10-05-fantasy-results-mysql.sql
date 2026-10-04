-- MySQL 8.0.17+; additive and repeatable. Backup and explicit approval before production.
-- Requires football, account, GW and entry migrations. No seed/reset/source-statistic writes.
SET time_zone = '+00:00';
-- Explicit source evidence and published contest scores; raw statistics stay untouched.
CREATE TABLE IF NOT EXISTS fantasy_unrated_confirmations (
    fixture_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    source_ref VARCHAR(500) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    confirmed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (fixture_id,player_id),
    FOREIGN KEY (fixture_id,player_id) REFERENCES manual_fixture_player_stats(fixture_id,player_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS fantasy_fixture_confirmations (
    fixture_id INTEGER PRIMARY KEY,
    source_hash CHAR(64) NOT NULL,
    source_ref VARCHAR(500) NOT NULL,
    confirmed_at DATETIME(6) NOT NULL,
    FOREIGN KEY (fixture_id) REFERENCES fixtures(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS fantasy_result_publications (
    season INTEGER NOT NULL CHECK (season=2026),
    gameweek INTEGER NOT NULL CHECK (gameweek BETWEEN 6 AND 38),
    version INTEGER NOT NULL CHECK (version>=1),
    source_hash CHAR(64) NOT NULL,
    published_at DATETIME(6) NOT NULL,
    published_by BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('PUBLISH','RECALCULATE')),
    reason VARCHAR(500) NOT NULL,
    PRIMARY KEY (season,gameweek,version),
    FOREIGN KEY (season,gameweek) REFERENCES fantasy_gameweeks(season,gameweek),
    FOREIGN KEY (published_by) REFERENCES accounts(id),
    CHECK (CHAR_LENGTH(TRIM(reason)) BETWEEN 3 AND 500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS fantasy_team_results (
    account_id BIGINT NOT NULL,
    season INTEGER NOT NULL,
    gameweek INTEGER NOT NULL,
    version INTEGER NOT NULL,
    submitted_version BIGINT NOT NULL,
    total_points DECIMAL(12,2) NOT NULL CHECK (total_points>=0),
    breakdown_json TEXT NOT NULL,
    PRIMARY KEY (account_id,season,gameweek,version),
    FOREIGN KEY (season,gameweek,version) REFERENCES fantasy_result_publications(season,gameweek,version),
    FOREIGN KEY (account_id,season,gameweek) REFERENCES fantasy_entries(account_id,season,gameweek)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
