-- MySQL 8.0.17+ (not MariaDB). Apply manually after backup and schema review (Step 2).
-- Additive migration: creates fpl_provisional_player_stats and fpl_provisional_unmapped
-- for automatic post-match provisional FPL statistics. Does not ALTER or DROP any existing table.
-- DDL commits implicitly. All DATETIME(6) values are UTC; runtime SQL init must stay disabled.
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS fpl_provisional_player_stats (
    fixture_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    minutes INTEGER NOT NULL,
    goals INTEGER NOT NULL,
    assists INTEGER NOT NULL,
    yellow_cards INTEGER NOT NULL,
    red_cards INTEGER NOT NULL,
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
    fpl_status VARCHAR(20) NOT NULL,
    fetched_at DATETIME(6) NOT NULL,
    PRIMARY KEY (fixture_id, player_id),
    KEY fpps_season_fixture_idx (league_id, season_year, fixture_id),
    KEY fpps_player_idx (player_id),
    KEY fpps_status_idx (fixture_id, fpl_status),
    CONSTRAINT fpps_fixture_fk FOREIGN KEY (fixture_id) REFERENCES fixtures(id),
    CONSTRAINT fpps_player_fk FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT fpps_club_fk FOREIGN KEY (club_id) REFERENCES clubs(id),
    CONSTRAINT fpps_season_fk FOREIGN KEY (league_id, season_year) REFERENCES seasons(league_id, season_year),
    CONSTRAINT fpps_status_chk CHECK (fpl_status IN ('PROVISIONAL', 'FINAL_FPL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS fpl_provisional_unmapped (
    fixture_id INTEGER NOT NULL,
    fpl_element_id INTEGER NOT NULL,
    fpl_name VARCHAR(200) NOT NULL,
    club_id INTEGER NULL,
    club_name VARCHAR(200) NULL,
    minutes INTEGER NOT NULL,
    fetched_at DATETIME(6) NOT NULL,
    PRIMARY KEY (fixture_id, fpl_element_id),
    KEY fpu_fetched_idx (fetched_at),
    CONSTRAINT fpu_fixture_fk FOREIGN KEY (fixture_id) REFERENCES fixtures(id),
    CONSTRAINT fpu_club_fk FOREIGN KEY (club_id) REFERENCES clubs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
