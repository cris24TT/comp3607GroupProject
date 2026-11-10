package com.example;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class PackageEventAdapterTest {

    private static final Instant NOW = Instant.parse("2026-11-09T21:00:00Z");

    @Test
    void freshFolderMeansProjectCreated() {
        assertEquals(PackageEventAdapter.PROJECT_CREATED,
                PackageEventAdapter.openedType(NOW.minusSeconds(30), NOW));
    }

    @Test
    void oldFolderMeansProjectOpened() {
        assertEquals(PackageEventAdapter.PROJECT_OPENED,
                PackageEventAdapter.openedType(NOW.minusSeconds(3600), NOW));
    }

    @Test
    void unknownFolderAgeMeansProjectOpened() {
        assertEquals(PackageEventAdapter.PROJECT_OPENED,
                PackageEventAdapter.openedType(null, NOW));
    }

    @Test
    void buildFillsRecord() {
        PackageEventAdapter adapter = new PackageEventAdapter(Clock.fixed(NOW, ZoneOffset.UTC));

        TelemetryRecord r = adapter.build(PackageEventAdapter.PROJECT_CLOSED, "Demo", 3);

        assertEquals(PackageEventAdapter.CATEGORY, r.getCategory());
        assertEquals(PackageEventAdapter.PROJECT_CLOSED, r.getEventType());
        assertEquals(NOW, r.getTimestamp());
        assertEquals("Demo", r.getAttributes().get("projectName"));
        assertEquals(3, r.getAttributes().get("classCount"));
    }

    @Test
    void missingClassCountIsLeftOut() {
        TelemetryRecord r = new PackageEventAdapter().build(PackageEventAdapter.PROJECT_OPENED, "Demo", null);

        assertFalse(r.getAttributes().containsKey("classCount"));
    }
}
