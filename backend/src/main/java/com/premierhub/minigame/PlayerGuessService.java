package com.premierhub.minigame;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import static com.premierhub.minigame.PlayerGuessData.*;
import static com.premierhub.minigame.PlayerGuessRules.*;

@Service
@ConditionalOnProperty(name = "premierhub.minigame.enabled", havingValue = "true")
public class PlayerGuessService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<String> KEYS = List.of("height", "foot", "age", "ovr", "nationality", "position", "club", "shirtNumber");
    private static final List<String> LABELS = List.of("Chiều cao", "Chân thuận", "Tuổi", "OVR FC 27", "Quốc tịch", "Vị trí chính", "CLB", "Số áo");
    private final PlayerGuessRepository repository;
    private final Clock clock;

    public PlayerGuessService(PlayerGuessRepository repository, @Qualifier("playerGuessClock") Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public PlayerGuessInfo info() {
        Instant now = clock.instant();
        var hints = new ArrayList<Hint>();
        for (int i = 0; i < 8; i++) hints.add(new Hint(KEYS.get(i), LABELS.get(i), false, null));
        return new PlayerGuessInfo(2026, ZONE.getId(), now, nextDay(today(now)), true, 100, 3, 2, 10, 20, List.copyOf(hints));
    }

    @Transactional
    public Current current(long owner, Mode mode) {
        repository.lockAccount(owner);
        Instant now = clock.instant();
        expireOld(today(now), owner, now);
        var game = mode == Mode.DAILY ? repository.dailyGame(owner, today(now)) : repository.practice(owner);
        return new Current(game.isEmpty() ? "NOT_STARTED" : game.get().state().status().name(), today(now), now,
                nextDay(today(now)), game.map(value -> view(value, now)).orElse(null));
    }

    @Transactional
    public Mutation start(long owner, Mode mode, String actionId, String expectedGameId, long expectedVersion) {
        // Only starts acquire the selector lock. All start paths then lock account -> game.
        Cycle cycle = repository.lockCycle();
        repository.lockAccount(owner);
        Instant now = clock.instant();
        LocalDate date = today(now);
        expireOld(date, owner, now);
        String fingerprint = "START:" + mode + ":" + expectedGameId + ":" + expectedVersion;
        var replay = replay(owner, actionId, fingerprint, now);
        if (replay != null) return replay;
        var existing = mode == Mode.DAILY ? repository.dailyGame(owner, date) : repository.practice(owner);
        if (existing.isPresent() && (mode == Mode.DAILY || existing.get().state().status() == Status.IN_PROGRESS)) {
            repository.insertAction(owner, actionId, fingerprint, existing.get().id());
            return success(existing.get(), now, new Effect("RESUMED", 0, null, false));
        }
        if (mode == Mode.PRACTICE && (!Objects.equals(expectedGameId, existing.map(Game::id).orElse(null))
                || expectedVersion != existing.map(Game::version).orElse(0L)))
            throw failure(HttpStatus.CONFLICT, "VERSION_CONFLICT");

        List<Candidate> candidates = repository.candidates(date);
        List<Candidate> eligible = candidates.stream().filter(player -> player.eligible(date)).toList();
        List<PlayerChoice> choices = choices(candidates);
        Question daily = ensureDaily(date, eligible, choices, cycle);
        Question question = daily;
        if (mode == Mode.PRACTICE) {
            List<Candidate> pool = eligible.stream().filter(player -> player.playerId() != daily.snapshot().playerId()).toList();
            if (pool.isEmpty()) throw failure(HttpStatus.CONFLICT, "PRACTICE_POOL_TOO_SMALL");
            var excluded = new ArrayList<>(repository.recentPractice(owner));
            var available = pool.stream().filter(player -> !excluded.contains(player.playerId())).toList();
            while (available.isEmpty() && !excluded.isEmpty()) {
                excluded.removeLast(); // Relax the oldest recent answer, never the daily exclusion.
                available = pool.stream().filter(player -> !excluded.contains(player.playerId())).toList();
            }
            Candidate selected = available.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(available.size()));
            question = new Question(uuid(), mode, date, null, selected.snapshot(date, choices));
            repository.insertQuestion(question);
        }
        now = requireSameDay(date);
        var game = new Game(uuid(), owner, question.id(), mode, mode == Mode.DAILY ? date : null,
                initial(), 0, now, null);
        repository.insertGame(game);
        if (mode == Mode.PRACTICE) repository.pointPractice(owner, game.id());
        repository.insertAction(owner, actionId, fingerprint, game.id());
        Mutation result = success(game, now, new Effect("STARTED", 0, null, false));
        requireSameDay(date); // No further SQL after this check; late writes/reads roll back together.
        return result;
    }

    private Question ensureDaily(LocalDate date, List<Candidate> eligible, List<PlayerChoice> choices, Cycle cycle) {
        var existing = repository.daily(date);
        if (existing.isPresent()) return existing.get();
        if (eligible.isEmpty()) throw failure(HttpStatus.CONFLICT, "INSUFFICIENT_DATA");
        var byId = new LinkedHashMap<Integer, Candidate>();
        eligible.forEach(player -> byId.put(player.playerId(), player));
        // The remaining queue is frozen for a cycle; only still-eligible members can be used today.
        var remaining = new ArrayList<>(cycle.remaining().stream().filter(byId::containsKey).toList());
        long number = cycle.number();
        if (remaining.isEmpty()) {
            remaining.addAll(byId.keySet());
            Collections.shuffle(remaining);
            number++;
            if (remaining.size() > 1 && Objects.equals(remaining.getFirst(), cycle.lastPlayerId()))
                Collections.swap(remaining, 0, 1);
        }
        int answer = remaining.removeFirst();
        var question = new Question(uuid(), Mode.DAILY, date, nextDay(date), byId.get(answer).snapshot(date, choices));
        repository.insertQuestion(question);
        repository.saveCycle(new Cycle(number, List.copyOf(remaining), answer));
        return question;
    }

    @Transactional
    public GameView read(long owner, String id) {
        repository.lockAccount(owner);
        Game game = owned(owner, id);
        Instant now = clock.instant();
        return view(expire(game, now), now);
    }

    @Transactional
    public Mutation change(long owner, String id, String actionId, long expectedVersion, Integer playerId) {
        repository.lockAccount(owner);
        Game game = owned(owner, id);
        Instant now = clock.instant(); // Time after queued row locks.
        game = expire(game, now);
        String fingerprint = (playerId == null ? "HINT:" : "GUESS:" + playerId + ":") + id + ":" + expectedVersion;
        var replay = replay(owner, actionId, fingerprint, now);
        if (replay != null) return replay;
        if (game.state().status() != Status.IN_PROGRESS)
            return error(game, now, game.state().status() == Status.EXPIRED ? "GAME_EXPIRED" : "GAME_FINISHED");
        if (game.version() != expectedVersion) return error(game, now, "VERSION_CONFLICT");
        if (playerId == null && game.state().revealedHints() == 8) return error(game, now, "ALL_HINTS_REVEALED");

        Question question = repository.question(game.questionId());
        PlayerChoice choice = null;
        if (playerId != null) {
            choice = question.snapshot().choices().stream().filter(player -> player.playerId() == playerId).findFirst().orElse(null);
            if (choice == null) return error(game, now, "INVALID_PLAYER");
            if (repository.guesses(id).stream().anyMatch(guess -> guess.playerId() == playerId))
                return error(game, now, "PLAYER_ALREADY_GUESSED");
        }
        // Validation/SQL can also take time. Expiry must commit even when the requested action is rejected.
        now = clock.instant();
        game = expire(game, now);
        if (game.state().status() == Status.EXPIRED) return error(game, now, "GAME_EXPIRED");
        boolean correct = playerId != null && playerId == question.snapshot().playerId();
        State next = playerId == null ? reveal(game.state()) : guess(game.state(), correct);
        String revealed = next.revealedHints() > game.state().revealedHints() ? KEYS.get(next.revealedHints() - 1) : null;
        int delta = next.score() - game.state().score();
        var updated = new Game(id, owner, game.questionId(), game.mode(), game.dailyDate(), next,
                game.version() + 1, game.createdAt(), next.status() == Status.IN_PROGRESS ? null : now);
        repository.updateGame(updated);
        if (choice != null) repository.insertGuess(id, new Guess(next.guessesUsed(), playerId, choice.name(), correct, correct ? 0 : 20, revealed));
        repository.recordResult(updated);
        repository.insertAction(owner, actionId, fingerprint, id);
        Mutation result = success(updated, now, new Effect(playerId == null ? "HINT_REVEALED" : correct ? "CORRECT" : "WRONG", delta, revealed, false));
        if (game.mode() == Mode.DAILY && !clock.instant().isBefore(question.expiresAt()))
            throw failure(HttpStatus.CONFLICT, "DAY_CHANGED_RETRY"); // Transaction rolls back the tentative guess, including ledger.
        return result;
    }

    @Transactional
    public List<PlayerChoice> players(long owner, String gameId, String query) {
        repository.lockAccount(owner);
        var game = expire(owned(owner, gameId), clock.instant());
        var choices = repository.question(game.questionId()).snapshot().choices();
        String keyword = normalize(query == null ? "" : query.strip());
        return choices.stream().filter(player -> normalize(player.name()).contains(keyword)).toList();
    }

    @Transactional
    public History history(long owner, int offset, int limit) {
        repository.lockAccount(owner);
        Instant now = clock.instant();
        expireOld(today(now), owner, now);
        return new History(repository.history(owner, offset, limit).stream().map(game -> view(game, now)).toList(), offset, limit);
    }

    @Transactional
    public Leaderboard leaderboard(int offset, int limit) {
        Instant now = clock.instant();
        // Account -> game, also here: result INSERTs check the account FK on MySQL.
        // Lock owners in ascending order when normalizing several accounts in one transaction.
        for (long owner : repository.expiredOwners(today(now))) {
            repository.lockAccount(owner);
            expireOld(today(now), owner, clock.instant());
        }
        return new Leaderboard(2026, now, repository.standings(offset, limit), offset, limit);
    }

    private Mutation replay(long owner, String actionId, String fingerprint, Instant now) {
        var existing = repository.action(owner, actionId);
        if (existing.isEmpty()) return null;
        if (!existing.get().fingerprint().equals(fingerprint)) throw failure(HttpStatus.CONFLICT, "ACTION_KEY_REUSED");
        Game game = expire(owned(owner, existing.get().gameId()), now);
        return success(game, now, new Effect("REPLAY", 0, null, true));
    }

    private Game owned(long owner, String id) {
        return repository.game(owner, id, true).orElseThrow(() -> failure(HttpStatus.NOT_FOUND, "GAME_NOT_FOUND"));
    }

    private void expireOld(LocalDate date, long owner, Instant now) {
        for (Game game : repository.expiredGames(date, owner)) expire(game, now);
    }

    private Game expire(Game game, Instant now) {
        if (game.mode() != Mode.DAILY || game.state().status() != Status.IN_PROGRESS
                || now.isBefore(nextDay(game.dailyDate()))) return game;
        var expired = new Game(game.id(), game.accountId(), game.questionId(), game.mode(), game.dailyDate(),
                PlayerGuessRules.expire(game.state()), game.version() + 1, game.createdAt(), nextDay(game.dailyDate()));
        repository.updateGame(expired);
        repository.recordResult(expired);
        return expired;
    }

    private GameView view(Game game, Instant now) {
        Snapshot snapshot = repository.question(game.questionId()).snapshot();
        State state = game.state();
        boolean playing = state.status() == Status.IN_PROGRESS;
        String foot = switch (snapshot.preferredFoot()) { case "LEFT" -> "Trái"; case "RIGHT" -> "Phải"; default -> "Hai chân"; };
        List<String> values = List.of(snapshot.heightCm() + " cm", foot, snapshot.age() + " tuổi",
                String.valueOf(snapshot.fc27Overall()), snapshot.nationality(), snapshot.primaryPosition(),
                snapshot.club(), "#" + snapshot.shirtNumber());
        var hints = new ArrayList<Hint>();
        for (int i = 0; i < 8; i++) {
            boolean open = !playing || i < state.revealedHints();
            hints.add(new Hint(KEYS.get(i), LABELS.get(i), open, open ? values.get(i) : null));
        }
        Answer answer = playing ? null : new Answer(snapshot.playerId(), snapshot.name(), snapshot.clubId(),
                snapshot.club(), snapshot.primaryPosition(), snapshot.shirtNumber());
        return new GameView(game.id(), game.accountId(), 2026, game.mode(), snapshot.questionDate(), state.status(), game.version(),
                state.score(), state.finalScore(), state.guessesUsed(), 3 - state.guessesUsed(), state.revealedHints(), 8,
                playing, playing && state.revealedHints() < 8, playing && state.revealedHints() < 8 ? KEYS.get(state.revealedHints()) : null,
                now, game.mode() == Mode.DAILY ? nextDay(game.dailyDate()) : null, nextDay(today(now)),
                List.copyOf(hints), repository.guesses(game.id()), answer);
    }

    private Mutation success(Game game, Instant now, Effect effect) { return new Mutation("OK", null, view(game, now), effect); }
    private Mutation error(Game game, Instant now, String code) { return new Mutation(code, message(code), view(game, now), null); }
    private Instant requireSameDay(LocalDate date) {
        Instant now = clock.instant();
        if (!today(now).equals(date)) throw failure(HttpStatus.CONFLICT, "DAY_CHANGED_RETRY");
        return now;
    }
    private static List<PlayerChoice> choices(List<Candidate> candidates) {
        return candidates.stream().filter(player -> player.membershipCount() == 1 && player.name() != null && !player.name().isBlank())
                .map(player -> new PlayerChoice(player.playerId(), player.name())).distinct().toList();
    }
    static LocalDate today(Instant now) { return now.atZone(ZONE).toLocalDate(); }
    static Instant nextDay(LocalDate date) { return date.plusDays(1).atStartOfDay(ZONE).toInstant(); }
    static String normalize(String text) {
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT).replace('đ', 'd');
    }
    private static String uuid() { return UUID.randomUUID().toString(); }
    private static ResponseStatusException failure(HttpStatus status, String code) { return new ResponseStatusException(status, code); }
    public static String message(String code) {
        return switch (code) {
            case "INSUFFICIENT_DATA" -> "Chưa đủ dữ liệu để tạo câu hỏi.";
            case "PRACTICE_POOL_TOO_SMALL" -> "Chưa đủ cầu thủ khác để tạo ván luyện tập.";
            case "INVALID_PLAYER" -> "Hãy chọn cầu thủ hợp lệ từ danh sách.";
            case "PLAYER_ALREADY_GUESSED" -> "Bạn đã đoán cầu thủ này. Điểm và lượt không bị trừ.";
            case "ALL_HINTS_REVEALED" -> "Bạn đã mở hết gợi ý.";
            case "GAME_EXPIRED" -> "Ván hằng ngày đã hết hạn.";
            case "GAME_FINISHED" -> "Ván đã kết thúc.";
            case "VERSION_CONFLICT" -> "Ván đã thay đổi ở tab hoặc thiết bị khác. Hãy tải lại.";
            case "ACTION_KEY_REUSED" -> "Mã hành động đã được dùng với nội dung khác.";
            case "DAY_CHANGED_RETRY" -> "Ngày đã đổi trong lúc xử lý. Hãy tải lại và thử tiếp.";
            case "SESSION_CHANGED" -> "Phiên đã đổi tài khoản. Hãy tải lại.";
            case "GAME_NOT_FOUND" -> "Không tìm thấy ván của bạn.";
            default -> "Dữ liệu yêu cầu không hợp lệ.";
        };
    }
}
