package com.premierhub.fantasy;

import com.premierhub.accounts.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController
@RequestMapping("/api/fantasy/2026")
public class FantasyResultController {
    public record PublicationRequest(@NotNull @PositiveOrZero Integer expectedVersion,@NotBlank @Size(min=3,max=500) String reason) {}
    private final FantasyResultService results;
    private final AccountService accounts;
    public FantasyResultController(FantasyResultService results,AccountService accounts) { this.results=results;this.accounts=accounts; }
    @GetMapping("/admin/gameweeks/{gameweek}/readiness")
    public ResponseEntity<?> readiness(@PathVariable int gameweek) { return noStore(results.readiness(gameweek)); }
    @PostMapping("/admin/gameweeks/{gameweek}/publish-results")
    public ResponseEntity<?> publish(@PathVariable int gameweek,@Valid @RequestBody PublicationRequest body,Authentication auth) {
        return noStore(results.publish(gameweek,body.expectedVersion(),accounts.current(auth.getName()).id(),body.reason(),false));
    }
    @PostMapping("/admin/gameweeks/{gameweek}/recalculate-results")
    public ResponseEntity<?> recalculate(@PathVariable int gameweek,@Valid @RequestBody PublicationRequest body,Authentication auth) {
        return noStore(results.publish(gameweek,body.expectedVersion(),accounts.current(auth.getName()).id(),body.reason(),true));
    }
    @GetMapping("/me/gameweeks/{gameweek}/result")
    public ResponseEntity<?> mine(@PathVariable int gameweek,Authentication auth,@RequestHeader("X-PrismaXI-Account-ID") long expectedId) {
        long owner=accounts.current(auth.getName()).id();
        if(owner!=expectedId) throw new ResponseStatusException(HttpStatus.CONFLICT,"SESSION_CHANGED");
        return noStore(results.mine(owner,gameweek));
    }
    @ExceptionHandler(FantasyResultService.NotReady.class)
    ResponseEntity<?> notReady(FantasyResultService.NotReady failure) {
        return ResponseEntity.status(409).header("Cache-Control","no-store").body(Map.of("code","RESULTS_NOT_READY",
                "message","GW còn thiếu dữ liệu, chưa thể công bố.","readiness",failure.readiness()));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> error(ResponseStatusException failure) {
        return ResponseEntity.status(failure.getStatusCode()).header("Cache-Control","no-store")
                .body(Map.of("code","SESSION_CHANGED".equals(failure.getReason())?"SESSION_CHANGED":"FANTASY_RESULTS_REQUEST",
                        "message",failure.getReason()==null?"Yêu cầu kết quả không hợp lệ.":failure.getReason()));
    }
    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    ResponseEntity<?> missingSession() {
        return ResponseEntity.badRequest().header("Cache-Control","no-store").body(Map.of("code","FANTASY_RESULTS_SESSION","message","Thiếu thông tin phiên tài khoản."));
    }
    private static <T> ResponseEntity<T> noStore(T value) { return ResponseEntity.ok().header("Cache-Control","no-store").body(value); }
}
