-- MySQL 8.0.17+; only the four auth tables. No H2 accounts are copied.
-- Before running: backup, inspect SHOW CREATE TABLE for existing auth tables and stop
-- if they differ from these definitions. IF NOT EXISTS does not upgrade an old table.
-- Rerunnable on a compatible schema. DDL commits implicitly; no destructive rollback.
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NULL,
    role VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT accounts_email_unique UNIQUE (email),
    CONSTRAINT accounts_role_check CHECK (role IN ('USER', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS account_identities (
    provider VARCHAR(16) NOT NULL,
    subject_id VARCHAR(255) NOT NULL,
    account_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (provider, subject_id),
    KEY account_identities_account_idx (account_id),
    CONSTRAINT account_identities_account_fk FOREIGN KEY (account_id) REFERENCES accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS SPRING_SESSION (
    PRIMARY_ID CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    SESSION_ID CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(254) NULL,
    PRIMARY KEY (PRIMARY_ID),
    UNIQUE KEY SPRING_SESSION_IX1 (SESSION_ID),
    KEY SPRING_SESSION_IX2 (EXPIRY_TIME),
    KEY SPRING_SESSION_IX3 (PRINCIPAL_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BLOB NOT NULL,
    PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
