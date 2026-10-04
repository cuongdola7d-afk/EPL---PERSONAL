package com.premierhub.accounts;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;

/** Secrets stay in server configuration, never in a response or a record/toString. */
@Component
public class GoogleOAuthSettings {
    public static final String AUTHORIZE_BASE = "/api/auth/google/authorize";
    public static final String CALLBACK = "/api/auth/google/callback";
    private final String clientId;
    private final String clientSecret;
    private final String frontendOrigin;
    private final String callbackUri;

    public GoogleOAuthSettings(Environment environment) {
        clientId = environment.getProperty("PREMIERHUB_GOOGLE_CLIENT_ID", "");
        clientSecret = environment.getProperty("PREMIERHUB_GOOGLE_CLIENT_SECRET", "");
        frontendOrigin = environment.getProperty("PREMIERHUB_GOOGLE_FRONTEND_ORIGIN", "");
        callbackUri = environment.getProperty("PREMIERHUB_GOOGLE_CALLBACK_URI", "");
        if (enabled()) {
            validateUrl(frontendOrigin, true);
            validateUrl(callbackUri, false);
        }
    }

    public boolean enabled() { return !clientId.isBlank() && !clientSecret.isBlank(); }
    String clientId() { return clientId; }
    String clientSecret() { return clientSecret; }
    String callbackUri() { return callbackUri; }

    public String redirect(String returnPath, String result) {
        // The origin is pinned configuration; the browser supplies only the SPA's internal route.
        URI route = URI.create(safeReturnPath(returnPath));
        String query = route.getRawQuery();
        return frontendOrigin + "/?" + (query == null ? "" : query + "&") + "google=" + result
                + (route.getRawFragment() == null ? "" : "#" + route.getRawFragment());
    }

    public static String safeReturnPath(String value) {
        try {
            if (value == null || value.length() > 2048 || value.contains("\\")
                    || value.chars().anyMatch(c -> c < 32 || c == 127)) throw new IllegalArgumentException();
            URI uri = URI.create(value);
            if (uri.isAbsolute() || uri.getRawAuthority() != null || !"/".equals(uri.getRawPath())) {
                throw new IllegalArgumentException();
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException invalid) {
            throw new AccountInputException("Đường dẫn quay lại không hợp lệ.");
        }
    }

    private static void validateUrl(String value, boolean origin) {
        try {
            URI uri = URI.create(value);
            boolean local = "localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || !("https".equals(uri.getScheme()) || (local && "http".equals(uri.getScheme())))
                    || (origin ? !uri.getRawPath().isEmpty() : !CALLBACK.equals(uri.getRawPath()))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException invalid) {
            // Do not include supplied configuration values (in particular secrets) in startup errors.
            throw new IllegalArgumentException("Configure a valid Google frontend origin and exact /api/auth/google/callback URI");
        }
    }
}
