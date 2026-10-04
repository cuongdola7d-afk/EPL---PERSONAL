package com.premierhub.accounts;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
public class AccountRepository {
    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    Optional<Account> findByEmail(String email) {
        return jdbc.query("SELECT id,email,display_name,password_hash,role,created_at FROM accounts WHERE email=?",
                (rs, index) -> new Account(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getTimestamp(6).toLocalDateTime().toInstant(ZoneOffset.UTC)), email)
                .stream().findFirst();
    }

    Account create(String email, String name, String hash, Instant createdAt) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO accounts (email,display_name,password_hash,role,created_at) "
                    + "VALUES (?,?,?,'USER',?)", new String[]{"id"});
            statement.setString(1, email);
            statement.setString(2, name);
            statement.setString(3, hash);
            statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC)));
            return statement;
        }, key);
        if (key.getKey() == null) throw new IllegalStateException("Account ID was not generated");
        return new Account(key.getKey().longValue(), email, name, hash, "USER", createdAt);
    }
}
