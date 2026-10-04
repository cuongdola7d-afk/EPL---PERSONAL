-- UTC timestamps; football tables and account/session data are untouched.
CREATE TABLE IF NOT EXISTS fantasy_gameweeks (
    season INTEGER NOT NULL,
    gameweek INTEGER NOT NULL,
    deadline_utc TIMESTAMP(6) NOT NULL,
    deadline_published_at TIMESTAMP(6) NOT NULL,
    first_fixture_id INTEGER NOT NULL,
    first_kickoff_utc TIMESTAMP(6) NOT NULL,
    workflow_status VARCHAR(20) NOT NULL,
    results_published_at TIMESTAMP(6),
    revision INTEGER NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    roster_as_of DATE NOT NULL,
    PRIMARY KEY (season, gameweek),
    CHECK (season = 2026 AND gameweek BETWEEN 6 AND 38),
    CHECK (workflow_status IN ('OPEN','LOCKED','AWAITING_RESULTS','PUBLISHED')),
    CHECK ((workflow_status = 'PUBLISHED' AND results_published_at IS NOT NULL)
        OR (workflow_status <> 'PUBLISHED' AND results_published_at IS NULL)),
    CHECK (revision >= 1)
);

CREATE TABLE IF NOT EXISTS fantasy_deadline_changes (
    season INTEGER NOT NULL,
    gameweek INTEGER NOT NULL,
    revision INTEGER NOT NULL,
    old_deadline_utc TIMESTAMP(6),
    new_deadline_utc TIMESTAMP(6) NOT NULL,
    changed_at TIMESTAMP(6) NOT NULL,
    changed_by BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    PRIMARY KEY (season, gameweek, revision),
    FOREIGN KEY (season, gameweek) REFERENCES fantasy_gameweeks(season, gameweek),
    FOREIGN KEY (changed_by) REFERENCES accounts(id),
    CHECK (CHAR_LENGTH(TRIM(reason)) BETWEEN 3 AND 500)
);

-- Upgrade existing local H2 gameweeks without resetting drafts or snapshots.
ALTER TABLE fantasy_gameweeks ADD COLUMN IF NOT EXISTS roster_as_of DATE;
UPDATE fantasy_gameweeks SET roster_as_of = CAST(DATEADD('HOUR', 7, deadline_published_at) AS DATE)
WHERE season=2026 AND roster_as_of IS NULL;
ALTER TABLE fantasy_gameweeks ALTER COLUMN roster_as_of SET NOT NULL;
