package com.premierhub.config;

import com.premierhub.accounts.GoogleOAuthSecurity;
import com.premierhub.accounts.AuthProxyFilter;
import com.premierhub.accounts.AuthProxySettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import java.util.List;

@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }

    @Bean HttpSessionCsrfTokenRepository csrfTokens() { return new HttpSessionCsrfTokenRepository(); }

    @Bean SecurityContextRepository securityContexts() {
        return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(), new HttpSessionSecurityContextRepository());
    }

    @Bean SessionAuthenticationStrategy sessionAuthentication(HttpSessionCsrfTokenRepository tokens) {
        return new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(tokens)));
    }

    @Bean SecurityFilterChain apiSecurity(HttpSecurity http, HttpSessionCsrfTokenRepository tokens, SecurityContextRepository contexts,
                                       SessionAuthenticationStrategy sessions, ObjectProvider<GoogleOAuthSecurity> google,
                                       ObjectProvider<AuthProxySettings> proxy) throws Exception {
        var proxySettings = proxy.getIfAvailable();
        if (proxySettings != null) http.addFilterBefore(new AuthProxyFilter(proxySettings), SecurityContextHolderFilter.class);
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.csrfTokenRepository(tokens)
                        // This existing endpoint only reads football data; it does not save a user's team.
                        .ignoringRequestMatchers("/api/fantasy/2026/validate"))
                .securityContext(context -> context.securityContextRepository(contexts))
                .sessionManagement(session -> session.sessionAuthenticationStrategy(sessions))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/csrf", "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/google/status", "/api/auth/google/start", "/api/auth/google/authorize/google", "/api/auth/google/callback").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/api/fantasy/2026/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/fantasy/2026/gameweeks", "/api/fantasy/2026/gameweeks/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clubs/**", "/api/players/**", "/api/matches/**", "/api/standings/**", "/api/fantasy/2026/team-of-week", "/api/fantasy/2024/team-of-week", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/fantasy/2026/validate", "/api/fantasy/2024/validate").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, failure) -> jsonError(response, 401, "AUTH_REQUIRED", "Bạn cần đăng nhập."))
                        .accessDeniedHandler((request, response, failure) -> jsonError(response, 403, "ACCESS_DENIED", "Phiên hoặc mã CSRF không hợp lệ. Vui lòng thử lại.")))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true).clearAuthentication(true)
                        .logoutSuccessHandler((request, response, auth) -> response.setStatus(204)));
        var googleSecurity = google.getIfAvailable();
        if (googleSecurity != null) googleSecurity.configure(http);
        return http.build();
    }

    private static void jsonError(jakarta.servlet.http.HttpServletResponse response, int status, String code, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
