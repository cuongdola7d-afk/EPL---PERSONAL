-- MySQL 8.0.17+ (not MariaDB). Apply manually after backup and schema review.
-- Additive migration: creates manual_fixture_player_extra_stats for FPL extended metrics
-- starting from GW11 season 2026/27. Does not modify GW1-GW10 or existing tables.
-- DDL commits implicitly. All DATETIME(6) values are UTC; runtime SQL init must stay disabled.
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS manual_fixture_player_extra_stats (
    fixture_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    starts INTEGER NULL,
    clean_sheets INTEGER NULL,
    goals_conceded INTEGER NULL,
    own_goals INTEGER NULL,
    penalties_saved INTEGER NULL,
    penalties_missed INTEGER NULL,
    saves INTEGER NULL,
    bonus INTEGER NULL,
    bps INTEGER NULL,
    clearances_blocks_interceptions INTEGER NULL,
    recoveries INTEGER NULL,
    tackles INTEGER NULL,
    defensive_contribution INTEGER NULL,
    expected_goals DECIMAL(5,2) NULL,
    expected_assists DECIMAL(5,2) NULL,
    expected_goal_involvements DECIMAL(5,2) NULL,
    expected_goals_conceded DECIMAL(5,2) NULL,
    influence DECIMAL(6,1) NULL,
    creativity DECIMAL(6,1) NULL,
    threat DECIMAL(6,1) NULL,
    ict_index DECIMAL(6,1) NULL,
    source VARCHAR(30) NOT NULL,
    fetched_at DATETIME(6) NOT NULL,
    PRIMARY KEY (fixture_id, player_id),
    KEY mfpes_season_club_idx (league_id, season_year, club_id),
    KEY mfpes_player_idx (player_id),
    CONSTRAINT mfpes_fixture_fk FOREIGN KEY (fixture_id) REFERENCES fixtures(id),
    CONSTRAINT mfpes_player_fk FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT mfpes_club_fk FOREIGN KEY (club_id) REFERENCES clubs(id),
    CONSTRAINT mfpes_season_fk FOREIGN KEY (league_id, season_year) REFERENCES seasons(league_id, season_year),
    CONSTRAINT mfpes_source_chk CHECK (source = 'FPL')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
