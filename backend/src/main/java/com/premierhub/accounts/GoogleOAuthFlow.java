package com.premierhub.accounts;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

/** One session-bound OAuth attempt. Linking requires a separate, CSRF-protected confirmation. */
@Component
public class GoogleOAuthFlow implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GoogleOAuthFlow.class);
    static final String FLOW = GoogleOAuthFlow.class.getName() + ".flow";
    static final String PENDING = GoogleOAuthFlow.class.getName() + ".pending";
    static final String PREVIOUS_AUTH = GoogleOAuthFlow.class.getName() + ".previousAuthentication";
    private final GoogleOAuthSettings settings;
    private final GoogleAccountService accounts;
    private final SecurityContextRepository contexts;

    public enum Mode { LOGIN, LINK }
    record Attempt(Mode mode, String returnPath, Long accountId, Instant createdAt, String state) implements Serializable {
        Attempt withState(String value) { return new Attempt(mode, returnPath, accountId, createdAt, value); }
    }
    record Pending(long accountId, GoogleAccountService.Profile profile, Instant createdAt) implements Serializable { }

    public GoogleOAuthFlow(GoogleOAuthSettings settings, GoogleAccountService accounts, SecurityContextRepository contexts) {
        this.settings = settings;
        this.accounts = accounts;
        this.contexts = contexts;
    }

    static boolean signedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    void start(HttpServletRequest request, Authentication auth, Mode mode, String returnPath) {
        if (!settings.enabled()) throw new GoogleAccountException("unavailable");
        String safePath = GoogleOAuthSettings.safeReturnPath(returnPath);
        if (mode == Mode.LINK && !signedIn(auth)) throw new GoogleAccountException("login_required");
        if (mode == Mode.LOGIN && signedIn(auth)) throw new GoogleAccountException("already_signed_in");
        Long accountId = mode == Mode.LINK ? accounts.current(auth.getName()).id() : null;
        var session = request.getSession(true);
        session.removeAttribute(PENDING);
        session.setAttribute(FLOW, new Attempt(mode, safePath, accountId, Instant.now(), null));
    }

    Attempt attempt(HttpServletRequest request) {
        var session = request.getSession(false);
        return session == null ? null : (Attempt) session.getAttribute(FLOW);
    }

    boolean matchesAccount(Attempt attempt, Authentication auth) {
        if (attempt.mode() == Mode.LOGIN) return !signedIn(auth);
        return signedIn(auth) && accounts.current(auth.getName()).id() == attempt.accountId();
    }

    static boolean fresh(Instant createdAt) {
        return !createdAt.isAfter(Instant.now()) && Duration.between(createdAt, Instant.now()).compareTo(Duration.ofMinutes(10)) < 0;
    }

    Pending pending(HttpServletRequest request, Authentication auth) {
        var session = request.getSession(false);
        if (session == null || !signedIn(auth)) return null;
        var pending = (Pending) session.getAttribute(PENDING);
        return pending != null && fresh(pending.createdAt()) && accounts.current(auth.getName()).id() == pending.accountId() ? pending : null;
    }

    void confirm(HttpServletRequest request, Authentication auth, boolean confirmed) {
        if (!confirmed) throw new AccountInputException("Bạn cần xác nhận liên kết Google.");
        var pending = pending(request, auth);
        if (pending == null) throw new GoogleAccountException("session_changed");
        request.getSession(false).removeAttribute(PENDING);
        accounts.link(pending.accountId(), pending.profile());
    }

    void cancel(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) session.removeAttribute(PENDING);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication auth) throws IOException {
        var attempt = attempt(request);
        var previous = (Authentication) request.getAttribute(PREVIOUS_AUTH);
        try {
            if (attempt == null || !fresh(attempt.createdAt())) throw new GoogleAccountException("invalid_flow");
            if (!(auth instanceof OAuth2AuthenticationToken oauth) || !"google".equals(oauth.getAuthorizedClientRegistrationId())
                    || !(oauth.getPrincipal() instanceof OidcUser user)) throw new GoogleAccountException("invalid_identity");
            var profile = accounts.verifiedProfile(user);
            String result;
            if (attempt.mode() == Mode.LINK) {
                if (!matchesAccount(attempt, previous)) throw new GoogleAccountException("session_changed");
                request.getSession(false).setAttribute(PENDING, new Pending(attempt.accountId(), profile, Instant.now()));
                save(previous, request, response);
                result = "confirm_link";
            } else {
                if (signedIn(previous)) throw new GoogleAccountException("session_changed");
                var account = accounts.login(profile);
                var principal = User.withUsername(account.email()).password("").roles(account.role()).build();
                save(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()), request, response);
                result = "success";
            }
            clearAttempt(request);
            response.sendRedirect(settings.redirect(attempt.returnPath(), result));
        } catch (GoogleAccountException failure) {
            fail(request, response, previous, failure.code());
        } catch (RuntimeException failure) {
            // Never leave Spring's temporary OIDC principal authenticated when local persistence fails.
            // Record the failure type without provider tokens, request bodies or SQL parameter values.
            LOG.error("Google account completion failed ({})", failure.getClass().getSimpleName());
            fail(request, response, previous, "provider_error");
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException failure) throws IOException {
        String code = failure instanceof OAuth2AuthenticationException oauth ? oauth.getError().getErrorCode() : "provider_error";
        String result = "access_denied".equals(code) ? "cancelled" : "invalid_flow".equals(code) ? "invalid_flow" : "provider_error";
        // Never redirect with the provider's error_description, tokens, email or subject.
        var previous = (Authentication) request.getAttribute(PREVIOUS_AUTH);
        if (previous == null && signedIn(SecurityContextHolder.getContext().getAuthentication())) {
            previous = SecurityContextHolder.getContext().getAuthentication();
        }
        fail(request, response, previous, result);
    }

    void fail(HttpServletRequest request, HttpServletResponse response, Authentication previous, String result) throws IOException {
        var attempt = attempt(request);
        String path = attempt == null ? "/" : attempt.returnPath();
        clearAttempt(request);
        // A logout/expired session must never be resurrected by an in-flight OAuth callback.
        if (request.getSession(false) == null) previous = null;
        save(previous, request, response);
        response.sendRedirect(settings.redirect(path, result));
    }

    private void save(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(signedIn(auth) ? auth : null);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
    }

    private void clearAttempt(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) session.removeAttribute(FLOW);
    }
}
