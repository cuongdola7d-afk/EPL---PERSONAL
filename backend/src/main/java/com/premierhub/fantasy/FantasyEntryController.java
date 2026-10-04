package com.premierhub.fantasy;

import com.premierhub.accounts.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/fantasy/2026/me/gameweeks/{gameweek}")
public class FantasyEntryController {
    public record Change(@NotBlank @Size(max=10) String formation,
                         @NotNull @Size(max=11) Map<@Pattern(regexp="[0-9]-[0-9]") String,@NotNull @Positive Integer> picks,
                         @NotNull @PositiveOrZero Long expectedVersion) { }
    private final FantasyEntryService service;
    private final AccountService accounts;
    public FantasyEntryController(FantasyEntryService service,AccountService accounts) { this.service=service; this.accounts=accounts; }
    @GetMapping
    public ResponseEntity<FantasyEntryService.Mine> read(@PathVariable int gameweek,Authentication auth,
                                                        @RequestHeader("X-PrismaXI-Account-ID") long expectedAccountId) {
        return noStore(service.read(owner(auth,expectedAccountId),gameweek));
    }
    @PostMapping("/draft")
    public ResponseEntity<FantasyEntryService.Mine> draft(@PathVariable int gameweek,@Valid @RequestBody Change body,
            Authentication auth,@RequestHeader("X-PrismaXI-Account-ID") long expectedAccountId) {
        return noStore(service.save(owner(auth,expectedAccountId),gameweek,body.formation(),body.picks(),body.expectedVersion(),false));
    }
    @PostMapping("/submit")
    public ResponseEntity<FantasyEntryService.Mine> submit(@PathVariable int gameweek,@Valid @RequestBody Change body,
            Authentication auth,@RequestHeader("X-PrismaXI-Account-ID") long expectedAccountId) {
        return noStore(service.save(owner(auth,expectedAccountId),gameweek,body.formation(),body.picks(),body.expectedVersion(),true));
    }
    // This header detects a tab with an old session; it never chooses the owner.
    private long owner(Authentication auth,long expectedId) {
        long actual=accounts.current(auth.getName()).id();
        if (actual!=expectedId) throw new ResponseStatusException(HttpStatus.CONFLICT,"SESSION_CHANGED");
        return actual;
    }
    @ExceptionHandler(FantasyEntryService.InvalidLineup.class)
    ResponseEntity<?> invalid(FantasyEntryService.InvalidLineup failure) {
        return ResponseEntity.status(422).header("Cache-Control","no-store").body(Map.of(
                "code","LINEUP_INVALID","message","Đội hình chưa hợp lệ.","issues",failure.validation().issues()));
    }
    @ExceptionHandler({org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    ResponseEntity<?> badRequest() {
        return ResponseEntity.badRequest().header("Cache-Control","no-store")
                .body(Map.of("code","FANTASY_ENTRY_INPUT","message","Thiếu thông tin phiên hoặc dữ liệu đội hình không hợp lệ."));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> error(ResponseStatusException failure) {
        boolean sessionChanged="SESSION_CHANGED".equals(failure.getReason());
        return ResponseEntity.status(failure.getStatusCode()).header("Cache-Control","no-store").body(Map.of(
                "code",sessionChanged ? "SESSION_CHANGED" : "FANTASY_ENTRY_REQUEST",
                "message",sessionChanged ? "Phiên đã đổi tài khoản. Tải lại tài khoản trước khi tiếp tục." : failure.getReason()));
    }
    private static <T> ResponseEntity<T> noStore(T data) { return ResponseEntity.ok().header("Cache-Control","no-store").body(data); }
}
