package com.premierhub.fantasy;

import com.premierhub.accounts.AccountService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDate;
import com.premierhub.service.FootballQueries;
import com.premierhub.web.dto.PlayerResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fantasy/2026")
public class GameweekController {
    private final GameweekService service;
    private final AccountService accounts;
    private final FootballQueries queries;
    public GameweekController(GameweekService service, AccountService accounts, FootballQueries queries) {
        this.service = service; this.accounts = accounts; this.queries = queries;
    }
    public record Publish(String reason, LocalDate rosterAsOf) { }
    public record Roster(int season, int gameweek, LocalDate rosterAsOf, List<PlayerResponse> players) { }
    public record Adjustment(Instant deadlineUtc, int expectedRevision, String reason) { }

    @GetMapping("/gameweeks")
    public ResponseEntity<GameweekService.Overview> overview() { return noStore(service.overview()); }

    @GetMapping("/gameweeks/{gameweek}")
    public ResponseEntity<GameweekService.Overview> info(@PathVariable int gameweek) {
        // Include the same server timestamp as the status decision, not a second clock read.
        if (gameweek < 1 || gameweek > 38) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "GW phải từ 1 đến 38.");
        var overview = service.overview();
        return noStore(new GameweekService.Overview(overview.serverTimeUtc(), overview.recommendedGameweek(),
                java.util.List.of(overview.gameweeks().get(gameweek - 1))));
    }

    @PostMapping("/admin/gameweeks/{gameweek}/publish-deadline")
    public ResponseEntity<GameweekService.View> publish(@PathVariable int gameweek, @RequestBody Publish body, Authentication authentication) {
        return noStore(service.publishDeadline(gameweek, accounts.current(authentication.getName()).id(), body.reason(), body.rosterAsOf()));
    }

    @GetMapping("/gameweeks/{gameweek}/players")
    public ResponseEntity<Roster> roster(@PathVariable int gameweek) {
        LocalDate asOf = service.rosterAsOf(gameweek);
        return noStore(new Roster(2026, gameweek, asOf, queries.players(2026, null, null, asOf)));
    }

    @PostMapping("/admin/gameweeks/{gameweek}/adjust-deadline")
    public ResponseEntity<GameweekService.View> adjust(@PathVariable int gameweek, @RequestBody Adjustment body, Authentication authentication) {
        return noStore(service.adjustDeadline(gameweek, body.deadlineUtc(), body.expectedRevision(),
                accounts.current(authentication.getName()).id(), body.reason()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> error(ResponseStatusException failure) {
        return ResponseEntity.status(failure.getStatusCode()).header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of("code", "GAMEWEEK_REQUEST", "message", failure.getReason() == null ? "Yêu cầu GW không hợp lệ." : failure.getReason()));
    }

    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(body);
    }
}
