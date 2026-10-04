-- MySQL 8.0.17+; additive/idempotent DDL. Run only after backup and explicit approval.
-- Requires existing accounts migration; no seed/football writes, no result publication.
-- DATETIME(6) values represent UTC; DDL auto-commits in MySQL.
SET time_zone = '+00:00';

-- UTC timestamps; football tables and account/session data are untouched.
CREATE TABLE IF NOT EXISTS fantasy_gameweeks (
    season INTEGER NOT NULL,
    gameweek INTEGER NOT NULL,
    deadline_utc DATETIME(6) NOT NULL,
    deadline_published_at DATETIME(6) NOT NULL,
    first_fixture_id INTEGER NOT NULL,
    first_kickoff_utc DATETIME(6) NOT NULL,
    workflow_status VARCHAR(20) NOT NULL,
    results_published_at DATETIME(6),
    revision INTEGER NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (season, gameweek),
    CHECK (season = 2026 AND gameweek BETWEEN 6 AND 38),
    CHECK (workflow_status IN ('OPEN','LOCKED','AWAITING_RESULTS','PUBLISHED')),
    CHECK ((workflow_status = 'PUBLISHED' AND results_published_at IS NOT NULL)
        OR (workflow_status <> 'PUBLISHED' AND results_published_at IS NULL)),
    CHECK (revision >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS fantasy_deadline_changes (
    season INTEGER NOT NULL,
    gameweek INTEGER NOT NULL,
    revision INTEGER NOT NULL,
    old_deadline_utc DATETIME(6),
    new_deadline_utc DATETIME(6) NOT NULL,
    changed_at DATETIME(6) NOT NULL,
    changed_by BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    PRIMARY KEY (season, gameweek, revision),
    FOREIGN KEY (season, gameweek) REFERENCES fantasy_gameweeks(season, gameweek),
    FOREIGN KEY (changed_by) REFERENCES accounts(id),
    CHECK (CHAR_LENGTH(TRIM(reason)) BETWEEN 3 AND 500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
