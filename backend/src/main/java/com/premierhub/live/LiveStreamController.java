package com.premierhub.live;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/live")
public class LiveStreamController {
    private static final MediaType UTF8_JSON = new MediaType("application", "json", StandardCharsets.UTF_8);

    private final LiveStreamService liveStreamService;
    private final LiveScoreService liveScoreService;

    public LiveStreamController(LiveStreamService liveStreamService) {
        this(liveStreamService, null);
    }

    @Autowired
    public LiveStreamController(LiveStreamService liveStreamService, LiveScoreService liveScoreService) {
        this.liveStreamService = liveStreamService;
        this.liveScoreService = liveScoreService;
    }

    @GetMapping("/matches")
    public ResponseEntity<List<LiveMatchState>> matches() {
        List<LiveMatchState> current = liveScoreService == null ? List.of() : liveScoreService.currentMatches();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(current);
    }

    @GetMapping("/stream")
    public ResponseEntity<SseEmitter> stream() {
        List<LiveMatchState> initialSnapshot = liveScoreService == null ? List.of() : liveScoreService.currentMatches();
        SseEmitter emitter = liveStreamService.subscribeMain(initialSnapshot);
        return ResponseEntity.ok()
                .headers(sseHeaders())
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }

    @GetMapping("/test-stream")
    public ResponseEntity<?> testStream() {
        if (!liveStreamService.isTestStreamEnabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentType(UTF8_JSON)
                    .body("{\"code\":\"RESOURCE_NOT_FOUND\",\"message\":\"Kênh thử nghiệm SSE đang tắt.\"}");
        }
        SseEmitter emitter = liveStreamService.subscribeTest();
        return ResponseEntity.ok()
                .headers(sseHeaders())
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }

    @ExceptionHandler(LiveStreamCapacityException.class)
    public ResponseEntity<String> handleCapacityExceeded(LiveStreamCapacityException ex, HttpServletRequest request) {
        String body = "{\"code\":\"LIVE_STREAM_CAPACITY_EXCEEDED\",\"message\":\""
                + escapeJson(ex.getMessage()) + "\"}";
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(UTF8_JSON)
                .body(body);
    }

    private static HttpHeaders sseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, no-transform");
        headers.set("X-Accel-Buffering", "no");
        headers.set(HttpHeaders.CONNECTION, "keep-alive");
        return headers;
    }

    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
