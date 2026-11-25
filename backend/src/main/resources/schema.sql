CREATE TABLE IF NOT EXISTS seasons (
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    PRIMARY KEY (league_id, season_year)
);

CREATE TABLE IF NOT EXISTS clubs (
    id INTEGER PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    city VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS season_clubs (
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    PRIMARY KEY (league_id, season_year, club_id),
    FOREIGN KEY (league_id, season_year) REFERENCES seasons(league_id, season_year),
    FOREIGN KEY (club_id) REFERENCES clubs(id)
);

-- Current club information verified on a date; not a fixture or historical manager record.
CREATE TABLE IF NOT EXISTS club_season_information (
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    manager_name VARCHAR(200),
    manager_status VARCHAR(20),
    stadium_name VARCHAR(200),
    verified_on DATE NOT NULL,
    CONSTRAINT club_info_season CHECK (season_year = 2026),
    CONSTRAINT club_info_status CHECK ((CASE manager_status
        WHEN 'PERMANENT' THEN 1 WHEN 'INTERIM' THEN 1
        ELSE CASE WHEN manager_status IS NULL THEN 1 ELSE 0 END END) = 1),
    CONSTRAINT club_info_manager CHECK ((manager_name IS NULL AND manager_status IS NULL)
        OR (manager_name IS NOT NULL AND manager_status IS NOT NULL)),
    PRIMARY KEY (league_id, season_year, club_id),
    FOREIGN KEY (league_id, season_year, club_id) REFERENCES season_clubs(league_id, season_year, club_id)
);

CREATE TABLE IF NOT EXISTS players (
    id INTEGER PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS player_season_stats (
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    position VARCHAR(30),
    appearances INTEGER,
    minutes INTEGER,
    goals INTEGER,
    assists INTEGER,
    PRIMARY KEY (league_id, season_year, player_id, club_id),
    FOREIGN KEY (league_id, season_year) REFERENCES seasons(league_id, season_year),
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (club_id) REFERENCES clubs(id)
);

-- Verified 2026/27 roles are per player and season, independent of club transfers
-- and of the broad position and statistics in player_season_stats.
CREATE TABLE IF NOT EXISTS player_specific_positions (
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    primary_position VARCHAR(3) NOT NULL,
    PRIMARY KEY (league_id, season_year, player_id),
    FOREIGN KEY (league_id, season_year) REFERENCES seasons(league_id, season_year),
    FOREIGN KEY (player_id) REFERENCES players(id),
    CHECK (season_year = 2026)
);

CREATE TABLE IF NOT EXISTS player_eligible_positions (
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    position_code VARCHAR(3) NOT NULL,
    PRIMARY KEY (league_id, season_year, player_id, position_code),
    FOREIGN KEY (league_id, season_year, player_id)
        REFERENCES player_specific_positions(league_id, season_year, player_id),
    CHECK (season_year = 2026)
);

-- Half-open membership interval [start_date, end_date); NULL end_date is open-ended.
CREATE TABLE IF NOT EXISTS manual_player_memberships (
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    PRIMARY KEY (league_id, season_year, player_id, club_id, start_date),
    FOREIGN KEY (league_id, season_year, player_id, club_id)
        REFERENCES player_season_stats(league_id, season_year, player_id, club_id)
);

CREATE TABLE IF NOT EXISTS player_season_profiles (
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    nationality VARCHAR(100),
    birth_date DATE,
    height_cm INTEGER,
    preferred_foot VARCHAR(5),
    shirt_number INTEGER,
    fc27_overall INTEGER,
    PRIMARY KEY (league_id, season_year, player_id, club_id),
    FOREIGN KEY (league_id, season_year, player_id, club_id)
        REFERENCES player_season_stats(league_id, season_year, player_id, club_id),
    CHECK (height_cm IS NULL OR height_cm BETWEEN 100 AND 250),
    CHECK (shirt_number IS NULL OR shirt_number BETWEEN 1 AND 99),
    CHECK (fc27_overall IS NULL OR fc27_overall BETWEEN 1 AND 99)
);

CREATE TABLE IF NOT EXISTS fixtures (
    id INTEGER PRIMARY KEY,
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    home_club_id INTEGER NOT NULL,
    away_club_id INTEGER NOT NULL,
    gameweek INTEGER NOT NULL,
    match_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_status VARCHAR(12) NOT NULL,
    home_goals INTEGER,
    away_goals INTEGER,
    payload_hash CHAR(64) NOT NULL,
    synced_at TIMESTAMP NOT NULL,
    FOREIGN KEY (league_id, season_year) REFERENCES seasons(league_id, season_year),
    FOREIGN KEY (home_club_id) REFERENCES clubs(id),
    FOREIGN KEY (away_club_id) REFERENCES clubs(id)
);

CREATE TABLE IF NOT EXISTS standings (
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    position INTEGER NOT NULL,
    played INTEGER NOT NULL,
    won INTEGER NOT NULL,
    drawn INTEGER NOT NULL,
    lost INTEGER NOT NULL,
    goals_for INTEGER NOT NULL,
    goals_against INTEGER NOT NULL,
    goal_difference INTEGER NOT NULL,
    points INTEGER NOT NULL,
    PRIMARY KEY (league_id, season_year, club_id),
    FOREIGN KEY (league_id, season_year) REFERENCES seasons(league_id, season_year),
    FOREIGN KEY (club_id) REFERENCES clubs(id)
);

CREATE TABLE IF NOT EXISTS fixture_player_stats (
    fixture_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    position VARCHAR(10),
    minutes INTEGER,
    goals INTEGER,
    assists INTEGER,
    yellow_cards INTEGER,
    red_cards INTEGER,
    rating VARCHAR(16),
    shots_on INTEGER,
    passes_key INTEGER,
    tackles INTEGER,
    saves INTEGER,
    raw_statistics TEXT NOT NULL,
    synced_at TIMESTAMP NOT NULL,
    PRIMARY KEY (fixture_id, player_id),
    FOREIGN KEY (fixture_id) REFERENCES fixtures(id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (club_id) REFERENCES clubs(id)
);

-- Manually entered 2026/27 fixture statistics are kept apart from
-- API-Football raw rows and the 2024/25 v1 scoring rules.
CREATE TABLE IF NOT EXISTS manual_fixture_player_stats (
    fixture_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    participation_status VARCHAR(20) NOT NULL,
    rating DECIMAL(4,2),
    fantasy_points DECIMAL(4,2),
    minutes INTEGER,
    goals INTEGER,
    assists INTEGER,
    yellow_cards INTEGER,
    red_cards INTEGER,
    PRIMARY KEY (fixture_id, player_id),
    FOREIGN KEY (fixture_id) REFERENCES fixtures(id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (club_id) REFERENCES clubs(id),
    FOREIGN KEY (league_id, season_year) REFERENCES seasons(league_id, season_year)
);

CREATE TABLE IF NOT EXISTS fixture_score_evidence (
    fixture_id INTEGER PRIMARY KEY,
    payload_json TEXT NOT NULL,
    captured_at TIMESTAMP NOT NULL,
    FOREIGN KEY (fixture_id) REFERENCES fixtures(id)
);

-- Extended per-match metrics sourced from FPL API starting from GW11 2026/27.
-- Kept separate from manual_fixture_player_stats; GW1-GW10 are not backfilled.
CREATE TABLE IF NOT EXISTS manual_fixture_player_extra_stats (
    fixture_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    starts INTEGER,
    clean_sheets INTEGER,
    goals_conceded INTEGER,
    own_goals INTEGER,
    penalties_saved INTEGER,
    penalties_missed INTEGER,
    saves INTEGER,
    bonus INTEGER,
    bps INTEGER,
    clearances_blocks_interceptions INTEGER,
    recoveries INTEGER,
    tackles INTEGER,
    defensive_contribution INTEGER,
    expected_goals DECIMAL(5,2),
    expected_assists DECIMAL(5,2),
    expected_goal_involvements DECIMAL(5,2),
    expected_goals_conceded DECIMAL(5,2),
    influence DECIMAL(6,1),
    creativity DECIMAL(6,1),
    threat DECIMAL(6,1),
    ict_index DECIMAL(6,1),
    source VARCHAR(30) NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    PRIMARY KEY (fixture_id, player_id),
    FOREIGN KEY (fixture_id) REFERENCES fixtures(id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (club_id) REFERENCES clubs(id),
    FOREIGN KEY (league_id, season_year) REFERENCES seasons(league_id, season_year),
    CHECK (source = 'FPL')
);

-- Verified 2026/27 formations and matchday roles; independent of statistics and Fantasy positions.
CREATE TABLE IF NOT EXISTS club_season_formations (
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL CHECK (season_year=2026),
    club_id INTEGER NOT NULL,
    default_formation VARCHAR(20),
    updated_on DATE NOT NULL,
    scope_from_gw INTEGER NOT NULL,
    scope_to_gw INTEGER NOT NULL,
    verified_matches INTEGER NOT NULL,
    formation_counts TEXT NOT NULL,
    fixture_ids TEXT NOT NULL,
    default_source VARCHAR(20) NOT NULL DEFAULT 'MISSING',
    source_note VARCHAR(500),
    PRIMARY KEY (league_id,season_year,club_id),
    FOREIGN KEY (league_id,season_year,club_id) REFERENCES season_clubs(league_id,season_year,club_id),
    CHECK (scope_from_gw BETWEEN 1 AND 38 AND scope_to_gw BETWEEN scope_from_gw AND 38),
    CHECK (verified_matches >= 0),
    CHECK ((CASE default_source WHEN 'USER' THEN 1 WHEN 'OBSERVED' THEN 1 WHEN 'MISSING' THEN 1 ELSE 0 END)=1),
    CHECK ((default_source='USER' AND default_formation IS NOT NULL AND source_note IS NOT NULL)
        OR (default_source='MISSING' AND verified_matches=0 AND default_formation IS NULL)
        OR (default_source='OBSERVED' AND verified_matches>0 AND default_formation IS NOT NULL))
);

CREATE TABLE IF NOT EXISTS fixture_lineups (
    fixture_id INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL CHECK (season_year=2026),
    formation VARCHAR(20),
    formation_verified_on DATE,
    formation_source VARCHAR(500),
    roles_verified_on DATE,
    roles_source VARCHAR(500),
    PRIMARY KEY (fixture_id,club_id),
    FOREIGN KEY (fixture_id) REFERENCES fixtures(id),
    FOREIGN KEY (league_id,season_year,club_id) REFERENCES season_clubs(league_id,season_year,club_id)
);

CREATE TABLE IF NOT EXISTS fixture_lineup_players (
    fixture_id INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    role VARCHAR(20) NOT NULL CHECK ((CASE role WHEN 'STARTER' THEN 1 WHEN 'SUB_USED' THEN 1 WHEN 'SUB_UNUSED' THEN 1 ELSE 0 END)=1),
    match_position VARCHAR(20),
    row_index INTEGER,
    slot_index INTEGER,
    substitution_in_minute INTEGER,
    substitution_out_minute INTEGER,
    PRIMARY KEY (fixture_id,club_id,player_id),
    FOREIGN KEY (fixture_id,club_id) REFERENCES fixture_lineups(fixture_id,club_id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    CHECK ((row_index IS NULL AND slot_index IS NULL) OR (row_index IS NOT NULL AND slot_index IS NOT NULL AND row_index >= 0 AND slot_index >= 0)),
    CHECK (substitution_in_minute IS NULL OR substitution_in_minute BETWEEN 0 AND 130),
    CHECK (substitution_out_minute IS NULL OR substitution_out_minute BETWEEN 0 AND 130)
);

CREATE TABLE IF NOT EXISTS sync_states (
    league_id INTEGER NOT NULL,
    season_year INTEGER NOT NULL,
    scope VARCHAR(32) NOT NULL,
    item_id INTEGER NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    synced_at TIMESTAMP NOT NULL,
    PRIMARY KEY (league_id, season_year, scope, item_id),
    FOREIGN KEY (league_id, season_year) REFERENCES seasons(league_id, season_year)
);

-- Additive provider mappings: existing API-Football IDs and data are untouched.
CREATE TABLE IF NOT EXISTS football_data_teams (
    provider_id INTEGER PRIMARY KEY,
    club_id INTEGER NOT NULL UNIQUE,
    FOREIGN KEY (club_id) REFERENCES clubs(id)
);

CREATE TABLE IF NOT EXISTS football_data_fixtures (
    provider_id INTEGER PRIMARY KEY,
    fixture_id INTEGER NOT NULL UNIQUE,
    kickoff_utc VARCHAR(40) NOT NULL,
    provider_status VARCHAR(30) NOT NULL,
    FOREIGN KEY (fixture_id) REFERENCES fixtures(id)
);

-- Provisional per-match statistics fetched automatically from FPL after a match finishes
-- while awaiting confirmed SofaScore ratings/lineups. Never merged into season/Fantasy totals.
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
    starts INTEGER,
    clean_sheets INTEGER,
    goals_conceded INTEGER,
    own_goals INTEGER,
    penalties_saved INTEGER,
    penalties_missed INTEGER,
    saves INTEGER,
    bonus INTEGER,
    bps INTEGER,
    clearances_blocks_interceptions INTEGER,
    recoveries INTEGER,
    tackles INTEGER,
    defensive_contribution INTEGER,
    expected_goals DECIMAL(5,2),
    expected_assists DECIMAL(5,2),
    expected_goal_involvements DECIMAL(5,2),
    expected_goals_conceded DECIMAL(5,2),
    influence DECIMAL(6,1),
    creativity DECIMAL(6,1),
    threat DECIMAL(6,1),
    ict_index DECIMAL(6,1),
    fpl_status VARCHAR(20) NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    PRIMARY KEY (fixture_id, player_id),
    FOREIGN KEY (fixture_id) REFERENCES fixtures(id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (club_id) REFERENCES clubs(id),
    FOREIGN KEY (league_id, season_year) REFERENCES seasons(league_id, season_year),
    CHECK (fpl_status IN ('PROVISIONAL', 'FINAL_FPL'))
);

CREATE TABLE IF NOT EXISTS fpl_provisional_unmapped (
    fixture_id INTEGER NOT NULL,
    fpl_element_id INTEGER NOT NULL,
    fpl_name VARCHAR(200) NOT NULL,
    club_id INTEGER,
    club_name VARCHAR(200),
    minutes INTEGER NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    PRIMARY KEY (fixture_id, fpl_element_id),
    FOREIGN KEY (fixture_id) REFERENCES fixtures(id),
    FOREIGN KEY (club_id) REFERENCES clubs(id)
);

-- Privacy-preserving daily visitor and pageview counters (Vietnam calendar day; UTC first_seen_at).
CREATE TABLE IF NOT EXISTS site_visitors_daily (
    stat_day DATE NOT NULL,
    visitor_hash CHAR(64) NOT NULL,
    first_seen_at TIMESTAMP(6) NOT NULL,
    views INTEGER NOT NULL,
    PRIMARY KEY (stat_day, visitor_hash)
);

CREATE TABLE IF NOT EXISTS site_page_views_daily (
    stat_day DATE NOT NULL,
    page_key VARCHAR(32) NOT NULL,
    views INTEGER NOT NULL,
    PRIMARY KEY (stat_day, page_key)
);
