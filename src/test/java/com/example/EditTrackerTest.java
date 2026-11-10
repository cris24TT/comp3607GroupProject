package com.example;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EditTrackerTest {

    private static final Instant T0 = Instant.parse("2026-11-09T21:00:00Z");

    @Test
    void editsCloseTogetherFormOneBurst() {
        EditTracker t = new EditTracker();

        assertTrue(t.onEdit("A", T0, 1, 0, 0, 0).isEmpty());
        assertTrue(t.onEdit("A", T0.plusSeconds(1), 1, 0, 0, 0).isEmpty());
        assertTrue(t.onEdit("A", T0.plusSeconds(2), 0, 1, 0, 0).isEmpty());

        List<TelemetryRecord> out = t.flush();

        assertEquals(1, out.size());
        Map<String, Object> a = out.get(0).getAttributes();
        assertEquals(EditTracker.EDIT_BURST, out.get(0).getEventType());
        assertEquals(T0, out.get(0).getTimestamp());
        assertEquals(3, a.get("edits"));
        assertEquals(2, a.get("charsInserted"));
        assertEquals(1, a.get("charsDeleted"));
        assertEquals(2000L, a.get("gapSumMs"));
        assertEquals(2000L, a.get("durationMs"));
        assertEquals(T0.plusSeconds(2), a.get("end"));
    }

    @Test
    void longPauseStartsNewBurst() {
        EditTracker t = new EditTracker();
        t.onEdit("A", T0, 1, 0, 0, 0);

        List<TelemetryRecord> out = t.onEdit("A", T0.plusSeconds(30), 1, 0, 0, 0);

        assertEquals(1, out.size());
        assertEquals(1, out.get(0).getAttributes().get("edits"));
        assertEquals(1, t.flush().size()); // the second burst is still open
    }

    @Test
    void switchingClassStartsNewBurst() {
        EditTracker t = new EditTracker();
        t.onEdit("A", T0, 5, 0, 1, 0);

        List<TelemetryRecord> out = t.onEdit("B", T0.plusSeconds(1), 1, 0, 0, 0);

        assertEquals(1, out.size());
        assertEquals("A", out.get(0).getAttributes().get("className"));
        assertEquals(1, out.get(0).getAttributes().get("linesAdded"));
    }

    @Test
    void flushWithNothingOpenReturnsEmpty() {
        EditTracker t = new EditTracker();

        assertTrue(t.flush().isEmpty());
        t.onEdit("A", T0, 1, 0, 0, 0);
        t.flush();
        assertTrue(t.flush().isEmpty());
    }
}