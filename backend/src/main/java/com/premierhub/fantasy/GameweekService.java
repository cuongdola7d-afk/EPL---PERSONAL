package com.premierhub.fantasy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static com.premierhub.fantasy.GameweekRepository.*;

@Service
public class GameweekService {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    public enum Status { OPEN, LOCKED, AWAITING_RESULTS, PUBLISHED }
    public record View(int season, int gameweek, String mode, boolean configured,
                       Instant deadlineUtc, Instant deadlinePublishedAt, Instant candidateDeadlineUtc,
                       Fixture firstFixture, boolean scheduleComplete, Status status, boolean canEdit,
                       int revision, List<Change> deadlineChanges, LocalDate rosterAsOf) { }
    public record Overview(Instant serverTimeUtc, Integer recommendedGameweek, List<View> gameweeks) { }
    private final GameweekRepository repository;
    private final Clock clock;

    public GameweekService(GameweekRepository repository, @Qualifier("fantasyGameweekClock") Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public static Instant calculateDeadline(Instant kickoffUtc) {
        return kickoffUtc.atZone(VIETNAM).toLocalDate().minusDays(1).atStartOfDay(VIETNAM).toInstant();
    }

    @Transactional(readOnly = true)
    public Overview overview() {
        var fixtures = repository.fixtures().stream().collect(Collectors.groupingBy(Fixture::gameweek));
        var configurations = repository.configurations().stream().collect(Collectors.toMap(Configuration::gameweek, Function.identity()));
        var changes = repository.changes().stream().collect(Collectors.groupingBy(Change::gameweek));
        // SQL/network waits can cross the deadline; decide with time after the reads.
        Instant now = clock.instant();
        var views = IntStream.rangeClosed(1, 38).mapToObj(gw -> view(gw, fixtures.getOrDefault(gw, List.of()),
                configurations.get(gw), changes.getOrDefault(gw, List.of()), now)).toList();
        Integer recommended = views.stream().filter(v -> v.gameweek() >= 6 && v.scheduleComplete()
                && (v.configured() ? v.canEdit() : v.candidateDeadlineUtc().isAfter(now)))
                .map(View::gameweek).findFirst().orElse(null);
        return new Overview(now, recommended, views);
    }

    public View info(int gameweek) {
        validateGameweek(gameweek);
        return overview().gameweeks().get(gameweek - 1);
    }

    private View view(int gw, List<Fixture> fixtures, Configuration configuration, List<Change> changes, Instant now) {
        Fixture first = fixtures.stream().filter(f -> f.kickoffUtc() != null)
                .min(Comparator.comparing(Fixture::kickoffUtc).thenComparingInt(Fixture::id)).orElse(null);
        // Every fixture needs its actual timestamp, including postponed fixtures, before initial publication.
        boolean complete = fixtures.size() == 10 && fixtures.stream().allMatch(f -> f.kickoffUtc() != null);
        Status status = configuration == null ? null : effectiveStatus(configuration, fixtures, now);
        return new View(2026, gw, gw <= 5 ? "REPLAY" : "OFFICIAL", configuration != null,
                configuration == null ? null : configuration.deadlineUtc(),
                configuration == null ? null : configuration.deadlinePublishedAt(),
                complete ? calculateDeadline(first.kickoffUtc()) : null, first, complete, status,
                status == Status.OPEN, configuration == null ? 0 : configuration.revision(), changes,
                configuration == null ? null : configuration.rosterAsOf());
    }

    static Status effectiveStatus(Configuration configuration, List<Fixture> fixtures, Instant now) {
        if ("PUBLISHED".equals(configuration.workflowStatus())) return Status.PUBLISHED;
        // Clock always overrides an old OPEN row: equality is locked, even without a cron.
        if (now.isBefore(configuration.deadlineUtc())) {
            return "OPEN".equals(configuration.workflowStatus()) ? Status.OPEN : Status.valueOf(configuration.workflowStatus());
        }
        boolean started = fixtures.stream().anyMatch(f -> List.of("FINISHED", "IN_PLAY", "PAUSED", "LIVE").contains(f.status())
                || ("SCHEDULED".equals(f.status()) && f.kickoffUtc() != null && !now.isBefore(f.kickoffUtc())));
        return started || "AWAITING_RESULTS".equals(configuration.workflowStatus()) ? Status.AWAITING_RESULTS : Status.LOCKED;
    }

    // Step 3 must call this while holding its team/config transaction lock immediately before writing.
    public void requireOpen(int gameweek) {
        if (!info(gameweek).canEdit()) throw conflict("GW chưa mở hoặc đã hết hạn chỉnh/chốt đội.");
    }

    // Call with the locked configuration after slow reads/writes; no additional SQL here.
    public Instant requireOpen(Configuration configuration) {
        Instant now = clock.instant();
        if (configuration == null || !"OPEN".equals(configuration.workflowStatus())
                || !now.isBefore(configuration.deadlineUtc()))
            throw conflict("GW chưa mở hoặc đã hết hạn chỉnh/chốt đội.");
        return now;
    }

    @Transactional
    public View publishDeadline(int gameweek, long actorId, String reason) {
        return publishDeadline(gameweek, actorId, reason, null);
    }

    @Transactional
    public View publishDeadline(int gameweek, long actorId, String reason, LocalDate selectedRosterAsOf) {
        validateOfficial(gameweek);
        reason = validateReason(reason);
        if (repository.lock(gameweek).isPresent()) throw conflict("Deadline đã công bố; dùng thao tác điều chỉnh có lý do.");
        var info = info(gameweek);
        if (!info.scheduleComplete()) throw conflict("Chưa đủ 10 trận có thời điểm UTC xác định để công bố deadline.");
        Instant now = clock.instant();
        LocalDate rosterAsOf = selectedRosterAsOf == null ? now.atZone(VIETNAM).toLocalDate() : selectedRosterAsOf;
        if (!repository.hasRoster(rosterAsOf))
            throw conflict("Chưa có dữ liệu roster hiệu lực tại ngày " + rosterAsOf + ". Chọn ngày đã có dữ liệu.");
        try { repository.create(gameweek, info.firstFixture(), info.candidateDeadlineUtc(), now, rosterAsOf); }
        catch (DuplicateKeyException duplicate) { throw conflict("Deadline đã được công bố bởi request khác."); }
        repository.audit(gameweek, 1, null, info.candidateDeadlineUtc(), now, actorId, reason);
        return info(gameweek);
    }

    public LocalDate rosterAsOf(int gameweek) {
        validateOfficial(gameweek);
        return repository.find(gameweek).orElseThrow(() -> conflict("GW chưa công bố mốc roster.")).rosterAsOf();
    }

    @Transactional
    public View adjustDeadline(int gameweek, Instant deadline, int expectedRevision, long actorId, String reason) {
        validateOfficial(gameweek);
        reason = validateReason(reason);
        var configuration = repository.lock(gameweek).orElseThrow(() -> conflict("GW chưa công bố deadline."));
        Instant now = clock.instant();
        if (!now.isBefore(configuration.deadlineUtc()) || !"OPEN".equals(configuration.workflowStatus()))
            throw conflict("Không mở lại hoặc điều chỉnh vòng đã khóa.");
        if (expectedRevision != configuration.revision()) throw conflict("Deadline đã thay đổi; tải lại trước khi điều chỉnh.");
        if (deadline == null || deadline.equals(configuration.deadlineUtc()) || !deadline.isAfter(now)
                || !deadline.isBefore(configuration.firstKickoffUtc()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deadline mới phải khác hạn cũ, ở tương lai và trước trận đầu đã công bố.");
        int revision = configuration.revision() + 1;
        repository.adjust(gameweek, deadline, revision, now);
        repository.audit(gameweek, revision, configuration.deadlineUtc(), deadline, now, actorId, reason);
        return info(gameweek);
    }

    private static void validateGameweek(int gameweek) {
        if (gameweek < 1 || gameweek > 38) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GW phải từ 1 đến 38.");
    }
    private static void validateOfficial(int gameweek) {
        validateGameweek(gameweek);
        if (gameweek <= 5) throw conflict("GW1–GW5 chỉ Replay; chưa triển khai cấu hình cuộc thi Replay.");
    }
    private static String validateReason(String reason) {
        if (reason == null || reason.strip().length() < 3 || reason.strip().length() > 500
                || reason.chars().anyMatch(Character::isISOControl))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần lý do công khai từ 3 đến 500 ký tự, không chứa thông tin riêng tư.");
        return reason.strip();
    }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
