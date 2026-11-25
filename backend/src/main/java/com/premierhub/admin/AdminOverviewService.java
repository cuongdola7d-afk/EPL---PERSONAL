package com.premierhub.admin;

import com.premierhub.accounts.SystemAccount;
import com.premierhub.fantasy.FantasyAutomation;
import com.premierhub.fantasy.GameweekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminOverviewService {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int SEASON = 2026;
    private static final int LEAGUE = 39;

    public record AccountsSummary(
            long total,
            long newLast7Days,
            long activeToday,
            long active7Days,
            long active30Days
    ) {
    }

    public record DailyPoint(LocalDate day, long visitors, long views) {
    }

    public record TopPage(String pageKey, long views) {
    }

    public record TrafficSummary(
            long visitorsToday,
            long visitorsSameDayLastWeek,
            Integer deltaPercent,
            List<DailyPoint> dailySeries,
            List<TopPage> topPages
    ) {
    }

    public record GameweekCard(
            int gameweek,
            String status,
            Instant deadlineUtc,
            Instant actionAtUtc,
            boolean bySystem,
            Integer finishedFixtures,
            Integer totalFixtures
    ) {
    }

    public record FantasyPlayersSummary(
            long everSubmitted,
            Integer currentGameweek,
            long currentSubmitted,
            Integer previousGameweek,
            long previousSubmitted,
            long currentDrafts
    ) {
    }

    public record HistoryItem(
            String actionType,
            int gameweek,
            Instant occurredAtUtc,
            String actorType,
            String actorName,
            String reason
    ) {
    }

    public record FantasySummary(
            boolean automationEnabled,
            boolean automationDryRun,
            GameweekCard openGameweek,
            GameweekCard latestPublishedGameweek,
            FantasyPlayersSummary players,
            List<HistoryItem> history
    ) {
    }

    public record RecentAccount(
            String displayName,
            String authMethod,
            Instant createdAtUtc,
            LocalDate lastActiveDay
    ) {
    }

    public record Overview(
            Instant generatedAtUtc,
            int rangeDays,
            AccountsSummary accounts,
            TrafficSummary traffic,
            FantasySummary fantasy,
            List<RecentAccount> recentAccounts
    ) {
    }

    private final JdbcTemplate jdbc;
    private final GameweekService gameweekService;
    private final FantasyAutomation fantasyAutomation;
    private final Clock clock;

    @Autowired
    public AdminOverviewService(
            JdbcTemplate jdbc,
            GameweekService gameweekService,
            FantasyAutomation fantasyAutomation,
            @Qualifier("fantasyGameweekClock") Clock clock) {
        this.jdbc = jdbc;
        this.gameweekService = gameweekService;
        this.fantasyAutomation = fantasyAutomation;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Overview overview(int rangeDays) {
        if (rangeDays != 7 && rangeDays != 30) {
            throw new IllegalArgumentException("range must be 7 or 30");
        }
        Instant now = clock.instant();
        LocalDate todayVn = now.atZone(VIETNAM).toLocalDate();

        AccountsSummary accounts = loadAccountsSummary(now, todayVn);
        TrafficSummary traffic = loadTrafficSummary(todayVn, rangeDays);
        FantasySummary fantasy = loadFantasySummary();
        List<RecentAccount> recent = loadRecentAccounts();

        return new Overview(now, rangeDays, accounts, traffic, fantasy, recent);
    }

    private AccountsSummary loadAccountsSummary(Instant now, LocalDate todayVn) {
        long total = queryLong(
                "SELECT COUNT(*) FROM accounts WHERE email <> ?",
                SystemAccount.EMAIL);
        Timestamp sevenDaysAgoUtc = Timestamp.valueOf(
                LocalDateTime.ofInstant(now.minus(Duration.ofDays(7)), ZoneOffset.UTC));
        long newLast7 = queryLong(
                "SELECT COUNT(*) FROM accounts WHERE email <> ? AND created_at >= ?",
                SystemAccount.EMAIL, sevenDaysAgoUtc);
        long activeToday = queryLong("""
                SELECT COUNT(DISTINCT d.account_id)
                FROM account_activity_days d
                JOIN accounts a ON a.id = d.account_id
                WHERE a.email <> ? AND d.stat_day = ?
                """, SystemAccount.EMAIL, Date.valueOf(todayVn));
        long active7 = queryLong("""
                SELECT COUNT(DISTINCT d.account_id)
                FROM account_activity_days d
                JOIN accounts a ON a.id = d.account_id
                WHERE a.email <> ? AND d.stat_day >= ? AND d.stat_day <= ?
                """, SystemAccount.EMAIL, Date.valueOf(todayVn.minusDays(6)), Date.valueOf(todayVn));
        long active30 = queryLong("""
                SELECT COUNT(DISTINCT d.account_id)
                FROM account_activity_days d
                JOIN accounts a ON a.id = d.account_id
                WHERE a.email <> ? AND d.stat_day >= ? AND d.stat_day <= ?
                """, SystemAccount.EMAIL, Date.valueOf(todayVn.minusDays(29)), Date.valueOf(todayVn));
        return new AccountsSummary(total, newLast7, activeToday, active7, active30);
    }

    private TrafficSummary loadTrafficSummary(LocalDate todayVn, int rangeDays) {
        long visitorsToday = queryLong(
                "SELECT COUNT(*) FROM site_visitors_daily WHERE stat_day = ?",
                Date.valueOf(todayVn));
        long visitorsLastWeek = queryLong(
                "SELECT COUNT(*) FROM site_visitors_daily WHERE stat_day = ?",
                Date.valueOf(todayVn.minusDays(7)));
        Integer deltaPercent = visitorsLastWeek == 0
                ? null
                : (int) Math.round(((visitorsToday - visitorsLastWeek) * 100.0) / visitorsLastWeek);

        LocalDate startDay = todayVn.minusDays(rangeDays - 1L);
        Map<LocalDate, Long> visitorsByDay = new HashMap<>();
        jdbc.query("""
                SELECT stat_day, COUNT(*) AS visitors
                FROM site_visitors_daily
                WHERE stat_day >= ? AND stat_day <= ?
                GROUP BY stat_day
                """, rs -> {
            visitorsByDay.put(rs.getDate("stat_day").toLocalDate(), rs.getLong("visitors"));
        }, Date.valueOf(startDay), Date.valueOf(todayVn));

        Map<LocalDate, Long> viewsByDay = new HashMap<>();
        jdbc.query("""
                SELECT stat_day, COALESCE(SUM(views), 0) AS total_views
                FROM site_page_views_daily
                WHERE stat_day >= ? AND stat_day <= ?
                GROUP BY stat_day
                """, rs -> {
            viewsByDay.put(rs.getDate("stat_day").toLocalDate(), rs.getLong("total_views"));
        }, Date.valueOf(startDay), Date.valueOf(todayVn));

        List<DailyPoint> series = new ArrayList<>(rangeDays);
        for (int i = 0; i < rangeDays; i++) {
            LocalDate day = startDay.plusDays(i);
            series.add(new DailyPoint(
                    day,
                    visitorsByDay.getOrDefault(day, 0L),
                    viewsByDay.getOrDefault(day, 0L)
            ));
        }

        List<TopPage> topPages = jdbc.query("""
                SELECT page_key, COALESCE(SUM(views), 0) AS total_views
                FROM site_page_views_daily
                WHERE stat_day >= ? AND stat_day <= ?
                GROUP BY page_key
                ORDER BY total_views DESC, page_key ASC
                """, (rs, idx) -> new TopPage(rs.getString("page_key"), rs.getLong("total_views")),
                Date.valueOf(startDay), Date.valueOf(todayVn));

        return new TrafficSummary(visitorsToday, visitorsLastWeek, deltaPercent, List.copyOf(series), List.copyOf(topPages));
    }

    private FantasySummary loadFantasySummary() {
        GameweekService.Overview gwOverview = gameweekService.overview();
        List<GameweekService.View> official = gwOverview.gameweeks().stream()
                .filter(v -> v.gameweek() >= 6 && v.gameweek() <= 38)
                .toList();

        GameweekService.View openView = official.stream()
                .filter(v -> v.configured() && v.status() == GameweekService.Status.OPEN)
                .max(Comparator.comparingInt(GameweekService.View::gameweek))
                .orElse(null);

        GameweekService.View publishedView = official.stream()
                .filter(v -> v.configured() && v.status() == GameweekService.Status.PUBLISHED)
                .max(Comparator.comparingInt(GameweekService.View::gameweek))
                .orElse(null);

        GameweekCard openCard = openView == null ? null : buildOpenCard(openView);
        GameweekCard publishedCard = publishedView == null ? null : buildPublishedCard(publishedView);

        Integer currentGw = openView != null ? Integer.valueOf(openView.gameweek()) : gwOverview.recommendedGameweek();
        Integer previousGw = currentGw != null && currentGw > 6 ? currentGw - 1 : null;

        long everSubmitted = queryLong("""
                SELECT COUNT(DISTINCT e.account_id)
                FROM fantasy_entries e
                JOIN accounts a ON a.id = e.account_id
                WHERE e.season = ? AND e.submitted_at IS NOT NULL AND a.email <> ?
                """, SEASON, SystemAccount.EMAIL);
        long currentSubmitted = currentGw == null ? 0L : queryLong("""
                SELECT COUNT(*)
                FROM fantasy_entries e
                JOIN accounts a ON a.id = e.account_id
                WHERE e.season = ? AND e.gameweek = ? AND e.submitted_at IS NOT NULL AND a.email <> ?
                """, SEASON, currentGw, SystemAccount.EMAIL);
        long previousSubmitted = previousGw == null ? 0L : queryLong("""
                SELECT COUNT(*)
                FROM fantasy_entries e
                JOIN accounts a ON a.id = e.account_id
                WHERE e.season = ? AND e.gameweek = ? AND e.submitted_at IS NOT NULL AND a.email <> ?
                """, SEASON, previousGw, SystemAccount.EMAIL);
        long currentDrafts = currentGw == null ? 0L : queryLong("""
                SELECT COUNT(*)
                FROM fantasy_entries e
                JOIN accounts a ON a.id = e.account_id
                WHERE e.season = ? AND e.gameweek = ?
                  AND e.draft_saved_at IS NOT NULL AND e.submitted_at IS NULL AND a.email <> ?
                """, SEASON, currentGw, SystemAccount.EMAIL);

        List<HistoryItem> history = loadMergedHistory();

        return new FantasySummary(
                fantasyAutomation.isEnabled(),
                fantasyAutomation.isDryRun(),
                openCard,
                publishedCard,
                new FantasyPlayersSummary(everSubmitted, currentGw, currentSubmitted, previousGw, previousSubmitted, currentDrafts),
                history
        );
    }

    private GameweekCard buildOpenCard(GameweekService.View view) {
        List<String> emails = jdbc.queryForList("""
                SELECT a.email
                FROM fantasy_deadline_changes c
                JOIN accounts a ON a.id = c.changed_by
                WHERE c.season = ? AND c.gameweek = ? AND c.revision = 1
                """, String.class, SEASON, view.gameweek());
        boolean bySystem = !emails.isEmpty() && SystemAccount.isSystemEmail(emails.getFirst());
        return new GameweekCard(
                view.gameweek(),
                view.status().name(),
                view.deadlineUtc(),
                view.deadlinePublishedAt(),
                bySystem,
                null,
                null
        );
    }

    private GameweekCard buildPublishedCard(GameweekService.View view) {
        record PublishedMeta(String email, Instant publishedAt) {
        }
        List<PublishedMeta> pubs = jdbc.query("""
                SELECT a.email, p.published_at
                FROM fantasy_result_publications p
                JOIN accounts a ON a.id = p.published_by
                WHERE p.season = ? AND p.gameweek = ?
                ORDER BY p.version DESC
                LIMIT 1
                """, (rs, idx) -> new PublishedMeta(
                rs.getString("email"),
                toInstant(rs.getTimestamp("published_at"))
        ), SEASON, view.gameweek());
        boolean bySystem = false;
        Instant publishedAt = null;
        if (!pubs.isEmpty()) {
            bySystem = SystemAccount.isSystemEmail(pubs.getFirst().email());
            publishedAt = pubs.getFirst().publishedAt();
        }
        int totalFixtures = (int) queryLong(
                "SELECT COUNT(*) FROM fixtures WHERE league_id = ? AND season_year = ? AND gameweek = ?",
                LEAGUE, SEASON, view.gameweek());
        int finishedFixtures = (int) queryLong(
                "SELECT COUNT(*) FROM fixtures WHERE league_id = ? AND season_year = ? AND gameweek = ? AND status = 'FINISHED'",
                LEAGUE, SEASON, view.gameweek());
        return new GameweekCard(
                view.gameweek(),
                view.status().name(),
                view.deadlineUtc(),
                publishedAt,
                bySystem,
                finishedFixtures,
                totalFixtures
        );
    }

    private List<HistoryItem> loadMergedHistory() {
        List<HistoryItem> items = new ArrayList<>();

        items.addAll(jdbc.query("""
                SELECT c.gameweek, c.revision, c.changed_at, c.reason, a.email AS actor_email
                FROM fantasy_deadline_changes c
                JOIN accounts a ON a.id = c.changed_by
                WHERE c.season = ?
                ORDER BY c.changed_at DESC
                LIMIT 20
                """, (rs, idx) -> {
            int revision = rs.getInt("revision");
            String email = rs.getString("actor_email");
            boolean isSystem = SystemAccount.isSystemEmail(email);
            return new HistoryItem(
                    revision == 1 ? "PUBLISH_DEADLINE" : "ADJUST_DEADLINE",
                    rs.getInt("gameweek"),
                    toInstant(rs.getTimestamp("changed_at")),
                    isSystem ? "SYSTEM" : "ADMIN",
                    isSystem ? SystemAccount.DISPLAY_NAME : "Admin",
                    rs.getString("reason")
            );
        }, SEASON));

        items.addAll(jdbc.query("""
                SELECT p.gameweek, p.action, p.published_at, p.reason, a.email AS actor_email
                FROM fantasy_result_publications p
                JOIN accounts a ON a.id = p.published_by
                WHERE p.season = ?
                ORDER BY p.published_at DESC
                LIMIT 20
                """, (rs, idx) -> {
            String action = rs.getString("action");
            String email = rs.getString("actor_email");
            boolean isSystem = SystemAccount.isSystemEmail(email);
            return new HistoryItem(
                    "RECALCULATE".equals(action) ? "RECALCULATE_RESULTS" : "PUBLISH_RESULTS",
                    rs.getInt("gameweek"),
                    toInstant(rs.getTimestamp("published_at")),
                    isSystem ? "SYSTEM" : "ADMIN",
                    isSystem ? SystemAccount.DISPLAY_NAME : "Admin",
                    rs.getString("reason")
            );
        }, SEASON));

        items.sort(Comparator.comparing(HistoryItem::occurredAtUtc).reversed());
        if (items.size() > 20) {
            return List.copyOf(items.subList(0, 20));
        }
        return List.copyOf(items);
    }

    private List<RecentAccount> loadRecentAccounts() {
        return jdbc.query("""
                SELECT a.display_name, a.created_at,
                       (SELECT COUNT(*) FROM account_identities i WHERE i.account_id = a.id) AS identity_count,
                       (SELECT MAX(d.stat_day) FROM account_activity_days d WHERE d.account_id = a.id) AS last_active_day
                FROM accounts a
                WHERE a.email <> ?
                ORDER BY a.created_at DESC, a.id DESC
                LIMIT 10
                """, (rs, idx) -> {
            Date lastDay = rs.getDate("last_active_day");
            return new RecentAccount(
                    rs.getString("display_name"),
                    rs.getInt("identity_count") > 0 ? "Google" : "Email",
                    toInstant(rs.getTimestamp("created_at")),
                    lastDay == null ? null : lastDay.toLocalDate()
            );
        }, SystemAccount.EMAIL);
    }

    private long queryLong(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    static Instant toInstant(Object value) {
        return switch (value) {
            case null -> null;
            case Instant instant -> instant;
            case Timestamp ts -> ts.toLocalDateTime().toInstant(ZoneOffset.UTC);
            case LocalDateTime ldt -> ldt.toInstant(ZoneOffset.UTC);
            case OffsetDateTime odt -> odt.toInstant();
            default -> throw new IllegalArgumentException(
                    "Unsupported timestamp type: " + value.getClass().getName());
        };
    }
}
