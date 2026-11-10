package com.example;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Groups individual edits into bursts so the JSON stays small. No BlueJ code, so it is unit-testable. */
public class EditTracker {

    public static final String CATEGORY = "EDIT";
    public static final String EDIT_BURST = "EDIT_BURST";
    public static final Duration DEFAULT_BURST_GAP = Duration.ofSeconds(5);

    private final Duration burstGap;

    private String className;
    private Instant start;
    private Instant last;
    private int edits;
    private int charsInserted;
    private int charsDeleted;
    private int linesAdded;
    private int linesRemoved;
    private long gapSumMs;

    public EditTracker() {
        this(DEFAULT_BURST_GAP);
    }

    public EditTracker(Duration burstGap) {
        this.burstGap = burstGap;
    }

    /** Records one edit. Returns the previous burst if this edit starts a new one, otherwise an empty list. */
    public synchronized List<TelemetryRecord> onEdit(String cls, Instant time,
                                                     int inserted, int deleted, int added, int removed) {
        List<TelemetryRecord> out = new ArrayList<>();
        if (className != null
                && (!className.equals(cls) || Duration.between(last, time).compareTo(burstGap) > 0)) {
            out.addAll(flush());
        }
        if (className == null) {
            className = cls;
            start = time;
        } else {
            gapSumMs += Duration.between(last, time).toMillis();
        }
        last = time;
        edits++;
        charsInserted += inserted;
        charsDeleted += deleted;
        linesAdded += added;
        linesRemoved += removed;
        return out;
    }

    /** Closes the current burst (if any) and returns it as a record. */
    public synchronized List<TelemetryRecord> flush() {
        if (className == null) {
            return List.of();
        }
        TelemetryRecord r = new TelemetryRecord();
        r.setTimeStamp(start);
        r.setCategory(CATEGORY);
        r.setEventType(EDIT_BURST);
        r.addAttribute("className", className);
        r.addAttribute("edits", edits);
        r.addAttribute("charsInserted", charsInserted);
        r.addAttribute("charsDeleted", charsDeleted);
        r.addAttribute("linesAdded", linesAdded);
        r.addAttribute("linesRemoved", linesRemoved);
        r.addAttribute("durationMs", Duration.between(start, last).toMillis());
        r.addAttribute("gapSumMs", gapSumMs);
        r.addAttribute("end", last);

        className = null;
        start = null;
        last = null;
        edits = 0;
        charsInserted = 0;
        charsDeleted = 0;
        linesAdded = 0;
        linesRemoved = 0;
        gapSumMs = 0;
        return List.of(r);
    }
}