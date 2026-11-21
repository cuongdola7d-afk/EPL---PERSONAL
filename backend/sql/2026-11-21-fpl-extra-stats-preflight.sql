-- Read-only preflight for 2026-11-21-fpl-extra-stats-mysql.sql.
-- Execute only against a separately confirmed database target before applying migration.
SET time_zone = '+00:00';

SELECT VERSION() AS mysql_version,
       DATABASE() AS database_name,
       @@session.time_zone AS session_time_zone,
       UTC_TIMESTAMP(6) AS checked_at_utc;

-- Verify required parent tables and key column types match FK definitions.
SELECT t.TABLE_NAME, t.ENGINE, t.TABLE_COLLATION, c.COLUMN_NAME, c.COLUMN_TYPE, c.IS_NULLABLE
FROM information_schema.TABLES t
JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = t.TABLE_SCHEMA AND c.TABLE_NAME = t.TABLE_NAME
WHERE t.TABLE_SCHEMA = DATABASE()
  AND ((t.TABLE_NAME = 'fixtures' AND c.COLUMN_NAME = 'id')
    OR (t.TABLE_NAME = 'players' AND c.COLUMN_NAME = 'id')
    OR (t.TABLE_NAME = 'clubs' AND c.COLUMN_NAME = 'id')
    OR (t.TABLE_NAME = 'seasons' AND c.COLUMN_NAME IN ('league_id', 'season_year')))
ORDER BY t.TABLE_NAME, c.ORDINAL_POSITION;

-- Check whether target table already exists (expected: 0 rows before first migration).
SELECT TABLE_NAME, ENGINE, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'manual_fixture_player_extra_stats';

-- Baseline counts for 2026/27 GW1-GW10 (must remain unchanged after migration).
SELECT COUNT(*) AS manual_stats_rows_2026,
       COUNT(DISTINCT fixture_id) AS recorded_fixtures_2026
FROM manual_fixture_player_stats
WHERE league_id = 39 AND season_year = 2026;
