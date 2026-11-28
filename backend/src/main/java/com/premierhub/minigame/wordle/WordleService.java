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

    private static final long ABSENT_PUZZLE_TTL_SECONDS = 30;

    private final WordleRepository repository;
    private final Clock clock;
    private final Map<LocalDate, Puzzle> puzzleCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, DictionaryItem>> dictionaryCache = new ConcurrentHashMap<>();
    private final Map<String, AnswerView> answerDetailsCache = new ConcurrentHashMap<>();
    private volatile LocalDate absentPuzzleDate;
    private volatile Instant absentPuzzleCheckedAt;

    public WordleService(WordleRepository repository, @Qualifier("wordleClock") Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public record TimedInfoResult(InfoView view, String serverTiming) { }

    public InfoView info() {
        return infoTimed().view();
    }

    public TimedInfoResult infoTimed() {
        long tStart = System.nanoTime();
        Instant now = clock.instant();
        LocalDate today = today(now);
        Puzzle existing = puzzleCache.get(today);
        boolean cacheHit = false;
        long dbNs = 0L;
        if (existing != null) {
            cacheHit = true;
        } else if (today.equals(absentPuzzleDate)
                && absentPuzzleCheckedAt != null
                && !now.isAfter(absentPuzzleCheckedAt.plusSeconds(ABSENT_PUZZLE_TTL_SECONDS))) {
            cacheHit = true;
        } else {
            long tDbStart = System.nanoTime();
            existing = repository.findPuzzleByDate(today).orElse(null);
            dbNs = System.nanoTime() - tDbStart;
            if (existing != null) {
                cachePuzzle(existing);
            } else {
                absentPuzzleDate = today;
                absentPuzzleCheckedAt = now;
            }
        }
        InfoView view = new InfoView(
                true,
                existing == null ? null : existing.puzzleNumber(),
                today,
                existing == null ? null : existing.answerLetters().length(),
                existing == null ? null : WordleNames.wordShape(existing.answerWord()),
                nextDay(today),
                now
        );
        long totalNs = System.nanoTime() - tStart;
        String timing = cacheHit
                ? String.format(Locale.ROOT, "cache;desc=\"hit\", total;dur=%.2f", nsToMs(totalNs))
                : String.format(Locale.ROOT, "cache;desc=\"miss\", db;dur=%.2f, total;dur=%.2f", nsToMs(dbNs), nsToMs(totalNs));
        return new TimedInfoResult(view, timing);
    }

    @Transactional
    public TodayView today(long accountId) {
        Instant now = clock.instant();
        LocalDate today = today(now);
        Puzzle puzzle = ensureDailyPuzzle(today, now);
        Game game = findOrCreateGame(accountId, puzzle.id(), now);
        List<GuessView> guessViews = toGuessViews(repository.guesses(game.id(), puzzle.puzzleDate()));
        return toView(puzzle, game, now, guessViews, true);
    }

    public record TimedGuessResult(TodayView view, String serverTiming) { }

    @Transactional
    public TodayView submitGuess(long accountId, String rawLetters) {
        return submitGuessTimed(accountId, rawLetters, null).view();
    }

    @Transactional
    public TodayView submitGuess(long accountId, String rawLetters, LocalDate expectedDate) {
        return submitGuessTimed(accountId, rawLetters, expectedDate).view();
    }

    @Transactional
    public TimedGuessResult submitGuessTimed(long accountId, String rawLetters, LocalDate expectedDate) {
        long tTotalStart = System.nanoTime();
        Instant now = clock.instant();
        LocalDate today = today(now);
        if (expectedDate != null && !expectedDate.equals(today)) {
            throw failure(HttpStatus.CONFLICT, "GAME_FINISHED");
        }
        long tGameStart = System.nanoTime();
        Puzzle puzzle = ensureDailyPuzzle(today, now);
        Game game = findOrCreateGame(accountId, puzzle.id(), now);
        long gameNs = System.nanoTime() - tGameStart;
        return applyGuessTimed(puzzle, game, rawLetters, now, tTotalStart, gameNs);
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
        Game game = findOrCreateGame(accountId, puzzle.id(), now);
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
        Instant actionTime = requireSameDay(today);
        Game updated = new Game(game.id(), game.accountId(), game.puzzleId(),
                game.status(), true, game.startedAt(), game.finishedAt());
        repository.updateGame(updated);
        return toView(puzzle, updated, actionTime, toGuessViews(existing), false);
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
        puzzleCache.clear();
        dictionaryCache.clear();
        answerDetailsCache.clear();
        absentPuzzleDate = null;
        absentPuzzleCheckedAt = null;
    }

    private TodayView applyGuess(Puzzle puzzle, Game game, String rawLetters, Instant now) {
        return applyGuessTimed(puzzle, game, rawLetters, now, System.nanoTime(), 0L).view();
    }

    private TimedGuessResult applyGuessTimed(Puzzle puzzle, Game game, String rawLetters, Instant now,
                                             long tTotalStart, long gameNs) {
        LocalDate today = today(now);
        if (game.status() != Status.PLAYING || !puzzle.puzzleDate().equals(today)) {
            throw failure(HttpStatus.CONFLICT, "GAME_FINISHED");
        }
        if (rawLetters == null) {
            throw failure(HttpStatusCode.valueOf(422), "TOO_SHORT");
        }
        String normalized = rawLetters.trim().toUpperCase(Locale.ROOT);
        if (!normalized.isEmpty() && !normalized.matches("^[A-Z]+$")) {
            throw failure(HttpStatusCode.valueOf(422), "INVALID_CHARACTERS");
        }
        int maxLength = puzzle.answerLetters().length();
        if (normalized.length() < WordleData.MIN_ANSWER_LENGTH) {
            throw failure(HttpStatusCode.valueOf(422), "TOO_SHORT");
        }
        if (normalized.length() > maxLength) {
            throw failure(HttpStatusCode.valueOf(422), "TOO_LONG");
        }

        long tDictStart = System.nanoTime();
        Map<String, DictionaryItem> dict = loadDictionary(puzzle);
        DictionaryItem matched = dict.get(normalized);
        long dictNs = System.nanoTime() - tDictStart;
        if (matched == null) {
            throw failure(HttpStatusCode.valueOf(422), "NOT_A_PLAYER");
        }

        long tGuessesStart = System.nanoTime();
        List<GuessRow> existing = repository.guesses(game.id(), puzzle.puzzleDate());
        long guessesNs = System.nanoTime() - tGuessesStart;
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

        long tInsertStart = System.nanoTime();
        Instant actionTime = requireSameDay(today);
        repository.insertGuess(game.id(), turn, guessedWord, normalized, playerId, feedback, actionTime);
        long insertNs = System.nanoTime() - tInsertStart;

        Status nextStatus = isAnswer ? Status.WON : (turn >= WordleData.MAX_TURNS ? Status.LOST : Status.PLAYING);
        Game updated = game;
        long updateNs = 0L;
        if (nextStatus != Status.PLAYING) {
            long tUpdateStart = System.nanoTime();
            updated = new Game(game.id(), game.accountId(), game.puzzleId(),
                    nextStatus, game.hintUsed(), game.startedAt(), actionTime);
            repository.updateGame(updated);
            updateNs = System.nanoTime() - tUpdateStart;
        }

        long tViewStart = System.nanoTime();
        boolean finished = updated.status() != Status.PLAYING;
        AnswerView answerDetails = (finished || updated.hintUsed())
                ? loadAnswerDetails(puzzle)
                : null;
        String newPlayerName = isAnswer && answerDetails != null ? answerDetails.name() : matched.name();
        String newClub = isAnswer && answerDetails != null ? answerDetails.club() : matched.club();

        List<GuessView> guessViews = new ArrayList<>(existing.size() + 1);
        for (GuessRow g : existing) {
            guessViews.add(new GuessView(g.turn(), g.word(), g.letters(), g.feedback(), g.playerName(), g.club()));
        }
        guessViews.add(new GuessView(turn, guessedWord, normalized, feedback, newPlayerName, newClub));

        String hintClub = updated.hintUsed() && answerDetails != null ? answerDetails.club() : null;
        AnswerView answer = finished ? answerDetails : null;
        long statsNs = 0L;
        StatsView stats = null;
        if (finished) {
            long tStatsStart = System.nanoTime();
            stats = WordleStatsCalculator.compute(repository.outcomes(updated.accountId()), puzzle.puzzleDate());
            statsNs = System.nanoTime() - tStatsStart;
        }
        TodayView view = new TodayView(
                puzzle.puzzleNumber(),
                puzzle.puzzleDate(),
                puzzle.answerLetters().length(),
                WordleNames.wordShape(puzzle.answerWord()),
                WordleKeyCodec.encode(puzzle.answerLetters(), puzzle.id()),
                actionTime,
                nextDay(puzzle.puzzleDate()),
                updated.status(),
                updated.hintUsed(),
                hintClub,
                List.copyOf(guessViews),
                answer,
                stats
        );
        long viewNs = (System.nanoTime() - tViewStart) - statsNs;
        long totalNs = System.nanoTime() - tTotalStart;
        String timing = formatServerTiming(gameNs, dictNs, guessesNs, insertNs, updateNs, statsNs, viewNs, totalNs);
        return new TimedGuessResult(view, timing);
    }

    private static String formatServerTiming(long gameNs, long dictNs, long guessesNs, long insertNs,
                                             long updateNs, long statsNs, long viewNs, long totalNs) {
        return String.format(Locale.ROOT,
                "game;dur=%.2f, dict;dur=%.2f, guesses;dur=%.2f, insert;dur=%.2f, update;dur=%.2f, stats;dur=%.2f, view;dur=%.2f, total;dur=%.2f",
                nsToMs(gameNs), nsToMs(dictNs), nsToMs(guessesNs), nsToMs(insertNs),
                nsToMs(updateNs), nsToMs(statsNs), nsToMs(viewNs), nsToMs(totalNs));
    }

    private static double nsToMs(long ns) {
        return Math.max(0L, ns) / 1_000_000.0;
    }

    private Puzzle ensureDailyPuzzle(LocalDate date, Instant now) {
        Puzzle cached = puzzleCache.get(date);
        if (cached != null) {
            return cached;
        }
        var existing = repository.findPuzzleByDate(date);
        if (existing.isPresent()) {
            cachePuzzle(existing.get());
            return existing.get();
        }
        Cycle cycle = repository.lockCycle();
        existing = repository.findPuzzleByDate(date);
        if (existing.isPresent()) {
            cachePuzzle(existing.get());
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
        cachePuzzle(created);
        return created;
    }

    private void cachePuzzle(Puzzle puzzle) {
        if (puzzleCache.size() >= 4) {
            puzzleCache.clear();
        }
        puzzleCache.put(puzzle.puzzleDate(), puzzle);
        if (puzzle.puzzleDate().equals(absentPuzzleDate)) {
            absentPuzzleDate = null;
            absentPuzzleCheckedAt = null;
        }
    }

    private Game findOrCreateGame(long accountId, String puzzleId, Instant now) {
        var existing = repository.findGame(accountId, puzzleId, true);
        if (existing.isPresent()) {
            return existing.get();
        }
        repository.lockAccount(accountId);
        return repository.findGame(accountId, puzzleId, true)
                .orElseGet(() -> createGame(accountId, puzzleId, now));
    }

    private AnswerView loadAnswerDetails(Puzzle puzzle) {
        AnswerView cached = answerDetailsCache.get(puzzle.id());
        if (cached != null) {
            return cached;
        }
        AnswerView loaded = repository.answerDetails(puzzle).orElse(null);
        if (loaded != null) {
            if (answerDetailsCache.size() >= 4) {
                answerDetailsCache.clear();
            }
            answerDetailsCache.put(puzzle.id(), loaded);
        }
        return loaded;
    }

    private static List<GuessView> toGuessViews(List<GuessRow> rows) {
        return rows.stream()
                .map(g -> new GuessView(g.turn(), g.word(), g.letters(), g.feedback(), g.playerName(), g.club()))
                .toList();
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
            int len = norm.letters().length();
            if (len < WordleData.MIN_ANSWER_LENGTH || len > targetLen) {
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

    private TodayView toView(Puzzle puzzle, Game game, Instant now, List<GuessView> guesses, boolean includeStats) {
        boolean finished = game.status() != Status.PLAYING;
        AnswerView answerDetails = (finished || game.hintUsed())
                ? loadAnswerDetails(puzzle)
                : null;
        String hintClub = game.hintUsed() && answerDetails != null ? answerDetails.club() : null;
        AnswerView answer = finished ? answerDetails : null;
        StatsView stats = includeStats
                ? WordleStatsCalculator.compute(repository.outcomes(game.accountId()), puzzle.puzzleDate())
                : null;
        return new TodayView(
                puzzle.puzzleNumber(),
                puzzle.puzzleDate(),
                puzzle.answerLetters().length(),
                WordleNames.wordShape(puzzle.answerWord()),
                WordleKeyCodec.encode(puzzle.answerLetters(), puzzle.id()),
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
            case "TOO_SHORT" -> "Cần ít nhất 4 chữ cái.";
            case "TOO_LONG" -> "Tên quá dài so với đáp án hôm nay.";
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
