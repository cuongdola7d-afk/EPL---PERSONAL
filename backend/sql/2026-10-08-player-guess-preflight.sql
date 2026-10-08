-- Read-only preflight. Execute only against a separately confirmed database target.
-- Reports counts/schema only; never prints the selected answer, identities, tokens or passwords.
SET time_zone = '+00:00';
SET @pg_date = DATE(UTC_TIMESTAMP() + INTERVAL 7 HOUR);
SELECT VERSION() AS mysql_version, DATABASE() AS database_name,
       @@session.time_zone AS session_time_zone, @pg_date AS vietnam_question_date;

SELECT t.TABLE_NAME,t.ENGINE,c.COLUMN_NAME,c.COLUMN_TYPE,c.IS_NULLABLE,c.COLLATION_NAME
FROM information_schema.TABLES t
JOIN information_schema.COLUMNS c ON c.TABLE_SCHEMA=t.TABLE_SCHEMA AND c.TABLE_NAME=t.TABLE_NAME
WHERE t.TABLE_SCHEMA=DATABASE() AND t.TABLE_NAME='accounts' AND c.COLUMN_NAME='id';

-- Existing Minigame tables must be reviewed with SHOW CREATE TABLE before migration.
SELECT TABLE_NAME,ENGINE,TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN (
    'player_guess_selector','player_guess_questions','player_guess_games','player_guess_practice_state',
    'player_guess_guesses','player_guess_actions','player_guess_daily_results')
ORDER BY TABLE_NAME;

-- Match PlayerGuessRepository.candidates and Candidate.eligible, including exact-case codes.
WITH candidates AS (
    SELECT p.id,p.name,c.name AS club_name,f.nationality,f.birth_date,f.height_cm,
           f.preferred_foot,f.shirt_number,f.fc27_overall,pos.primary_position,
           (SELECT COUNT(*) FROM manual_player_memberships overlap
            WHERE overlap.league_id=39 AND overlap.season_year=2026 AND overlap.player_id=p.id
              AND overlap.start_date<=@pg_date AND (overlap.end_date IS NULL OR overlap.end_date>@pg_date)) AS membership_count
    FROM manual_player_memberships m
    JOIN season_clubs sc ON sc.league_id=m.league_id AND sc.season_year=m.season_year AND sc.club_id=m.club_id
    JOIN players p ON p.id=m.player_id
    JOIN clubs c ON c.id=m.club_id
    LEFT JOIN player_season_profiles f ON f.league_id=m.league_id AND f.season_year=m.season_year
        AND f.player_id=m.player_id AND f.club_id=m.club_id
    LEFT JOIN player_specific_positions pos ON pos.league_id=m.league_id AND pos.season_year=m.season_year
        AND pos.player_id=m.player_id
    WHERE m.league_id=39 AND m.season_year=2026 AND m.start_date<=@pg_date
      AND (m.end_date IS NULL OR m.end_date>@pg_date)
), eligibility AS (
    SELECT *, COALESCE(membership_count=1 AND CHAR_LENGTH(TRIM(name))>0 AND CHAR_LENGTH(TRIM(club_name))>0
        AND CHAR_LENGTH(TRIM(nationality))>0 AND birth_date IS NOT NULL AND birth_date<=@pg_date
        AND height_cm BETWEEN 100 AND 250 AND BINARY preferred_foot IN ('LEFT','RIGHT','BOTH')
        AND shirt_number BETWEEN 1 AND 99 AND fc27_overall BETWEEN 75 AND 99
        AND BINARY primary_position IN ('GK','LB','CB','RB','CM','CAM','LM','RM','LW','ST','RW'),0) AS eligible
    FROM candidates
)
SELECT COUNT(DISTINCT id) AS active_roster_players,
       COUNT(DISTINCT CASE WHEN membership_count=1 AND CHAR_LENGTH(TRIM(name))>0 THEN id END) AS guess_choices,
       COUNT(DISTINCT CASE WHEN eligible=1 THEN id END) AS eligible_answers,
       COUNT(DISTINCT CASE WHEN membership_count<>1 THEN id END) AS ambiguous_membership_players,
       COUNT(DISTINCT CASE WHEN fc27_overall IS NULL THEN id END) AS missing_ovr_players,
       COUNT(DISTINCT CASE WHEN fc27_overall<75 THEN id END) AS low_ovr_players,
       COUNT(DISTINCT CASE WHEN nationality IS NULL OR CHAR_LENGTH(TRIM(nationality))=0 OR birth_date IS NULL
           OR height_cm IS NULL OR preferred_foot IS NULL OR shirt_number IS NULL OR primary_position IS NULL
           THEN id END) AS missing_hint_players
FROM eligibility;

-- Baseline counts, not a replacement for a full backup/schema and data comparison.
SELECT COUNT(*) AS existing_accounts FROM accounts;
SELECT COUNT(*) AS existing_fantasy_entries FROM fantasy_entries;
SELECT season_year,COUNT(*) AS stats_rows,SUM(goals) AS known_goals,SUM(assists) AS known_assists
FROM player_season_stats WHERE league_id=39 AND season_year IN (2024,2026) GROUP BY season_year;
