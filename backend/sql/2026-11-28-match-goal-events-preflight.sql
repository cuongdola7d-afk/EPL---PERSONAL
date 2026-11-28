-- Read-only preflight for match_goal_events (live goal minutes persistence, 2026/27).
-- Execute only against a separately confirmed database target before applying 2026-11-28-match-goal-events-mysql.sql.
SET time_zone = '+00:00';
SELECT VERSION() AS mysql_version, DATABASE() AS database_name,
       @@session.time_zone AS session_time_zone, UTC_TIMESTAMP() AS utc_now;

-- Confirm parent FK column types and engines (fixtures.id, clubs.id, players.id INTEGER, InnoDB).
SELECT t.TABLE_NAME, t.ENGINE, c.COLUMN_NAME, c.COLUMN_TYPE, c.IS_NULLABLE, c.COLLATION_NAME
FROM information_schema.TABLES t
JOIN information_schema.COLUMNS c ON c.TABLE_SCHEMA = t.TABLE_SCHEMA AND c.TABLE_NAME = t.TABLE_NAME
WHERE t.TABLE_SCHEMA = DATABASE()
  AND ((t.TABLE_NAME = 'fixtures' AND c.COLUMN_NAME = 'id')
    OR (t.TABLE_NAME = 'clubs' AND c.COLUMN_NAME = 'id')
    OR (t.TABLE_NAME = 'players' AND c.COLUMN_NAME = 'id'))
ORDER BY t.TABLE_NAME;

-- Check whether match_goal_events already exists (expected: 0 rows before migration).
SELECT TABLE_NAME, ENGINE, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'match_goal_events';

-- Baseline counts of existing tables (must remain unchanged after migration).
SELECT COUNT(*) AS existing_fixtures_2026 FROM fixtures WHERE league_id = 39 AND season_year = 2026;
SELECT COUNT(*) AS existing_manual_match_stats FROM manual_fixture_player_stats;
SELECT COUNT(*) AS existing_fpl_provisional_stats FROM fpl_provisional_player_stats;
SELECT COUNT(*) AS existing_fantasy_entries FROM fantasy_entries;
