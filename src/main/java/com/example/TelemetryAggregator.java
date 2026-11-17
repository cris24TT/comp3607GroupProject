package com.example;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Works out the summary numbers for a session from its list of records.
public class TelemetryAggregator {

    public static final Duration DEFAULT_IDLE_THRESHOLD = Duration.ofMinutes(5);

    private final List<TelemetryRecord> records;
    private final Duration idleThreshold;

    public TelemetryAggregator(List<TelemetryRecord> records) {
        this(records, DEFAULT_IDLE_THRESHOLD);
    }

    public TelemetryAggregator(List<TelemetryRecord> records, Duration idleThreshold) {
        this.records = new ArrayList<>(records);
        this.idleThreshold = idleThreshold;
    }

    long count(String eventType) {
        long total = 0;
        for (TelemetryRecord r : records) {
            if (eventType.equals(r.getEventType())) {
                total++;
            }
        }
        return total;
    }

    // Compiling

    public long compileAttempts() {
        return count(CompileEventAdapter.COMPILE_START);
    }

    public long compileSuccesses() {
        return count(CompileEventAdapter.COMPILE_SUCCESS);
    }

    public long compileFailures() {
        return count(CompileEventAdapter.COMPILE_FAILED);
    }

    public long compileErrorCount() {
        return count(CompileEventAdapter.COMPILE_ERROR);
    }

    public double compileSuccessRate() {
        if (compileAttempts() == 0) {
            return 0.0;
        }
        return (double) compileSuccesses() / compileAttempts();
    }

    public double errorsPerCompileAttempt() {
        if (compileAttempts() == 0) {
            return 0.0;
        }
        return (double) compileErrorCount() / compileAttempts();
    }

    // How many times each kind of error happened, e.g. "';' expected" -> 2
    public Map<String, Long> errorFrequency() {
        Map<String, Long> frequency = new HashMap<>();
        for (TelemetryRecord r : records) {
            if (CompileEventAdapter.COMPILE_ERROR.equals(r.getEventType())) {
                String type = String.valueOf(r.getAttributes().get("errorType"));
                frequency.put(type, frequency.getOrDefault(type, 0L) + 1);
            }
        }
        return frequency;
    }

    // Running

    public long runCount() {
        long total = 0;
        for (TelemetryRecord r : records) {
            if (InvocationFinishedEventAdapter.CATEGORY.equals(r.getCategory())) {
                total++;
            }
        }
        return total;
    }

    public long exceptionCount() {
        return count(InvocationFinishedEventAdapter.RUN_EXCEPTION);
    }

    // Time

    // All the times something happened, in order. An edit burst counts at its start and its end.
    private List<Instant> activityTimes() {
        List<Instant> times = new ArrayList<>();
        for (TelemetryRecord r : records) {
            if (r.getTimestamp() != null) {
                times.add(r.getTimestamp());
            }
            if (r.getAttributes().get("end") instanceof Instant end) {
                times.add(end);
            }
        }
        Collections.sort(times);
        return times;
    }

    // Time from the first event to the last one.
    public Duration sessionSpan() {
        List<Instant> times = activityTimes();
        if (times.size() < 2) {
            return Duration.ZERO;
        }
        return Duration.between(times.get(0), times.get(times.size() - 1));
    }

    // Any gap between events longer than the threshold counts as idle.
    public Duration idleTime() {
        List<Instant> times = activityTimes();
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

    // Typing

    private List<TelemetryRecord> editBursts() {
        List<TelemetryRecord> bursts = new ArrayList<>();
        for (TelemetryRecord r : records) {
            if (EditTracker.EDIT_BURST.equals(r.getEventType())) {
                bursts.add(r);
            }
        }
        return bursts;
    }

    private static long number(TelemetryRecord r, String key) {
        Object value = r.getAttributes().get(key);
        if (value instanceof Number n) {
            return n.longValue();
        }
        return 0;
    }

    public long editBurstCount() {
        return editBursts().size();
    }

    public long totalEdits() {
        long total = 0;
        for (TelemetryRecord burst : editBursts()) {
            total += number(burst, "edits");
        }
        return total;
    }

    // Average milliseconds between two edits in a row.
    public double typingCadenceMs() {
        long totalGapMs = 0;
        long gaps = 0;
        for (TelemetryRecord burst : editBursts()) {
            totalGapMs += number(burst, "gapSumMs");
            gaps += Math.max(0, number(burst, "edits") - 1);
        }
        if (gaps == 0) {
            return 0.0;
        }
        return (double) totalGapMs / gaps;
    }

    public double editsPerActiveMinute() {
        double minutes = activeTime().toMillis() / 60000.0;
        if (minutes == 0) {
            return 0.0;
        }
        return totalEdits() / minutes;
    }

    // Creation timeline

    // Project created, classes created and methods added, oldest first.
    public List<Map<String, Object>> creationTimeline() {
        List<TelemetryRecord> created = new ArrayList<>();
        for (TelemetryRecord r : records) {
            String type = r.getEventType();
            if (ClassTracker.CLASS_CREATED.equals(type)
                    || ClassTracker.METHOD_ADDED.equals(type)
                    || PackageEventAdapter.PROJECT_CREATED.equals(type)) {
                created.add(r);
            }
        }
        created.sort((a, b) -> compareTimes(a.getTimestamp(), b.getTimestamp()));

        List<Map<String, Object>> timeline = new ArrayList<>();
        for (TelemetryRecord r : created) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("time", r.getTimestamp() == null ? null : r.getTimestamp().toString());
            entry.put("event", r.getEventType());
            entry.putAll(r.getAttributes());
            timeline.add(entry);
        }
        return timeline;
    }

    // Records without a time go last.
    private static int compareTimes(Instant a, Instant b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return a.compareTo(b);
    }
}
