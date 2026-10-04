package com.premierhub.accounts;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ConfigurationProperties("premierhub.auth-rate-limit")
public class AuthRateLimitSettings {
    public record Policy(int attempts, Duration window) {
        public Policy {
            if (attempts < 1 || window == null || window.compareTo(Duration.ofSeconds(1)) < 0
                    || window.compareTo(Duration.ofDays(7)) > 0) {
                throw new IllegalArgumentException("Auth limits require positive attempts and a window from 1 second to 7 days.");
            }
        }
    }

    private Policy loginIp = new Policy(20, Duration.ofMinutes(5));
    private Policy loginEmail = new Policy(5, Duration.ofMinutes(5));
    private Policy registrationIp = new Policy(5, Duration.ofMinutes(15));
    private Policy googleIp = new Policy(10, Duration.ofMinutes(5));
    private int maxEntries = 10_000;
    private Duration cleanupInterval = Duration.ofSeconds(60);
    private List<String> trustedProxies = List.of();

    public Policy getLoginIp() { return loginIp; }
    public void setLoginIp(Policy value) { loginIp = value; }
    public Policy getLoginEmail() { return loginEmail; }
    public void setLoginEmail(Policy value) { loginEmail = value; }
    public Policy getRegistrationIp() { return registrationIp; }
    public void setRegistrationIp(Policy value) { registrationIp = value; }
    public Policy getGoogleIp() { return googleIp; }
    public void setGoogleIp(Policy value) { googleIp = value; }
    public int getMaxEntries() { return maxEntries; }
    public void setMaxEntries(int value) { maxEntries = value; }
    public Duration getCleanupInterval() { return cleanupInterval; }
    public void setCleanupInterval(Duration value) { cleanupInterval = value; }
    public List<String> getTrustedProxies() { return trustedProxies; }
    public void setTrustedProxies(List<String> value) { trustedProxies = List.copyOf(value); }
}
