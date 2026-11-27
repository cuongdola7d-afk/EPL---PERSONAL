-- Read-only preflight for Minigame Wordle (2026/27).
-- Execute only against a separately confirmed database target before applying 2026-11-28-wordle-mysql.sql.
SET time_zone = '+00:00';
SET @wd_date = DATE(UTC_TIMESTAMP() + INTERVAL 7 HOUR);
SELECT VERSION() AS mysql_version, DATABASE() AS database_name,
       @@session.time_zone AS session_time_zone, @wd_date AS vietnam_puzzle_date;

-- Confirm parent FK column types and engines (accounts.id BIGINT, players.id INTEGER).
SELECT t.TABLE_NAME, t.ENGINE, c.COLUMN_NAME, c.COLUMN_TYPE, c.IS_NULLABLE, c.COLLATION_NAME
FROM information_schema.TABLES t
JOIN information_schema.COLUMNS c ON c.TABLE_SCHEMA = t.TABLE_SCHEMA AND c.TABLE_NAME = t.TABLE_NAME
WHERE t.TABLE_SCHEMA = DATABASE()
  AND ((t.TABLE_NAME = 'accounts' AND c.COLUMN_NAME = 'id')
    OR (t.TABLE_NAME = 'players' AND c.COLUMN_NAME = 'id'))
ORDER BY t.TABLE_NAME;

-- Check whether any wordle_* tables already exist.
SELECT TABLE_NAME, ENGINE, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'wordle_name_overrides', 'wordle_selector', 'wordle_puzzles',
      'wordle_games', 'wordle_guesses')
ORDER BY TABLE_NAME;

-- Active 2026/27 Premier League roster coverage on the current Vietnam date.
WITH active_players AS (
    SELECT p.id, p.name, c.name AS club_name, f.fc27_overall,
           (SELECT COUNT(*) FROM manual_player_memberships overlap
            WHERE overlap.league_id = 39 AND overlap.season_year = 2026 AND overlap.player_id = p.id
              AND overlap.start_date <= @wd_date AND (overlap.end_date IS NULL OR overlap.end_date > @wd_date)) AS membership_count
    FROM manual_player_memberships m
    JOIN season_clubs sc ON sc.league_id = m.league_id AND sc.season_year = m.season_year AND sc.club_id = m.club_id
    JOIN players p ON p.id = m.player_id
    JOIN clubs c ON c.id = m.club_id
    LEFT JOIN player_season_profiles f ON f.league_id = m.league_id AND f.season_year = m.season_year
        AND f.player_id = m.player_id AND f.club_id = m.club_id
    WHERE m.league_id = 39 AND m.season_year = 2026
      AND m.start_date <= @wd_date AND (m.end_date IS NULL OR m.end_date > @wd_date)
)
SELECT COUNT(DISTINCT id) AS active_roster_players,
       COUNT(DISTINCT CASE WHEN membership_count = 1 THEN id END) AS unique_active_players,
       COUNT(DISTINCT CASE WHEN membership_count = 1 AND fc27_overall >= 75 THEN id END) AS ovr_ge_75_players,
       COUNT(DISTINCT CASE WHEN membership_count <> 1 THEN id END) AS ambiguous_membership_players
FROM active_players;

-- Baseline counts of existing tables (must remain unchanged after migration).
SELECT COUNT(*) AS existing_accounts FROM accounts;
SELECT COUNT(*) AS existing_player_guess_games FROM player_guess_games;
SELECT COUNT(*) AS existing_fantasy_entries FROM fantasy_entries;
