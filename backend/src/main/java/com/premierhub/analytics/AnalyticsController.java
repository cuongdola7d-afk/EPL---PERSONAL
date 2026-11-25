package com.premierhub.analytics;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    public record PageviewPayload(String visitorId, String page) {
    }

    private final AnalyticsPageviewService pageviews;
    private final ObjectMapper objectMapper;

    public AnalyticsController(AnalyticsPageviewService pageviews, ObjectMapper objectMapper) {
        this.pageviews = pageviews;
        this.objectMapper = objectMapper;
    }

    @PostMapping(
            value = "/pageview",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<?> pageview(
            @RequestBody(required = false) String rawBody,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            Authentication authentication) {
        if (rawBody == null || rawBody.isBlank() || rawBody.length() > 512) {
            return badRequest();
        }
        PageviewPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, PageviewPayload.class);
        } catch (Exception invalidJson) {
            return badRequest();
        }
        if (payload == null) {
            return badRequest();
        }

        boolean isAdmin = authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        AnalyticsPageviewService.Status status = pageviews.record(
                payload.visitorId(), payload.page(), userAgent, isAdmin);
        return switch (status) {
            case INVALID_INPUT -> badRequest();
            case SALT_MISSING -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(Map.of("code", "ANALYTICS_NOT_CONFIGURED", "message", "Analytics chưa được cấu hình salt cho production."));
            case RATE_LIMITED -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(Map.of("code", "ANALYTICS_RATE_LIMITED", "message", "Quá nhiều lượt gửi trong một giờ."));
            case RECORDED, IGNORED_BOT, IGNORED_ADMIN -> ResponseEntity.noContent()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build();
        };
    }

    private static ResponseEntity<?> badRequest() {
        return ResponseEntity.badRequest()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of("code", "INVALID_PAGEVIEW", "message", "Dữ liệu pageview không hợp lệ."));
    }
}
