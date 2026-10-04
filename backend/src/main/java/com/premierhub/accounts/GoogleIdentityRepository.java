package com.premierhub.accounts;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
public class GoogleIdentityRepository {
    private final JdbcTemplate jdbc;
    public GoogleIdentityRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    Optional<Long> accountId(String subject) {
        return jdbc.query("SELECT account_id FROM account_identities WHERE provider='google' AND subject_id=?",
                (rs, index) -> rs.getLong(1), subject).stream().findFirst();
    }

    boolean linked(long accountId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM account_identities WHERE provider='google' AND account_id=?",
                Integer.class, accountId) > 0;
    }

    void link(long accountId, String subject) {
        jdbc.update("INSERT INTO account_identities (provider,subject_id,account_id,created_at) VALUES ('google',?,?,?)",
                subject, accountId, Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)));
    }
}
