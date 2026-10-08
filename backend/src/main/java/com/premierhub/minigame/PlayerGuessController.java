package com.premierhub.minigame;

import com.premierhub.accounts.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import static com.premierhub.minigame.PlayerGuessData.*;

@RestController
@RequestMapping("/api/minigame/2026/player-guess")
@ConditionalOnProperty(name = "premierhub.minigame.enabled", havingValue = "true")
public class PlayerGuessController {
    private static final String UUID_PATTERN = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    public record Start(@NotNull @Pattern(regexp = UUID_PATTERN) String actionId,
                        @Pattern(regexp = UUID_PATTERN) String expectedGameId,
                        @NotNull @PositiveOrZero Long expectedVersion) { }
    public record Change(@NotNull @Pattern(regexp = UUID_PATTERN) String actionId,
                         @NotNull @PositiveOrZero Long expectedVersion) { }
    public record GuessRequest(@NotNull @Pattern(regexp = UUID_PATTERN) String actionId,
                               @NotNull @PositiveOrZero Long expectedVersion,
                               @NotNull @Positive Integer player_id) { }
    private final PlayerGuessService service;
    private final AccountService accounts;

    public PlayerGuessController(PlayerGuessService service, AccountService accounts) {
        this.service = service;
        this.accounts = accounts;
    }

    @GetMapping("/info")
    public ResponseEntity<PlayerGuessInfo> info() { return response(service.info()); }

    @GetMapping("/practice/current")
    public ResponseEntity<Current> practice(Authentication auth, @RequestHeader("X-PrismaXI-Account-ID") long expected) {
        return response(service.current(owner(auth, expected), Mode.PRACTICE));
    }
    @GetMapping("/daily/current")
    public ResponseEntity<Current> daily(Authentication auth, @RequestHeader("X-PrismaXI-Account-ID") long expected) {
        return response(service.current(owner(auth, expected), Mode.DAILY));
    }
    @PostMapping("/practice/start")
    public ResponseEntity<Mutation> startPractice(Authentication auth, @RequestHeader("X-PrismaXI-Account-ID") long expected,
                                                  @Valid @RequestBody Start body) {
        return mutation(service.start(owner(auth, expected), Mode.PRACTICE, key(body.actionId()),
                body.expectedGameId() == null ? null : key(body.expectedGameId()), body.expectedVersion()));
    }
    @PostMapping("/daily/start")
    public ResponseEntity<Mutation> startDaily(Authentication auth, @RequestHeader("X-PrismaXI-Account-ID") long expected,
                                               @Valid @RequestBody Start body) {
        return mutation(service.start(owner(auth, expected), Mode.DAILY, key(body.actionId()),
                body.expectedGameId() == null ? null : key(body.expectedGameId()), body.expectedVersion()));
    }
    @GetMapping("/games/{gameId}")
    public ResponseEntity<GameView> read(Authentication auth, @RequestHeader("X-PrismaXI-Account-ID") long expected,
                                          @PathVariable String gameId) {
        return response(service.read(owner(auth, expected), key(gameId)));
    }
    @PostMapping("/games/{gameId}/guesses")
    public ResponseEntity<Mutation> guess(Authentication auth, @RequestHeader("X-PrismaXI-Account-ID") long expected,
                                           @PathVariable String gameId, @Valid @RequestBody GuessRequest body) {
        return mutation(service.change(owner(auth, expected), key(gameId), key(body.actionId()), body.expectedVersion(), body.player_id()));
    }
    @PostMapping("/games/{gameId}/hints/next")
    public ResponseEntity<Mutation> hint(Authentication auth, @RequestHeader("X-PrismaXI-Account-ID") long expected,
                                          @PathVariable String gameId, @Valid @RequestBody Change body) {
        return mutation(service.change(owner(auth, expected), key(gameId), key(body.actionId()), body.expectedVersion(), null));
    }
    @GetMapping("/players")
    public ResponseEntity<?> players(Authentication auth, @RequestHeader("X-PrismaXI-Account-ID") long expected,
                                     @RequestParam String gameId, @RequestParam(defaultValue = "") String q) {
        if (q.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MINIGAME_INPUT");
        return response(service.players(owner(auth, expected), key(gameId), q));
    }
    @GetMapping("/daily/history")
    public ResponseEntity<History> history(Authentication auth, @RequestHeader("X-PrismaXI-Account-ID") long expected,
                                            @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "20") int limit) {
        page(offset, limit);
        return response(service.history(owner(auth, expected), offset, limit));
    }
    @GetMapping("/leaderboard")
    public ResponseEntity<Leaderboard> leaderboard(@RequestParam(defaultValue = "0") int offset,
                                                     @RequestParam(defaultValue = "20") int limit) {
        page(offset, limit);
        return response(service.leaderboard(offset, limit));
    }

    private long owner(Authentication auth, long expected) {
        long actual = accounts.current(auth.getName()).id();
        if (actual != expected) throw new ResponseStatusException(HttpStatus.CONFLICT, "SESSION_CHANGED");
        return actual;
    }
    private static String key(String value) {
        if (value == null || !value.matches(UUID_PATTERN)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MINIGAME_INPUT");
        return value.toLowerCase(java.util.Locale.ROOT);
    }
    private static void page(int offset, int limit) {
        if (offset < 0 || offset > 100000 || limit < 1 || limit > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MINIGAME_INPUT");
    }
    private static ResponseEntity<Mutation> mutation(Mutation result) {
        int status = switch (result.code()) {
            case "OK" -> 200;
            case "INVALID_PLAYER", "PLAYER_ALREADY_GUESSED", "ALL_HINTS_REVEALED" -> 422;
            default -> 409;
        };
        return ResponseEntity.status(status).header("Cache-Control", "no-store").body(result);
    }
    private static <T> ResponseEntity<T> response(T body) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(body);
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> error(ResponseStatusException failure) {
        String code = failure.getReason() == null ? "MINIGAME_REQUEST" : failure.getReason();
        return ResponseEntity.status(failure.getStatusCode()).header("Cache-Control", "no-store")
                .body(Map.of("code", code, "message", PlayerGuessService.message(code)));
    }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    ResponseEntity<?> inputError() {
        return ResponseEntity.badRequest().header("Cache-Control", "no-store")
                .body(Map.of("code", "MINIGAME_INPUT", "message", "Thiếu hoặc sai dữ liệu yêu cầu. Điểm và lượt không bị trừ."));
    }
}
