package com.example;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypingStatsTest {

    private static final Instant T0 = Instant.parse("2026-11-09T21:00:00Z");

    private static TelemetryRecord burst(Instant start, Instant end, int edits, long gapSumMs) {
        TelemetryRecord r = new TelemetryRecord();
        r.setTimeStamp(start);
        r.setCategory(EditTracker.CATEGORY);
        r.setEventType(EditTracker.EDIT_BURST);
        r.addAttribute("edits", edits);
        r.addAttribute("gapSumMs", gapSumMs);
        r.addAttribute("end", end);
        return r;
    }

    @Test
    void cadenceAndFrequencyFromBursts() {
        TelemetryAggregator agg = new TelemetryAggregator(List.of(
                burst(T0, T0.plusSeconds(60), 3, 2000)));   // 3 edits, 2 gaps totalling 2 s, over 1 minute

        assertEquals(1, agg.editBurstCount());
        assertEquals(3, agg.totalEdits());
        assertEquals(1000.0, agg.typingCadenceMs(), 0.001);
        assertEquals(60, agg.activeTime().getSeconds());     // burst end counts as activity
        assertEquals(3.0, agg.editsPerActiveMinute(), 0.001);
    }

    @Test
    void noTypingGivesZeros() {
        TelemetryAggregator agg = new TelemetryAggregator(List.of());

        assertEquals(0, agg.totalEdits());
        assertEquals(0.0, agg.typingCadenceMs(), 0.001);
        assertEquals(0.0, agg.editsPerActiveMinute(), 0.001);
    }
}