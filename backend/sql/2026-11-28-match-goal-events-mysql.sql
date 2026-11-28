-- MySQL 8.0.17+ (not MariaDB). Apply manually after backup and preflight review.
-- Additive: creates match_goal_events table; does not touch existing tables.
-- Requires fixtures.id INTEGER, clubs.id INTEGER, players.id INTEGER, InnoDB.
-- DDL commits implicitly. All DATETIME(6) values are UTC; runtime init must stay disabled.
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS match_goal_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fixture_id INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    player_id INTEGER NULL,
    `minute` INTEGER NULL,
    goal_type VARCHAR(16) NOT NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'FPL_LIVE',
    detected_at DATETIME(6) NOT NULL,
    seq INTEGER NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_match_goal_events (fixture_id, player_id, goal_type, seq),
    KEY idx_match_goal_events_fixture (fixture_id),
    CONSTRAINT mge_fixture_fk FOREIGN KEY (fixture_id) REFERENCES fixtures(id),
    CONSTRAINT mge_club_fk FOREIGN KEY (club_id) REFERENCES clubs(id),
    CONSTRAINT mge_player_fk FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT mge_goal_type CHECK (goal_type IN ('GOAL', 'OWN_GOAL', 'PENALTY')),
    CONSTRAINT mge_minute CHECK (`minute` IS NULL OR `minute` >= 0),
    CONSTRAINT mge_seq CHECK (seq >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
