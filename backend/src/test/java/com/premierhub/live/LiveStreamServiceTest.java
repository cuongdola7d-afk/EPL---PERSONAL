package com.premierhub.live;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveStreamServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-22T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Test
    void subscribeSendsImmediateHelloAndRemovesOnCompletionTimeoutAndError() {
        LiveStreamService service = new LiveStreamService(5, 1800_000L, true, 3600_000L, FIXED_CLOCK);

        RecordingEmitter e1 = new RecordingEmitter();
        RecordingEmitter e2 = new RecordingEmitter();
        RecordingEmitter e3 = new RecordingEmitter();

        service.subscribe(LiveStreamService.CHANNEL_MAIN, e1);
        service.subscribe(LiveStreamService.CHANNEL_MAIN, e2);
        service.subscribe(LiveStreamService.CHANNEL_TEST, e3);

        assertEquals(3, service.activeConnectionCount());
        assertEquals(2, service.activeConnectionCount(LiveStreamService.CHANNEL_MAIN));
        assertEquals(1, service.activeConnectionCount(LiveStreamService.CHANNEL_TEST));
        assertEquals(1, e1.sentCount());
        assertEquals(1, e2.sentCount());
        assertEquals(1, e3.sentCount());

        e1.triggerCompletion();
        assertEquals(2, service.activeConnectionCount());

        e2.triggerTimeout();
        assertEquals(1, service.activeConnectionCount());
        assertTrue(e2.completed());

        e3.triggerError(new IOException("client closed"));
        assertEquals(0, service.activeConnectionCount());
    }

    @Test
    void enforcesMaxConnectionsAndAllowsNewConnectionsAfterDisconnect() {
        LiveStreamService service = new LiveStreamService(2, 1800_000L, false, 3600_000L, FIXED_CLOCK);

        RecordingEmitter e1 = new RecordingEmitter();
        RecordingEmitter e2 = new RecordingEmitter();
        service.subscribe(LiveStreamService.CHANNEL_MAIN, e1);
        service.subscribe(LiveStreamService.CHANNEL_MAIN, e2);
        assertEquals(2, service.activeConnectionCount());

        LiveStreamCapacityException ex = assertThrows(
                LiveStreamCapacityException.class,
                () -> service.subscribe(LiveStreamService.CHANNEL_MAIN, new RecordingEmitter())
        );
        assertEquals(2, ex.maxConnections());
        assertEquals(2, service.activeConnectionCount());

        e1.triggerCompletion();
        assertEquals(1, service.activeConnectionCount());

        RecordingEmitter e3 = new RecordingEmitter();
        service.subscribe(LiveStreamService.CHANNEL_MAIN, e3);
        assertEquals(2, service.activeConnectionCount());
    }

    @Test
    void broadcastAndHeartbeatIsolateFailingEmittersAndRemoveThem() {
        LiveStreamService service = new LiveStreamService(10, 1800_000L, true, 3600_000L, FIXED_CLOCK);

        RecordingEmitter healthy1 = new RecordingEmitter();
        RecordingEmitter failing = new RecordingEmitter();
        RecordingEmitter healthy2 = new RecordingEmitter();

        service.subscribe(LiveStreamService.CHANNEL_MAIN, healthy1);
        service.subscribe(LiveStreamService.CHANNEL_MAIN, failing);
        service.subscribe(LiveStreamService.CHANNEL_MAIN, healthy2);
        assertEquals(3, service.activeConnectionCount());

        // Make the middle emitter fail on the next send
        failing.failOnNextSend(true);

        service.broadcast("score-update", java.util.Map.of("fixtureId", 101));

        // Failing emitter was pruned; both healthy emitters received hello + score-update (2 sends)
        assertEquals(2, service.activeConnectionCount());
        assertEquals(2, healthy1.sentCount());
        assertEquals(2, healthy2.sentCount());

        // Heartbeat also prunes a newly broken emitter without affecting others
        healthy1.failOnNextSend(true);
        service.sendHeartbeat();
        assertEquals(1, service.activeConnectionCount());
        assertEquals(3, healthy2.sentCount());
    }

    @Test
    void testStreamTicksIncrementSeqPerConnection() {
        LiveStreamService service = new LiveStreamService(10, 1800_000L, true, 3600_000L, FIXED_CLOCK);

        RecordingEmitter t1 = new RecordingEmitter();
        service.subscribe(LiveStreamService.CHANNEL_TEST, t1);
        service.sendTestStreamTicks();

        RecordingEmitter t2 = new RecordingEmitter();
        service.subscribe(LiveStreamService.CHANNEL_TEST, t2);
        service.sendTestStreamTicks();

        // t1 received hello + 2 ticks = 3 events; t2 received hello + 1 tick = 2 events
        assertEquals(3, t1.sentCount());
        assertEquals(2, t2.sentCount());

        List<LiveStreamService.TickEvent> t1Ticks = t1.tickPayloads();
        List<LiveStreamService.TickEvent> t2Ticks = t2.tickPayloads();
        assertEquals(2, t1Ticks.size());
        assertEquals(1L, t1Ticks.get(0).seq());
        assertEquals(2L, t1Ticks.get(1).seq());
        assertEquals(FIXED_NOW.toString(), t1Ticks.get(0).serverTime());
        assertNotNull(t1Ticks.get(0).connectionId());

        assertEquals(1, t2Ticks.size());
        assertEquals(1L, t2Ticks.get(0).seq());
    }

    private static final class RecordingEmitter extends SseEmitter {
        private Runnable completionCallback;
        private Runnable timeoutCallback;
        private Consumer<Throwable> errorCallback;
        private boolean failNext;
        private boolean completed;
        private int sentCount;
        private final List<LiveStreamService.TickEvent> ticks = new ArrayList<>();

        RecordingEmitter() {
            super(60_000L);
        }

        @Override
        public synchronized void onCompletion(Runnable callback) {
            this.completionCallback = callback;
        }

        @Override
        public synchronized void onTimeout(Runnable callback) {
            this.timeoutCallback = callback;
        }

        @Override
        public synchronized void onError(Consumer<Throwable> callback) {
            this.errorCallback = callback;
        }

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            if (failNext) {
                throw new IOException("Simulated broken pipe");
            }
            sentCount++;
            Set<DataWithMediaType> items = builder.build();
            for (DataWithMediaType item : items) {
                if (item.getData() instanceof LiveStreamService.TickEvent tick) {
                    ticks.add(tick);
                }
            }
        }

        @Override
        public synchronized void complete() {
            this.completed = true;
        }

        void failOnNextSend(boolean fail) {
            this.failNext = fail;
        }

        void triggerCompletion() {
            if (completionCallback != null) {
                completionCallback.run();
            }
        }

        void triggerTimeout() {
            if (timeoutCallback != null) {
                timeoutCallback.run();
            }
        }

        void triggerError(Throwable t) {
            if (errorCallback != null) {
                errorCallback.accept(t);
            }
        }

        int sentCount() {
            return sentCount;
        }

        boolean completed() {
            return completed;
        }

        List<LiveStreamService.TickEvent> tickPayloads() {
            return ticks;
        }
    }
}
