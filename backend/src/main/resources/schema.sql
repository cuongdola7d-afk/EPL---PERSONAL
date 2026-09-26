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

CREATE TABLE IF NOT EXISTS fixture_score_evidence (
    fixture_id INTEGER PRIMARY KEY,
    payload_json TEXT NOT NULL,
    captured_at TIMESTAMP NOT NULL,
    FOREIGN KEY (fixture_id) REFERENCES fixtures(id)
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
