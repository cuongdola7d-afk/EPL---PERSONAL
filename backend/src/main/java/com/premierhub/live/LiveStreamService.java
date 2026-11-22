package com.premierhub.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LiveStreamService {
    public static final String CHANNEL_MAIN = "main";
    public static final String CHANNEL_TEST = "test";

    private static final Logger LOG = LoggerFactory.getLogger(LiveStreamService.class);

    private final ConcurrentHashMap<String, LiveConnection> connections = new ConcurrentHashMap<>();
    private final Object registrationLock = new Object();
    private final AtomicLong idSequence = new AtomicLong();

    private final int maxConnections;
    private final long emitterTimeoutMs;
    private final boolean testStreamEnabled;
    private final long testStreamTimeoutMs;
    private final Clock clock;

    @Autowired
    public LiveStreamService(
            @Value("${premierhub.live.max-connections:200}") int maxConnections,
            @Value("${premierhub.live.emitter-timeout-ms:1800000}") long emitterTimeoutMs,
            @Value("${premierhub.live.test-stream.enabled:false}") boolean testStreamEnabled,
            @Value("${premierhub.live.test-stream.timeout-ms:3600000}") long testStreamTimeoutMs) {
        this(maxConnections, emitterTimeoutMs, testStreamEnabled, testStreamTimeoutMs, Clock.systemUTC());
    }

    public LiveStreamService(int maxConnections, long emitterTimeoutMs,
                             boolean testStreamEnabled, long testStreamTimeoutMs, Clock clock) {
        if (maxConnections <= 0) {
            throw new IllegalArgumentException("maxConnections must be positive");
        }
        this.maxConnections = maxConnections;
        this.emitterTimeoutMs = emitterTimeoutMs;
        this.testStreamEnabled = testStreamEnabled;
        this.testStreamTimeoutMs = testStreamTimeoutMs;
        this.clock = clock;
    }

    public boolean isTestStreamEnabled() {
        return testStreamEnabled;
    }

    public int maxConnections() {
        return maxConnections;
    }

    public int activeConnectionCount() {
        return connections.size();
    }

    public int activeConnectionCount(String channel) {
        int count = 0;
        for (LiveConnection conn : connections.values()) {
            if (conn.channel().equals(channel)) {
                count++;
            }
        }
        return count;
    }

    public SseEmitter subscribeMain() {
        return subscribe(CHANNEL_MAIN, new SseEmitter(emitterTimeoutMs));
    }

    public SseEmitter subscribeTest() {
        return subscribe(CHANNEL_TEST, new SseEmitter(testStreamTimeoutMs));
    }

    public SseEmitter subscribe(String channel, SseEmitter emitter) {
        String id = nextConnectionId();
        Instant now = Instant.now(clock);
        LiveConnection connection = new LiveConnection(id, channel, emitter, now);

        synchronized (registrationLock) {
            if (connections.size() >= maxConnections) {
                LOG.warn("SSE connection rejected (capacity reached): channel={} active={}/{}",
                        channel, connections.size(), maxConnections);
                throw new LiveStreamCapacityException(maxConnections);
            }
            connections.put(id, connection);
        }

        emitter.onCompletion(() -> removeConnection(id, "completed", false));
        emitter.onTimeout(() -> removeConnection(id, "timeout", true));
        emitter.onError(ex -> removeConnection(id, "error", false));

        LOG.info("SSE connection opened: id={} channel={} active={}/{}",
                id, channel, connections.size(), maxConnections);

        sendHello(connection);
        return emitter;
    }

    public void broadcast(String eventName, Object data) {
        broadcast(CHANNEL_MAIN, eventName, data);
    }

    public void broadcast(String channel, String eventName, Object data) {
        List<LiveConnection> snapshot = new ArrayList<>(connections.values());
        for (LiveConnection conn : snapshot) {
            if (!conn.channel().equals(channel)) {
                continue;
            }
            try {
                conn.emitter().send(SseEmitter.event()
                        .name(eventName)
                        .data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | RuntimeException ex) {
                removeConnection(conn.id(), "send_failed", false);
            }
        }
    }

    @Scheduled(fixedRateString = "${premierhub.live.heartbeat-interval-ms:25000}",
               initialDelayString = "${premierhub.live.heartbeat-interval-ms:25000}")
    public void sendHeartbeat() {
        if (connections.isEmpty()) {
            return;
        }
        List<LiveConnection> snapshot = new ArrayList<>(connections.values());
        for (LiveConnection conn : snapshot) {
            try {
                conn.emitter().send(SseEmitter.event().comment("ping"));
            } catch (IOException | RuntimeException ex) {
                removeConnection(conn.id(), "heartbeat_failed", false);
            }
        }
    }

    @Scheduled(fixedRateString = "${premierhub.live.test-stream.tick-interval-ms:10000}",
               initialDelayString = "${premierhub.live.test-stream.tick-interval-ms:10000}")
    public void sendTestStreamTicks() {
        if (!testStreamEnabled || connections.isEmpty()) {
            return;
        }
        Instant now = Instant.now(clock);
        String serverTime = now.toString();
        List<LiveConnection> snapshot = new ArrayList<>(connections.values());
        for (LiveConnection conn : snapshot) {
            if (!CHANNEL_TEST.equals(conn.channel())) {
                continue;
            }
            long elapsedMs = Duration.between(conn.connectedAt(), now).toMillis();
            if (testStreamTimeoutMs > 0 && elapsedMs >= testStreamTimeoutMs) {
                removeConnection(conn.id(), "max_duration", true);
                continue;
            }
            long seq = conn.nextSeq();
            TickEvent payload = new TickEvent(seq, serverTime, conn.id());
            try {
                conn.emitter().send(SseEmitter.event()
                        .id(String.valueOf(seq))
                        .name("tick")
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException | RuntimeException ex) {
                removeConnection(conn.id(), "send_failed", false);
            }
        }
    }

    private void sendHello(LiveConnection conn) {
        HelloEvent payload = new HelloEvent(conn.id(), conn.channel(), Instant.now(clock).toString());
        try {
            conn.emitter().send(SseEmitter.event()
                    .name("hello")
                    .data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException | RuntimeException ex) {
            removeConnection(conn.id(), "hello_failed", false);
        }
    }

    private void removeConnection(String id, String reason, boolean completeEmitter) {
        LiveConnection removed = connections.remove(id);
        if (removed == null || !removed.markClosed()) {
            return;
        }
        long durationMs = Math.max(0L, Duration.between(removed.connectedAt(), Instant.now(clock)).toMillis());
        LOG.info("SSE connection closed: id={} channel={} reason={} durationMs={} active={}/{}",
                id, removed.channel(), reason, durationMs, connections.size(), maxConnections);
        if (completeEmitter) {
            try {
                removed.emitter().complete();
            } catch (RuntimeException ignored) {
                // Client socket already closed
            }
        }
    }

    private String nextConnectionId() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "sse-" + idSequence.incrementAndGet() + "-" + suffix;
    }

    public record HelloEvent(String connectionId, String channel, String serverTime) {
    }

    public record TickEvent(long seq, String serverTime, String connectionId) {
    }

    private static final class LiveConnection {
        private final String id;
        private final String channel;
        private final SseEmitter emitter;
        private final Instant connectedAt;
        private final AtomicLong seq = new AtomicLong(0);
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private LiveConnection(String id, String channel, SseEmitter emitter, Instant connectedAt) {
            this.id = id;
            this.channel = channel;
            this.emitter = emitter;
            this.connectedAt = connectedAt;
        }

        String id() { return id; }
        String channel() { return channel; }
        SseEmitter emitter() { return emitter; }
        Instant connectedAt() { return connectedAt; }
        long nextSeq() { return seq.incrementAndGet(); }
        boolean markClosed() { return closed.compareAndSet(false, true); }
    }
}
