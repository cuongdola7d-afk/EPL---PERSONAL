package com.premierhub.minigame.wordle;

import com.premierhub.accounts.AccountService;
import com.premierhub.minigame.wordle.WordleData.DictionaryEntry;
import com.premierhub.minigame.wordle.WordleData.InfoView;
import com.premierhub.minigame.wordle.WordleData.StatsView;
import com.premierhub.minigame.wordle.WordleData.TodayView;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/minigame/2026/wordle")
@ConditionalOnProperty(name = "premierhub.wordle.enabled", havingValue = "true")
public class WordleController {
    public record GuessRequest(String letters, LocalDate date) { }
    public record HintRequest(LocalDate date) { }

    private final WordleService service;
    private final AccountService accounts;

    public WordleController(WordleService service, AccountService accounts) {
        this.service = service;
        this.accounts = accounts;
    }

    @GetMapping("/info")
    public ResponseEntity<InfoView> info() {
        var timed = service.infoTimed();
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=30")
                .header("Server-Timing", timed.serverTiming())
                .body(timed.view());
    }

    @GetMapping("/today")
    public ResponseEntity<TodayView> today(Authentication auth,
                                           @RequestHeader("X-PrismaXI-Account-ID") long expected) {
        return noStore(service.today(owner(auth, expected)));
    }

    @PostMapping("/today/guesses")
    public ResponseEntity<TodayView> guess(Authentication auth,
                                           @RequestHeader("X-PrismaXI-Account-ID") long expected,
                                           @RequestBody(required = false) GuessRequest body) {
        String letters = body == null ? null : body.letters();
        LocalDate date = body == null ? null : body.date();
        var timed = service.submitGuessTimed(owner(auth, expected), letters, date);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("Server-Timing", timed.serverTiming())
                .body(timed.view());
    }

    @PostMapping("/today/hint")
    public ResponseEntity<TodayView> hint(Authentication auth,
                                          @RequestHeader("X-PrismaXI-Account-ID") long expected,
                                          @RequestBody(required = false) HintRequest body) {
        LocalDate date = body == null ? null : body.date();
        return noStore(service.revealHint(owner(auth, expected), date));
    }

    @GetMapping("/dictionary")
    public ResponseEntity<List<DictionaryEntry>> dictionary(Authentication auth,
                                                            @RequestHeader("X-PrismaXI-Account-ID") long expected) {
        owner(auth, expected);
        return noStore(service.dictionary());
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsView> stats(Authentication auth,
                                           @RequestHeader("X-PrismaXI-Account-ID") long expected) {
        return noStore(service.stats(owner(auth, expected)));
    }

    private long owner(Authentication auth, long expected) {
        long actual = accounts.current(auth.getName()).id();
        if (actual != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SESSION_CHANGED");
        }
        return actual;
    }

    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> error(ResponseStatusException failure) {
        String code = failure.getReason() == null ? "WORDLE_INPUT" : failure.getReason();
        return ResponseEntity.status(failure.getStatusCode())
                .header("Cache-Control", "no-store")
                .body(Map.of("code", code, "message", WordleService.message(code)));
    }

    @ExceptionHandler({
            org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class
    })
    ResponseEntity<Map<String, String>> inputError() {
        return ResponseEntity.badRequest()
                .header("Cache-Control", "no-store")
                .body(Map.of("code", "WORDLE_INPUT", "message", "Thiếu hoặc sai dữ liệu yêu cầu."));
    }
}
