-- Apply to the verified production MySQL after a fresh backup, before importing confirmed-not-registered.csv.
-- Fixture/player scope prevents an absence from silently affecting another gameweek.
CREATE TABLE IF NOT EXISTS fantasy_match_absence_confirmations (
    fixture_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    reason VARCHAR(40) NOT NULL CHECK (reason='INJURY_NOT_REGISTERED'),
    source_ref VARCHAR(500) NOT NULL,
    confirmed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (fixture_id,player_id),
    FOREIGN KEY (fixture_id) REFERENCES fixtures(id),
    FOREIGN KEY (player_id) REFERENCES players(id)
);
