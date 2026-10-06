-- Only current drafts/submissions; snapshots never depend on mutable football profiles.
CREATE TABLE IF NOT EXISTS fantasy_entries (
    account_id BIGINT NOT NULL,
    season INTEGER NOT NULL,
    gameweek INTEGER NOT NULL,
    version BIGINT NOT NULL,
    draft_formation VARCHAR(10),
    draft_saved_at TIMESTAMP(6),
    submitted_formation VARCHAR(10),
    submitted_at TIMESTAMP(6),
    submitted_version BIGINT,
    submitted_total_ovr INTEGER,
    PRIMARY KEY (account_id, season, gameweek),
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (season, gameweek) REFERENCES fantasy_gameweeks(season, gameweek),
    CHECK (season=2026 AND gameweek BETWEEN 6 AND 38 AND version>=1),
    CHECK ((draft_formation IS NULL AND draft_saved_at IS NULL)
        OR (draft_formation IS NOT NULL AND draft_formation IN ('4-2-1-3','4-3-3','4-4-2','3-5-2') AND draft_saved_at IS NOT NULL)),
    CHECK ((submitted_formation IS NULL AND submitted_at IS NULL AND submitted_version IS NULL AND submitted_total_ovr IS NULL)
        OR (submitted_formation IS NOT NULL AND submitted_formation IN ('4-2-1-3','4-3-3','4-4-2','3-5-2') AND submitted_at IS NOT NULL
            AND submitted_version IS NOT NULL AND submitted_total_ovr IS NOT NULL
            AND submitted_version BETWEEN 1 AND version AND submitted_total_ovr BETWEEN 11 AND 910))
);

CREATE TABLE IF NOT EXISTS fantasy_draft_picks (
    account_id BIGINT NOT NULL,
    season INTEGER NOT NULL,
    gameweek INTEGER NOT NULL,
    slot_key VARCHAR(3) NOT NULL,
    player_id INTEGER NOT NULL,
    PRIMARY KEY (account_id,season,gameweek,slot_key),
    UNIQUE (account_id,season,gameweek,player_id),
    FOREIGN KEY (account_id,season,gameweek) REFERENCES fantasy_entries(account_id,season,gameweek),
    CHECK (player_id > 0)
);

CREATE TABLE IF NOT EXISTS fantasy_submitted_picks (
    account_id BIGINT NOT NULL,
    season INTEGER NOT NULL,
    gameweek INTEGER NOT NULL,
    slot_key VARCHAR(3) NOT NULL,
    player_id INTEGER NOT NULL,
    required_position VARCHAR(3) NOT NULL,
    club_id INTEGER NOT NULL,
    player_name VARCHAR(200) NOT NULL,
    club_name VARCHAR(200) NOT NULL,
    ovr INTEGER NOT NULL,
    primary_position VARCHAR(3) NOT NULL,
    eligible_positions VARCHAR(100) NOT NULL,
    PRIMARY KEY (account_id,season,gameweek,slot_key),
    UNIQUE (account_id,season,gameweek,player_id),
    FOREIGN KEY (account_id,season,gameweek) REFERENCES fantasy_entries(account_id,season,gameweek),
    CHECK (ovr BETWEEN 1 AND 99 AND player_id > 0)
);
