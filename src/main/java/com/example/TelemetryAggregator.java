package com.example;
 
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
 
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
 
    // ---------- creation timeline ----------
 
    /**
     * Classes created and methods added this session, oldest first.
     * Each entry: time (ISO string), event, className and, for methods, method.
     */
    public List<Map<String, Object>> creationTimeline() {
        return records.stream()
                .filter(r -> ClassTracker.CLASS_CREATED.equals(r.getEventType())
                        || ClassTracker.METHOD_ADDED.equals(r.getEventType()))
                .sorted(Comparator.comparing(TelemetryRecord::getTimestamp,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(TelemetryAggregator::timelineEntry)
                .toList();
    }
 
    private static Map<String, Object> timelineEntry(TelemetryRecord r) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("time", r.getTimestamp() == null ? null : r.getTimestamp().toString());
        entry.put("event", r.getEventType());
        entry.put("className", r.getAttributes().get("className"));
        Object method = r.getAttributes().get("method");
        if (method != null) {
            entry.put("method", method);
        }
        return entry;
    }
 
    // ---------- time stats ----------
 
        private List<Instant> sortedTimestamps() {
        return records.stream()
                .flatMap(r -> {
                    Stream.Builder<Instant> b = Stream.builder();
                    if (r.getTimestamp() != null) {
                        b.add(r.getTimestamp());
                    }
                    if (r.getAttributes().get("end") instanceof Instant end) {
                        b.add(end);
                    }
                    return b.build();
                })
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
        // ---------- typing stats ----------

    private List<TelemetryRecord> editBursts() {
        return records.stream()
                .filter(r -> EditTracker.EDIT_BURST.equals(r.getEventType()))
                .toList();
    }

    private static long longAttr(TelemetryRecord r, String key) {
        return r.getAttributes().get(key) instanceof Number n ? n.longValue() : 0L;
    }

    public long editBurstCount() {
        return editBursts().size();
    }

    public long totalEdits() {
        return editBursts().stream().mapToLong(r -> longAttr(r, "edits")).sum();
    }

    /** Typing cadence: average milliseconds between consecutive edits while typing. */
    public double typingCadenceMs() {
        long gaps = editBursts().stream().mapToLong(r -> longAttr(r, "gapSumMs")).sum();
        long intervals = editBursts().stream().mapToLong(r -> Math.max(0, longAttr(r, "edits") - 1)).sum();
        return intervals == 0 ? 0.0 : (double) gaps / intervals;
    }

    /** Edit frequency: edits per minute of active time. */
    public double editsPerActiveMinute() {
        double minutes = activeTime().toMillis() / 60000.0;
        return minutes == 0 ? 0.0 : totalEdits() / minutes;
    }
}
