package com.premierhub.admin;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminOverviewServiceTest {
    @Test
    void toInstantSupportsSqlTimestampAndLocalDateTimeAsUtc() {
        LocalDateTime wallUtc = LocalDateTime.of(2026, 11, 24, 15, 30, 45, 123_456_000);
        Instant expected = Instant.parse("2026-11-24T15:30:45.123456Z");

        assertEquals(expected, AdminOverviewService.toInstant(Timestamp.valueOf(wallUtc)));
        assertEquals(expected, AdminOverviewService.toInstant(wallUtc));
    }

    @Test
    void toInstantSupportsOffsetDateTimeInstantAndNull() {
        Instant expected = Instant.parse("2026-11-24T15:30:45Z");
        OffsetDateTime plusSeven = OffsetDateTime.of(2026, 11, 24, 22, 30, 45, 0, ZoneOffset.ofHours(7));

        assertEquals(expected, AdminOverviewService.toInstant(plusSeven));
        assertEquals(expected, AdminOverviewService.toInstant(expected));
        assertNull(AdminOverviewService.toInstant(null));
    }

    @Test
    void toInstantRejectsUnsupportedType() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AdminOverviewService.toInstant("2026-11-24T15:30:45Z"));
        assertEquals("Unsupported timestamp type: java.lang.String", ex.getMessage());
    }
}
