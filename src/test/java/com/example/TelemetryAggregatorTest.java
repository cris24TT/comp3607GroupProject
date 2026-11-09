package com.example;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryAggregatorTest {

    // helper: make a record without BlueJ
    private static TelemetryRecord rec(String category, String type) {
        TelemetryRecord r = new TelemetryRecord();
        r.setCategory(category);
        r.setEventType(type);
        return r;
    }

    private static TelemetryRecord compileError(String errorType) {
        TelemetryRecord r = rec(CompileEventAdapter.CATEGORY, CompileEventAdapter.COMPILE_ERROR);
        r.addAttribute("errorType", errorType);
        return r;
    }

    @Test
    void emptySessionGivesZeros() {
        TelemetryAggregator agg = new TelemetryAggregator(List.of());

        assertEquals(0, agg.compileAttempts());
        assertEquals(0.0, agg.compileSuccessRate(), 0.0001);
        assertEquals(0.0, agg.errorsPerCompileAttempt(), 0.0001);
        assertTrue(agg.errorFrequency().isEmpty());
    }

    @Test
    void successRateIsSuccessesOverAttempts() {
        List<TelemetryRecord> records = new ArrayList<>();
        // attempt 1: fails
        records.add(rec(CompileEventAdapter.CATEGORY, CompileEventAdapter.COMPILE_START));
        records.add(compileError("';' expected"));
        records.add(rec(CompileEventAdapter.CATEGORY, CompileEventAdapter.COMPILE_FAILED));
        // attempt 2: succeeds
        records.add(rec(CompileEventAdapter.CATEGORY, CompileEventAdapter.COMPILE_START));
        records.add(rec(CompileEventAdapter.CATEGORY, CompileEventAdapter.COMPILE_SUCCESS));

        TelemetryAggregator agg = new TelemetryAggregator(records);

        assertEquals(2, agg.compileAttempts());
        assertEquals(1, agg.compileSuccesses());
        assertEquals(1, agg.compileFailures());
        assertEquals(0.5, agg.compileSuccessRate(), 0.0001);
    }

    @Test
    void errorFrequencyGroupsByErrorType() {
        TelemetryAggregator agg = new TelemetryAggregator(List.of(
                compileError("cannot find symbol"),
                compileError("cannot find symbol"),
                compileError("';' expected")));

        Map<String, Long> freq = agg.errorFrequency();

        assertEquals(2L, freq.get("cannot find symbol"));
        assertEquals(1L, freq.get("';' expected"));
    }

    @Test
    void errorsPerAttemptCountsEveryError() {
        List<TelemetryRecord> records = new ArrayList<>();

        records.add(rec(CompileEventAdapter.CATEGORY, CompileEventAdapter.COMPILE_START));
        records.add(compileError("cannot find symbol"));
        records.add(compileError("cannot find symbol"));
        records.add(rec(CompileEventAdapter.CATEGORY, CompileEventAdapter.COMPILE_FAILED));

        records.add(rec(CompileEventAdapter.CATEGORY, CompileEventAdapter.COMPILE_START));
        records.add(compileError("';' expected"));
        records.add(rec(CompileEventAdapter.CATEGORY, CompileEventAdapter.COMPILE_FAILED));

        TelemetryAggregator agg = new TelemetryAggregator(records);

        assertEquals(3, agg.compileErrorCount());
        assertEquals(1.5, agg.errorsPerCompileAttempt(), 0.0001);
    }

    @Test
    void runAndExceptionCounts() {
        List<TelemetryRecord> records = new ArrayList<>();
        records.add(rec(InvocationFinishedEventAdapter.CATEGORY, InvocationFinishedEventAdapter.RUN_NORMAL));
        records.add(rec(InvocationFinishedEventAdapter.CATEGORY, InvocationFinishedEventAdapter.RUN_EXCEPTION));
        records.add(rec(InvocationFinishedEventAdapter.CATEGORY, InvocationFinishedEventAdapter.RUN_NORMAL));
        // a compile record must NOT count as a run
        records.add(rec(CompileEventAdapter.CATEGORY, CompileEventAdapter.COMPILE_START));

        TelemetryAggregator agg = new TelemetryAggregator(records);

        assertEquals(3, agg.runCount());
        assertEquals(1, agg.exceptionCount());
    }

    private static TelemetryRecord at(String isoTime) {
        TelemetryRecord r = rec(CompileEventAdapter.CATEGORY, CompileEventAdapter.COMPILE_START);
        r.setTimeStamp(Instant.parse(isoTime));
        return r;
    }

    @Test
    void gapsLongerThanThresholdCountAsIdle() {
        TelemetryAggregator agg = new TelemetryAggregator(List.of(
                at("2026-11-09T10:00:00Z"),
                at("2026-11-09T10:02:00Z"), // 2 min gap -> active
                at("2026-11-09T10:12:00Z"), // 10 min gap -> idle
                at("2026-11-09T10:13:00Z")), // 1 min gap -> active
                Duration.ofMinutes(5));

        assertEquals(Duration.ofMinutes(13), agg.sessionSpan());
        assertEquals(Duration.ofMinutes(10), agg.idleTime());
        assertEquals(Duration.ofMinutes(3), agg.activeTime());
    }

    @Test
    void singleEventHasNoSpan() {
        TelemetryAggregator agg = new TelemetryAggregator(List.of(at("2026-11-09T10:00:00Z")));

        assertEquals(Duration.ZERO, agg.sessionSpan());
        assertEquals(Duration.ZERO, agg.activeTime());
    }

}