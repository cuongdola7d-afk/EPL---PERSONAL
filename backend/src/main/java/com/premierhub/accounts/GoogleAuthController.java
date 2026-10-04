package com.premierhub.accounts;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {
    private final GoogleOAuthSettings settings;
    private final GoogleOAuthFlow flow;
    private final GoogleAccountService accounts;
    private final AuthRateLimiter limits;
    private final AuthClientIpResolver clientIps;

    public GoogleAuthController(GoogleOAuthSettings settings, GoogleOAuthFlow flow, GoogleAccountService accounts,
                                AuthRateLimiter limits, AuthClientIpResolver clientIps) {
        this.settings = settings; this.flow = flow; this.accounts = accounts;
        this.limits = limits; this.clientIps = clientIps;
    }

    public record Status(boolean enabled, boolean linked, String pendingEmail) { }
    public record Start(@NotNull GoogleOAuthFlow.Mode mode, @NotNull @Size(max = 2048) String returnPath) { }
    public record Started(String authorizationPath) { }
    public record Confirmation(boolean confirmed) { }

    @GetMapping("/status")
    public Status status(HttpServletRequest request, Authentication auth) {
        boolean linked = GoogleOAuthFlow.signedIn(auth) && accounts.linked(accounts.current(auth.getName()).id());
        var pending = flow.pending(request, auth);
        return new Status(settings.enabled(), linked, pending == null ? null : pending.profile().email());
    }

    @PostMapping("/start")
    public Started start(@Valid @RequestBody Start body, HttpServletRequest request, Authentication auth) {
        limits.google(clientIps.resolve(request));
        flow.start(request, auth, body.mode(), body.returnPath());
        return new Started(GoogleOAuthSettings.AUTHORIZE_BASE + "/google");
    }

    @PostMapping("/link/confirm")
    public Status confirm(@RequestBody Confirmation body, HttpServletRequest request, Authentication auth) {
        flow.confirm(request, auth, body.confirmed());
        return status(request, auth);
    }

    @PostMapping("/link/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(HttpServletRequest request) { flow.cancel(request); }
}
