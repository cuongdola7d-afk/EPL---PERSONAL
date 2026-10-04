package com.premierhub.accounts;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService accounts;
    private final AuthenticationConfiguration authentication;
    private final SecurityContextRepository contexts;
    private final SessionAuthenticationStrategy sessions;
    private final AuthRateLimiter limits;
    private final AuthClientIpResolver clientIps;

    public AuthController(AccountService accounts, AuthenticationConfiguration authentication,
                          SecurityContextRepository contexts, SessionAuthenticationStrategy sessions,
                          AuthRateLimiter limits, AuthClientIpResolver clientIps) {
        this.accounts = accounts;
        this.authentication = authentication;
        this.contexts = contexts;
        this.sessions = sessions;
        this.limits = limits;
        this.clientIps = clientIps;
    }

    public record Registration(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(min = 2, max = 80) @Pattern(regexp = "[^\\p{Cntrl}]+") String displayName,
                               @NotBlank @Size(min = 8, max = 72) String password) {
        public Registration { email = AccountService.normalizeEmail(email); displayName = displayName == null ? null : displayName.strip(); }
        @Override public String toString() { return "Registration[credentials redacted]"; }
    }
    public record Login(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {
        public Login { email = AccountService.normalizeEmail(email); }
        @Override public String toString() { return "Login[credentials redacted]"; }
    }
    public record CsrfResponse(String token, String headerName) { }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) { return new CsrfResponse(token.getToken(), token.getHeaderName()); }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody Registration body, HttpServletRequest request) {
        limits.register(clientIps.resolve(request));
        return accounts.register(body.email(), body.displayName(), body.password());
    }

    @PostMapping("/login")
    public AccountResponse login(@Valid @RequestBody Login body, HttpServletRequest request, HttpServletResponse response) throws Exception {
        limits.login(clientIps.resolve(request), body.email());
        AccountService.validatePasswordBytes(body.password());
        var result = authentication.getAuthenticationManager().authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.email(), body.password()));
        sessions.onAuthentication(result, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return accounts.current(result.getName());
    }

    @GetMapping("/me")
    public AccountResponse me(Authentication authentication) { return accounts.current(authentication.getName()); }
    // POST /logout is handled by Spring Security, which invalidates the session and CSRF token.
}
