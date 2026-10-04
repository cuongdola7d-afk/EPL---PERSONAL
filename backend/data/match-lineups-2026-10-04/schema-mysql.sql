-- PREPARED ONLY: not executed on production. New metadata tables; no statistics updates.
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

