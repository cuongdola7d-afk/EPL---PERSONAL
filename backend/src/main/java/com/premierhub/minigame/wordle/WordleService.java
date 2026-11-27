package com.premierhub.minigame.wordle;

import com.premierhub.minigame.wordle.WordleData.AnswerView;
import com.premierhub.minigame.wordle.WordleData.Candidate;
import com.premierhub.minigame.wordle.WordleData.Cycle;
import com.premierhub.minigame.wordle.WordleData.DictionaryEntry;
import com.premierhub.minigame.wordle.WordleData.DictionaryItem;
import com.premierhub.minigame.wordle.WordleData.Game;
import com.premierhub.minigame.wordle.WordleData.GuessRow;
import com.premierhub.minigame.wordle.WordleData.GuessView;
import com.premierhub.minigame.wordle.WordleData.InfoView;
import com.premierhub.minigame.wordle.WordleData.Puzzle;
import com.premierhub.minigame.wordle.WordleData.StatsView;
import com.premierhub.minigame.wordle.WordleData.Status;
import com.premierhub.minigame.wordle.WordleData.TodayView;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "premierhub.wordle.enabled", havingValue = "true")
public class WordleService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final WordleRepository repository;
    private final Clock clock;
    private final Map<String, Map<String, DictionaryItem>> dictionaryCache = new ConcurrentHashMap<>();

    public WordleService(WordleRepository repository, @Qualifier("wordleClock") Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public InfoView info() {
        Instant now = clock.instant();
        LocalDate today = today(now);
        Puzzle existing = repository.findPuzzleByDate(today).orElse(null);
        return new InfoView(
                true,
                existing == null ? null : existing.puzzleNumber(),
                today,
                existing == null ? null : existing.answerLetters().length(),
                nextDay(today),
                now
        );
    }

    @Transactional
    public TodayView today(long accountId) {
        Instant now = clock.instant();
        LocalDate today = today(now);
        Puzzle puzzle = ensureDailyPuzzle(today, now);
        repository.lockAccount(accountId);
        Game game = repository.findGame(accountId, puzzle.id(), true)
                .orElseGet(() -> createGame(accountId, puzzle.id(), now));
        return toView(puzzle, game, now);
    }

    @Transactional
    public TodayView submitGuess(long accountId, String rawLetters) {
        return submitGuess(accountId, rawLetters, null);
    }

    @Transactional
    public TodayView submitGuess(long accountId, String rawLetters, LocalDate expectedDate) {
        Instant now = clock.instant();
        LocalDate today = today(now);
        if (expectedDate != null && !expectedDate.equals(today)) {
            throw failure(HttpStatus.CONFLICT, "GAME_FINISHED");
        }
        Puzzle puzzle = ensureDailyPuzzle(today, now);
        repository.lockAccount(accountId);
        Game game = repository.findGame(accountId, puzzle.id(), true)
                .orElseGet(() -> createGame(accountId, puzzle.id(), now));
        return applyGuess(puzzle, game, rawLetters, now);
    }

    /**
     * Explicit game-id entry point (used to verify past-midnight locking of yesterday's PLAYING game).
     */
    @Transactional
    public TodayView submitGuessForGame(long accountId, String gameId, String rawLetters) {
        repository.lockAccount(accountId);
        Instant now = clock.instant();
        LocalDate today = today(now);
        Game game = repository.findGameById(accountId, gameId, true)
                .orElseThrow(() -> failure(HttpStatus.NOT_FOUND, "GAME_NOT_FOUND"));
        Puzzle puzzle = repository.findPuzzleById(game.puzzleId())
                .orElseThrow(() -> failure(HttpStatus.NOT_FOUND, "GAME_NOT_FOUND"));
        if (!puzzle.puzzleDate().equals(today)) {
            throw failure(HttpStatus.CONFLICT, "GAME_FINISHED");
        }
        return applyGuess(puzzle, game, rawLetters, now);
    }

    @Transactional
    public TodayView revealHint(long accountId) {
        return revealHint(accountId, null);
    }

    @Transactional
    public TodayView revealHint(long accountId, LocalDate expectedDate) {
        Instant now = clock.instant();
        LocalDate today = today(now);
        if (expectedDate != null && !expectedDate.equals(today)) {
            throw failure(HttpStatus.CONFLICT, "GAME_FINISHED");
        }
        Puzzle puzzle = ensureDailyPuzzle(today, now);
        repository.lockAccount(accountId);
        Game game = repository.findGame(accountId, puzzle.id(), true)
                .orElseGet(() -> createGame(accountId, puzzle.id(), now));
        if (game.status() != Status.PLAYING) {
            throw failure(HttpStatus.CONFLICT, "GAME_FINISHED");
        }
        if (game.hintUsed()) {
            throw failure(HttpStatus.CONFLICT, "HINT_USED");
        }
        List<GuessRow> existing = repository.guesses(game.id(), puzzle.puzzleDate());
        if (existing.size() < WordleData.HINT_UNLOCK_TURNS) {
            throw failure(HttpStatusCode.valueOf(422), "HINT_LOCKED");
        }
        Game updated = new Game(game.id(), game.accountId(), game.puzzleId(),
                game.status(), true, game.startedAt(), game.finishedAt());
        repository.updateGame(updated);
        requireSameDay(today);
        return toView(puzzle, updated, clock.instant());
    }

    @Transactional
    public List<DictionaryEntry> dictionary() {
        Instant now = clock.instant();
        LocalDate today = today(now);
        Puzzle puzzle = ensureDailyPuzzle(today, now);
        return loadDictionary(puzzle).values().stream()
                .map(DictionaryItem::toPublicEntry)
                .toList();
    }

    @Transactional(readOnly = true)
    public StatsView stats(long accountId) {
        LocalDate today = today(clock.instant());
        return WordleStatsCalculator.compute(repository.outcomes(accountId), today);
    }

    void evictDictionaryCache() {
        dictionaryCache.clear();
    }

    private TodayView applyGuess(Puzzle puzzle, Game game, String rawLetters, Instant now) {
        LocalDate today = today(now);
        if (game.status() != Status.PLAYING || !puzzle.puzzleDate().equals(today)) {
            throw failure(HttpStatus.CONFLICT, "GAME_FINISHED");
        }
        if (rawLetters == null) {
            throw failure(HttpStatusCode.valueOf(422), "WRONG_LENGTH");
        }
        String normalized = rawLetters.trim().toUpperCase(Locale.ROOT);
        if (!normalized.isEmpty() && !normalized.matches("^[A-Z]+$")) {
            throw failure(HttpStatusCode.valueOf(422), "INVALID_CHARACTERS");
        }
        int expectedLength = puzzle.answerLetters().length();
        if (normalized.length() != expectedLength) {
            throw failure(HttpStatusCode.valueOf(422), "WRONG_LENGTH");
        }

        Map<String, DictionaryItem> dict = loadDictionary(puzzle);
        DictionaryItem matched = dict.get(normalized);
        if (matched == null) {
            throw failure(HttpStatusCode.valueOf(422), "NOT_A_PLAYER");
        }

        List<GuessRow> existing = repository.guesses(game.id(), puzzle.puzzleDate());
        if (existing.stream().anyMatch(g -> g.letters().equals(normalized))) {
            throw failure(HttpStatusCode.valueOf(422), "ALREADY_GUESSED");
        }
        if (existing.size() >= WordleData.MAX_TURNS) {
            throw failure(HttpStatus.CONFLICT, "GAME_FINISHED");
        }

        boolean isAnswer = normalized.equals(puzzle.answerLetters());
        int playerId = isAnswer ? puzzle.answerPlayerId() : matched.playerId();
        String guessedWord = isAnswer ? puzzle.answerWord() : matched.word();
        String feedback = WordleScorer.score(normalized, puzzle.answerLetters());
        int turn = existing.size() + 1;

        Instant actionTime = requireSameDay(today);
        repository.insertGuess(game.id(), turn, guessedWord, normalized, playerId, feedback, actionTime);

        Status nextStatus = isAnswer ? Status.WON : (turn >= WordleData.MAX_TURNS ? Status.LOST : Status.PLAYING);
        Game updated = game;
        if (nextStatus != Status.PLAYING) {
            updated = new Game(game.id(), game.accountId(), game.puzzleId(),
                    nextStatus, game.hintUsed(), game.startedAt(), actionTime);
            repository.updateGame(updated);
        }
        requireSameDay(today);
        return toView(puzzle, updated, actionTime);
    }

    private Puzzle ensureDailyPuzzle(LocalDate date, Instant now) {
        var existing = repository.findPuzzleByDate(date);
        if (existing.isPresent()) {
            return existing.get();
        }
        Cycle cycle = repository.lockCycle();
        existing = repository.findPuzzleByDate(date);
        if (existing.isPresent()) {
            return existing.get();
        }
        List<Candidate> eligible = repository.candidates(date).stream()
                .filter(Candidate::eligibleAnswer)
                .toList();
        if (eligible.isEmpty()) {
            throw failure(HttpStatus.CONFLICT, "INSUFFICIENT_DATA");
        }
        Map<Integer, Candidate> byId = new LinkedHashMap<>();
        eligible.forEach(c -> byId.put(c.playerId(), c));

        List<Integer> remaining = new ArrayList<>(cycle.remaining().stream().filter(byId::containsKey).toList());
        long cycleNumber = cycle.number();
        if (remaining.isEmpty()) {
            remaining.addAll(byId.keySet());
            Collections.shuffle(remaining);
            cycleNumber++;
            if (remaining.size() > 1 && Objects.equals(remaining.getFirst(), cycle.lastPlayerId())) {
                Collections.swap(remaining, 0, 1);
            }
        }
        int answerPlayerId = remaining.removeFirst();
        Candidate answer = byId.get(answerPlayerId);
        var normalized = answer.normalized();
        int nextNumber = repository.maxPuzzleNumber() + 1;
        Puzzle created = new Puzzle(
                UUID.randomUUID().toString(),
                date,
                nextNumber,
                answerPlayerId,
                normalized.word(),
                normalized.letters(),
                now
        );
        repository.insertPuzzle(created);
        repository.saveCycle(new Cycle(cycleNumber, List.copyOf(remaining), answerPlayerId));
        return created;
    }

    private Game createGame(long accountId, String puzzleId, Instant now) {
        Game game = new Game(UUID.randomUUID().toString(), accountId, puzzleId, Status.PLAYING, false, now, null);
        repository.insertGame(game);
        return game;
    }

    private Map<String, DictionaryItem> loadDictionary(Puzzle puzzle) {
        Map<String, DictionaryItem> cached = dictionaryCache.get(puzzle.id());
        if (cached != null) {
            return cached;
        }
        int targetLen = puzzle.answerLetters().length();
        List<Candidate> active = repository.candidates(puzzle.puzzleDate()).stream()
                .filter(Candidate::activeSingleClub)
                .toList();
        Map<String, DictionaryItem> bestByLetters = new LinkedHashMap<>();
        for (Candidate c : active) {
            var norm = c.normalized();
            if (norm.letters().length() != targetLen) {
                continue;
            }
            DictionaryItem candidateItem = new DictionaryItem(
                    c.playerId(), norm.letters(), norm.word(), c.name(), c.club(), c.fc27Overall());
            DictionaryItem current = bestByLetters.get(norm.letters());
            if (current == null || isBetterDisplayPlayer(candidateItem, current)) {
                bestByLetters.put(norm.letters(), candidateItem);
            }
        }
        // Sort alphabetically by letters so order never leaks anything about the answer.
        Map<String, DictionaryItem> sorted = new LinkedHashMap<>();
        bestByLetters.values().stream()
                .sorted(Comparator.comparing(DictionaryItem::letters))
                .forEach(item -> sorted.put(item.letters(), item));
        Map<String, DictionaryItem> immutable = Collections.unmodifiableMap(sorted);
        if (dictionaryCache.size() >= 4) {
            dictionaryCache.clear();
        }
        dictionaryCache.put(puzzle.id(), immutable);
        return immutable;
    }

    private static boolean isBetterDisplayPlayer(DictionaryItem incoming, DictionaryItem current) {
        int inOvr = incoming.fc27Overall() == null ? -1 : incoming.fc27Overall();
        int curOvr = current.fc27Overall() == null ? -1 : current.fc27Overall();
        if (inOvr != curOvr) {
            return inOvr > curOvr;
        }
        return incoming.playerId() < current.playerId();
    }

    private TodayView toView(Puzzle puzzle, Game game, Instant now) {
        List<GuessView> guesses = repository.guesses(game.id(), puzzle.puzzleDate()).stream()
                .map(g -> new GuessView(g.turn(), g.word(), g.letters(), g.feedback(), g.playerName(), g.club()))
                .toList();
        boolean finished = game.status() != Status.PLAYING;
        AnswerView answerDetails = (finished || game.hintUsed())
                ? repository.answerDetails(puzzle).orElse(null)
                : null;
        String hintClub = game.hintUsed() && answerDetails != null ? answerDetails.club() : null;
        AnswerView answer = finished ? answerDetails : null;
        StatsView stats = WordleStatsCalculator.compute(repository.outcomes(game.accountId()), puzzle.puzzleDate());
        return new TodayView(
                puzzle.puzzleNumber(),
                puzzle.puzzleDate(),
                puzzle.answerLetters().length(),
                now,
                nextDay(puzzle.puzzleDate()),
                game.status(),
                game.hintUsed(),
                hintClub,
                guesses,
                answer,
                stats
        );
    }

    private Instant requireSameDay(LocalDate expectedDate) {
        Instant now = clock.instant();
        if (!today(now).equals(expectedDate)) {
            throw failure(HttpStatus.CONFLICT, "GAME_FINISHED");
        }
        return now;
    }

    public static LocalDate today(Instant now) {
        return now.atZone(ZONE).toLocalDate();
    }

    public static Instant nextDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZONE).toInstant();
    }

    private static ResponseStatusException failure(HttpStatusCode status, String code) {
        return new ResponseStatusException(status, code);
    }

    public static String message(String code) {
        return switch (code) {
            case "WRONG_LENGTH" -> "Số chữ cái không khớp với đáp án hôm nay.";
            case "INVALID_CHARACTERS" -> "Chỉ được nhập chữ cái không dấu A–Z.";
            case "NOT_A_PLAYER" -> "Không có cầu thủ Premier League nào có tên như vậy.";
            case "ALREADY_GUESSED" -> "Bạn đã đoán tên này rồi.";
            case "GAME_FINISHED" -> "Ván hôm nay đã kết thúc hoặc đã qua ngày mới.";
            case "HINT_LOCKED" -> "Gợi ý CLB chỉ mở sau lượt đoán thứ 4.";
            case "HINT_USED" -> "Bạn đã mở gợi ý CLB cho ván hôm nay rồi.";
            case "SESSION_CHANGED" -> "Phiên đã đổi tài khoản. Hãy tải lại.";
            case "INSUFFICIENT_DATA" -> "Chưa đủ dữ liệu cầu thủ để tạo câu đố Wordle.";
            case "GAME_NOT_FOUND" -> "Không tìm thấy ván Wordle.";
            default -> "Dữ liệu yêu cầu không hợp lệ.";
        };
    }
}
