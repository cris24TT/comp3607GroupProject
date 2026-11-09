package com.example;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TelemetryAggregator {

    public static final Duration DEFAULT_IDLE_THRESHOLD = Duration.ofMinutes(5);

    private final List<TelemetryRecord> records;
    private final Duration idleThreshold;

    public TelemetryAggregator(List<TelemetryRecord> records) {
        this(records, DEFAULT_IDLE_THRESHOLD);
    }

    public TelemetryAggregator(List<TelemetryRecord> records, Duration idleThreshold) {
        this.records = List.copyOf(records);
        this.idleThreshold = idleThreshold;
    }

    long count(String eventType) {
        return records.stream()
                .filter(r -> eventType.equals(r.getEventType()))
                .count();
    }

    // ---------- compile stats ----------

    public long compileAttempts() {
        return count(CompileEventAdapter.COMPILE_START);
    }

    public long compileSuccesses() {
        return count(CompileEventAdapter.COMPILE_SUCCESS);
    }

    public long compileFailures() {
        return count(CompileEventAdapter.COMPILE_FAILED);
    }

    public double compileSuccessRate() {
        long attempts = compileAttempts();
        if (attempts == 0) {
            return 0.0;
        }
        return (double) compileSuccesses() / attempts;
    }

    public long compileErrorCount() {
        return count(CompileEventAdapter.COMPILE_ERROR);
    }

    public double errorsPerCompileAttempt() {
        long attempts = compileAttempts();
        if (attempts == 0) {
            return 0.0;
        }
        return (double) compileErrorCount() / attempts;
    }

    public Map<String, Long> errorFrequency() {
        return records.stream()
                .filter(r -> CompileEventAdapter.COMPILE_ERROR.equals(r.getEventType()))
                .collect(Collectors.groupingBy(
                        r -> String.valueOf(r.getAttributes().get("errorType")),
                        Collectors.counting()));
    }

    // ---------- run stats ----------

    public long runCount() {
        return records.stream()
                .filter(r -> InvocationFinishedEventAdapter.CATEGORY.equals(r.getCategory()))
                .count();
    }

    public long exceptionCount() {
        return count(InvocationFinishedEventAdapter.RUN_EXCEPTION);
    }

    // ---------- time stats ----------

    private List<Instant> sortedTimestamps() {
        return records.stream()
                .map(TelemetryRecord::getTimestamp)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    /** Time from the first event to the last event. */
    public Duration sessionSpan() {
        List<Instant> times = sortedTimestamps();
        if (times.size() < 2) {
            return Duration.ZERO;
        }
        return Duration.between(times.get(0), times.get(times.size() - 1));
    }

    /** Sum of all gaps between events that are longer than the idle threshold. */
    public Duration idleTime() {
        List<Instant> times = sortedTimestamps();
        Duration idle = Duration.ZERO;
        for (int i = 1; i < times.size(); i++) {
            Duration gap = Duration.between(times.get(i - 1), times.get(i));
            if (gap.compareTo(idleThreshold) > 0) {
                idle = idle.plus(gap);
            }
        }
        return idle;
    }

    public Duration activeTime() {
        return sessionSpan().minus(idleTime());
    }
}