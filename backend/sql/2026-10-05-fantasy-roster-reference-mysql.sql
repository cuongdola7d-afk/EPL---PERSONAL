-- MySQL 8.0.17+. Apply after the gameweek migration; backup before production writes.
-- Additive, repeatable upgrade. Never updates football data or submitted snapshots.
SET time_zone = '+00:00';
SET @roster_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'fantasy_gameweeks' AND column_name = 'roster_as_of'
);
SET @roster_ddl = IF(@roster_column_exists = 0,
    'ALTER TABLE fantasy_gameweeks ADD COLUMN roster_as_of DATE NULL', 'SELECT 1');
PREPARE roster_statement FROM @roster_ddl;
EXECUTE roster_statement;
DEALLOCATE PREPARE roster_statement;

-- Legacy GW default: original publication date in Vietnam, never migration/current date.
UPDATE fantasy_gameweeks
SET roster_as_of = DATE(deadline_published_at + INTERVAL 7 HOUR)
WHERE season = 2026 AND roster_as_of IS NULL;
ALTER TABLE fantasy_gameweeks MODIFY COLUMN roster_as_of DATE NOT NULL;
