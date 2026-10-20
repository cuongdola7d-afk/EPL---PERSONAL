-- Accept verified non-registration regardless of the reason for absence.
-- Run after a verified backup. Preserve existing injury evidence and all rows.
-- Deploy backend/frontend support for NOT_REGISTERED before publishing results.
-- MySQL 8.0.17+, mysql client (DELIMITER supported). Safe to run again.
DELIMITER $$
DROP PROCEDURE IF EXISTS premierhub_upgrade_fantasy_absence$$
CREATE PROCEDURE premierhub_upgrade_fantasy_absence()
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
     WHERE tc.TABLE_SCHEMA=DATABASE() AND tc.TABLE_NAME='fantasy_match_absence_confirmations'
       AND tc.CONSTRAINT_TYPE='CHECK' AND cc.CHECK_CLAUSE LIKE '%reason%';
    IF matches_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected one absence reason CHECK; inspect schema';
    END IF;
    SET normalized_clause=LOWER(REPLACE(REPLACE(REPLACE(REPLACE(check_clause,'`',''),CHAR(92),''),'(',''),')',''));
    -- MySQL prefixes string literals with the connection character set (e.g. _utf8mb4).
    SET normalized_clause=REGEXP_REPLACE(normalized_clause,
        CONCAT('_(utf8mb4|utf8mb3|utf8|cp850|latin1|ascii|binary)',CHAR(39 USING utf8mb4)),
        CHAR(39 USING utf8mb4));
    SET normalized_clause=REGEXP_REPLACE(normalized_clause,'[[:space:]]+','');
    IF normalized_clause = 'reasonin''injury_not_registered'',''not_registered''' THEN
        SELECT 'Absence reasons already upgraded; unchanged' AS migration_status;
    ELSEIF normalized_clause = 'reason=''injury_not_registered''' THEN
        SET @fantasy_absence_sql=CONCAT('ALTER TABLE fantasy_match_absence_confirmations DROP CHECK `',
            REPLACE(check_name,'`','``'),'`, ADD CONSTRAINT `',REPLACE(check_name,'`','``'),
            '` CHECK (reason IN (''INJURY_NOT_REGISTERED'',''NOT_REGISTERED''))');
        PREPARE fantasy_absence_change FROM @fantasy_absence_sql;
        EXECUTE fantasy_absence_change;
        DEALLOCATE PREPARE fantasy_absence_change;
        SET @fantasy_absence_sql=NULL;
        SELECT 'Absence reasons upgraded; rows preserved' AS migration_status;
    ELSE
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unrecognized absence reason CHECK; no table changes applied';
    END IF;
END$$
CALL premierhub_upgrade_fantasy_absence()$$
DROP PROCEDURE premierhub_upgrade_fantasy_absence$$
DELIMITER ;
