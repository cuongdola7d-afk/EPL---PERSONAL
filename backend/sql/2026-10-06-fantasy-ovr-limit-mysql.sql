-- Upgrade the existing 2026 Fantasy submission CHECK from 860 to 910.
-- Apply after the four Fantasy migrations, a verified backup and authorization.
-- No rows, snapshots, football tables or other CHECK clauses are changed.
-- MySQL 8.0.17+. mysql client understands the DELIMITER commands below.
DELIMITER $$
DROP PROCEDURE IF EXISTS premierhub_upgrade_fantasy_ovr_910$$
CREATE PROCEDURE premierhub_upgrade_fantasy_ovr_910()
BEGIN
    DECLARE matches_count INT;
    DECLARE check_name VARCHAR(64);
    DECLARE check_clause LONGTEXT;
    DECLARE normalized_clause LONGTEXT;
    SELECT COUNT(*), MAX(tc.CONSTRAINT_NAME), MAX(cc.CHECK_CLAUSE)
      INTO matches_count, check_name, check_clause
      FROM information_schema.TABLE_CONSTRAINTS tc
      JOIN information_schema.CHECK_CONSTRAINTS cc
        ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME
     WHERE tc.TABLE_SCHEMA=DATABASE() AND tc.TABLE_NAME='fantasy_entries'
       AND tc.CONSTRAINT_TYPE='CHECK' AND cc.CHECK_CLAUSE LIKE '%submitted_total_ovr%';
    IF matches_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected exactly one Fantasy submission CHECK; inspect schema before continuing';
    END IF;
    SET normalized_clause=REGEXP_REPLACE(REPLACE(LOWER(check_clause),'`',''),'[[:space:]]+',' ');
    IF normalized_clause LIKE '%submitted_total_ovr between 11 and 910%' THEN
        SELECT 'Fantasy OVR already 910; unchanged' AS migration_status;
    ELSEIF normalized_clause LIKE '%submitted_total_ovr between 11 and 860%'
       AND (LENGTH(check_clause)-LENGTH(REPLACE(check_clause,'860',''))) = 3 THEN
        -- Preserve the entire original nullability/version/formation invariant.
        -- One atomic ALTER replaces the CHECK; there is no unprotected interval.
        -- MySQL INFORMATION_SCHEMA escapes quotes in CHECK_CLAUSE (e.g. formation literals).
        SET check_clause=REPLACE(check_clause,CONCAT(CHAR(92),CHAR(39)),CHAR(39));
        SET @fantasy_ovr_sql=CONCAT('ALTER TABLE fantasy_entries DROP CHECK `',
            REPLACE(check_name,'`','``'),'`, ADD CONSTRAINT `',REPLACE(check_name,'`','``'),
            '` CHECK (',REPLACE(check_clause,'860','910'),')');
        PREPARE fantasy_ovr_change FROM @fantasy_ovr_sql;
        EXECUTE fantasy_ovr_change;
        DEALLOCATE PREPARE fantasy_ovr_change;
        SET @fantasy_ovr_sql=NULL;
        SELECT 'Fantasy OVR upgraded to 910; rows preserved' AS migration_status;
    ELSE
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unrecognized Fantasy OVR CHECK; no table changes applied';
    END IF;
END$$
CALL premierhub_upgrade_fantasy_ovr_910()$$
DROP PROCEDURE premierhub_upgrade_fantasy_ovr_910$$
DELIMITER ;
