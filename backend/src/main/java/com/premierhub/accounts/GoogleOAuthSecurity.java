package com.premierhub.accounts;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GoogleOAuthSecurity {
    private final GoogleOAuthSettings settings;
    private final GoogleOAuthFlow flow;

    public GoogleOAuthSecurity(GoogleOAuthSettings settings, GoogleOAuthFlow flow) { this.settings = settings; this.flow = flow; }

    public void configure(HttpSecurity http) throws Exception {
        if (!settings.enabled()) return; // No registration/network discovery needed for email login.
        var registrations = new InMemoryClientRegistrationRepository(registration());
        var delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, GoogleOAuthSettings.AUTHORIZE_BASE);
        delegate.setAuthorizationRequestCustomizer(builder -> {
            OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);
            builder.additionalParameters(parameters -> parameters.put("prompt", "select_account"));
        });
        var resolver = new OAuth2AuthorizationRequestResolver() {
            @Override public OAuth2AuthorizationRequest resolve(HttpServletRequest request) { return bind(request, delegate.resolve(request)); }
            @Override public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String id) { return bind(request, delegate.resolve(request, id)); }
            private OAuth2AuthorizationRequest bind(HttpServletRequest request, OAuth2AuthorizationRequest authorization) {
                if (authorization == null) return null;
                var attempt = flow.attempt(request);
                if (attempt == null || !GoogleOAuthFlow.fresh(attempt.createdAt()) || attempt.state() != null
                        || !flow.matchesAccount(attempt, SecurityContextHolder.getContext().getAuthentication())) {
                    throw new OAuth2AuthenticationException(new OAuth2Error("invalid_flow"));
                }
                request.getSession(false).setAttribute(GoogleOAuthFlow.FLOW, attempt.withState(authorization.getState()));
                return authorization; // Spring generates/stores state, nonce and PKCE. Do not replace any of them.
            }
        };
        http.oauth2Login(oauth -> oauth.clientRegistrationRepository(registrations)
                .authorizedClientRepository(new LoginOnlyClients())
                .loginPage("/api/auth/google/status")
                .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver))
                .redirectionEndpoint(endpoint -> endpoint.baseUri(GoogleOAuthSettings.CALLBACK))
                .successHandler(flow).failureHandler(flow)
                .withObjectPostProcessor(new ObjectPostProcessor<OAuth2AuthorizationRequestRedirectFilter>() {
                    @Override public <O extends OAuth2AuthorizationRequestRedirectFilter> O postProcess(O filter) {
                        filter.setAuthenticationFailureHandler(flow);
                        return filter;
                    }
                }));
        http.addFilterBefore(new OncePerRequestFilter() {
            @Override protected boolean shouldNotFilter(HttpServletRequest request) {
                return !GoogleOAuthSettings.CALLBACK.equals(request.getServletPath());
            }
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
                var previous = SecurityContextHolder.getContext().getAuthentication();
                request.setAttribute(GoogleOAuthFlow.PREVIOUS_AUTH, previous);
                var attempt = flow.attempt(request);
                if (attempt == null || !GoogleOAuthFlow.fresh(attempt.createdAt()) || attempt.state() == null
                        || !attempt.state().equals(request.getParameter("state")) || !flow.matchesAccount(attempt, previous)) {
                    flow.fail(request, response, previous, "invalid_flow");
                    return;
                }
                chain.doFilter(request, response);
            }
        }, OAuth2LoginAuthenticationFilter.class);
    }

    ClientRegistration registration() {
        return CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(settings.clientId()).clientSecret(settings.clientSecret())
                .scope("openid", "email", "profile").redirectUri(settings.callbackUri()).build();
    }

    /** Sign-in only: do not retain provider access/refresh tokens after authentication. */
    private static class LoginOnlyClients implements OAuth2AuthorizedClientRepository {
        @Override public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String id, Authentication principal, HttpServletRequest request) { return null; }
        @Override public void saveAuthorizedClient(OAuth2AuthorizedClient client, Authentication principal, HttpServletRequest request, HttpServletResponse response) { }
        @Override public void removeAuthorizedClient(String id, Authentication principal, HttpServletRequest request, HttpServletResponse response) { }
    }
}
