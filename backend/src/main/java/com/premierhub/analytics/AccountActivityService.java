package com.premierhub.analytics;

import com.premierhub.accounts.Account;
import com.premierhub.accounts.AccountRepository;
import com.premierhub.accounts.SystemAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records authenticated user daily activity with a 15-minute in-memory write throttle.
 * Failures never interrupt the originating HTTP request.
 */
@Service
public class AccountActivityService {
    private static final Logger log = LoggerFactory.getLogger(AccountActivityService.class);
    static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    static final Duration THROTTLE_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_CACHE_ENTRIES = 10_000;

    private final JdbcTemplate jdbc;
    private final AccountRepository accounts;
    private final Clock clock;
    private final ConcurrentHashMap<String, Instant> lastWrittenAt = new ConcurrentHashMap<>();

    @Autowired
    public AccountActivityService(
            JdbcTemplate jdbc,
            AccountRepository accounts,
            @Qualifier("fantasyGameweekClock") Clock clock) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.clock = clock;
    }

    /**
     * Records activity for the given authenticated account email.
     *
     * @return {@code true} if a DB write occurred, {@code false} if throttled, ignored, or failed.
     */
    public boolean recordActivity(String email) {
        if (email == null || email.isBlank() || SystemAccount.isSystemEmail(email)) {
            return false;
        }
        String normalized = email.strip().toLowerCase();
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);

        Instant previous = lastWrittenAt.get(normalized);
        if (previous != null && now.isBefore(previous.plus(THROTTLE_WINDOW))) {
            return false;
        }

        try {
            Optional<Account> account = accounts.findByEmail(normalized);
            if (account.isEmpty() || SystemAccount.isSystemEmail(account.get().email())) {
                return false;
            }
            if (lastWrittenAt.size() >= MAX_CACHE_ENTRIES) {
                pruneCache(now);
            }
            upsertDailyActivity(account.get().id(), now);
            lastWrittenAt.put(normalized, now);
            return true;
        } catch (Exception ex) {
            log.debug("Failed to record account activity: {}", ex.getMessage());
            return false;
        }
    }

    private void upsertDailyActivity(long accountId, Instant now) {
        LocalDate vnDay = now.atZone(VIETNAM).toLocalDate();
        Date sqlDay = Date.valueOf(vnDay);
        Timestamp utcNow = Timestamp.valueOf(LocalDateTime.ofInstant(now, ZoneOffset.UTC));

        int updated = jdbc.update(
                "UPDATE account_activity_days SET last_seen_at = ? WHERE account_id = ? AND stat_day = ?",
                utcNow, accountId, sqlDay);
        if (updated > 0) {
            return;
        }
        try {
            jdbc.update(
                    "INSERT INTO account_activity_days (account_id, stat_day, first_seen_at, last_seen_at) VALUES (?, ?, ?, ?)",
                    accountId, sqlDay, utcNow, utcNow);
        } catch (DuplicateKeyException race) {
            jdbc.update(
                    "UPDATE account_activity_days SET last_seen_at = ? WHERE account_id = ? AND stat_day = ?",
                    utcNow, accountId, sqlDay);
        }
    }

    private void pruneCache(Instant now) {
        Instant cutoff = now.minus(THROTTLE_WINDOW);
        lastWrittenAt.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        if (lastWrittenAt.size() >= MAX_CACHE_ENTRIES) {
            lastWrittenAt.clear();
        }
    }
}
