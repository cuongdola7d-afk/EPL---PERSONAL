-- Run only after checking the target database and making a new backup.
-- Add the two metadata columns required by LineupBatchImporter; preserve all rows.
-- Re-running this migration makes no further schema changes.
SET @gw6_lineup_default_source_sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='club_season_formations' AND column_name='default_source'),
    'DO 0',
    'ALTER TABLE club_season_formations ADD COLUMN default_source VARCHAR(20) NOT NULL DEFAULT ''MISSING'''
);
PREPARE gw6_lineup_schema_statement FROM @gw6_lineup_default_source_sql;
EXECUTE gw6_lineup_schema_statement;
DEALLOCATE PREPARE gw6_lineup_schema_statement;

SET @gw6_lineup_source_note_sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='club_season_formations' AND column_name='source_note'),
    'DO 0',
    'ALTER TABLE club_season_formations ADD COLUMN source_note VARCHAR(500)'
);
PREPARE gw6_lineup_schema_statement FROM @gw6_lineup_source_note_sql;
EXECUTE gw6_lineup_schema_statement;
DEALLOCATE PREPARE gw6_lineup_schema_statement;
