package com.premierhub.accounts;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** The public auth origin is configuration, never an incoming Host/Forwarded header. */
@Component
public class AuthProxySettings {
    private final boolean enabled;
    private final URI origin;
    private final byte[] secret;

    public AuthProxySettings(Environment environment) {
        enabled = environment.getProperty("premierhub.auth-proxy.enabled", Boolean.class, false);
        if (!enabled) { origin = null; secret = new byte[0]; return; }
        String configuredSecret = environment.getProperty("PREMIERHUB_AUTH_PROXY_SECRET", "");
        if (!configuredSecret.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("Configure a 32-byte hexadecimal PREMIERHUB_AUTH_PROXY_SECRET.");
        }
        secret = configuredSecret.getBytes(StandardCharsets.US_ASCII);
        try {
            origin = URI.create(environment.getProperty("PREMIERHUB_AUTH_PUBLIC_ORIGIN", ""));
            if (!"https".equals(origin.getScheme()) || origin.getHost() == null || origin.getUserInfo() != null
                    || !origin.getRawPath().isEmpty() || origin.getRawQuery() != null || origin.getRawFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("Configure an exact HTTPS PREMIERHUB_AUTH_PUBLIC_ORIGIN.");
        }
        if (!environment.getProperty("server.servlet.session.cookie.secure", Boolean.class, false)
                || !environment.getProperty("server.servlet.session.cookie.http-only", Boolean.class, true)
                || !"lax".equalsIgnoreCase(environment.getProperty("server.servlet.session.cookie.same-site", "lax"))
                || !"/".equals(environment.getProperty("server.servlet.session.cookie.path", "/"))
                || !environment.getProperty("server.servlet.session.cookie.domain", "").isBlank()
                || !"none".equalsIgnoreCase(environment.getProperty("server.forward-headers-strategy", "none"))) {
            throw new IllegalArgumentException("Proxied auth requires Secure/HttpOnly, host-only Path=/, SameSite=Lax and no global forwarding.");
        }
        if (!environment.getProperty("PREMIERHUB_GOOGLE_CLIENT_ID", "").isBlank()
                && !environment.getProperty("PREMIERHUB_GOOGLE_CLIENT_SECRET", "").isBlank()
                && (!origin.toString().equals(environment.getProperty("PREMIERHUB_GOOGLE_FRONTEND_ORIGIN", ""))
                || !(origin + GoogleOAuthSettings.CALLBACK).equals(environment.getProperty("PREMIERHUB_GOOGLE_CALLBACK_URI", "")))) {
            throw new IllegalArgumentException("Proxied Google frontend and callback must use the configured public auth origin.");
        }
    }

    public boolean enabled() { return enabled; }
    URI origin() { return origin; }

    boolean accepts(String supplied) {
        return supplied != null && supplied.length() == 64
                && MessageDigest.isEqual(secret, supplied.getBytes(StandardCharsets.US_ASCII));
    }
}
