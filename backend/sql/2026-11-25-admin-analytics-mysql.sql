-- MySQL 8.0.17+ (not MariaDB). Apply manually on Aiven production after backup.
-- Additive migration for Step 2:
--   1. Non-loginable "Hệ thống" actor account for Fantasy automation audit trails.
--   2. account_activity_days (daily active accounts by Vietnam calendar day).
--   3. site_visitors_daily and site_page_views_daily (privacy-preserving pageview stats).
-- Never alters or drops existing tables.
SET time_zone = '+00:00';

INSERT INTO accounts (email, display_name, password_hash, role, created_at)
SELECT 'system@prismaxi.invalid', 'Hệ thống', NULL, 'ADMIN', UTC_TIMESTAMP()
WHERE NOT EXISTS (
    SELECT 1 FROM accounts WHERE email = 'system@prismaxi.invalid'
);

CREATE TABLE IF NOT EXISTS account_activity_days (
    account_id BIGINT NOT NULL,
    stat_day DATE NOT NULL,
    first_seen_at DATETIME(6) NOT NULL,
    last_seen_at DATETIME(6) NOT NULL,
    PRIMARY KEY (account_id, stat_day),
    KEY account_activity_days_day_idx (stat_day),
    CONSTRAINT account_activity_days_account_fk FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS site_visitors_daily (
    stat_day DATE NOT NULL,
    visitor_hash CHAR(64) NOT NULL,
    first_seen_at DATETIME(6) NOT NULL,
    views INTEGER NOT NULL,
    PRIMARY KEY (stat_day, visitor_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS site_page_views_daily (
    stat_day DATE NOT NULL,
    page_key VARCHAR(32) NOT NULL,
    views INTEGER NOT NULL,
    PRIMARY KEY (stat_day, page_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
