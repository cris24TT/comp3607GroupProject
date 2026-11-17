package com.example;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Groups single keystrokes into "edit bursts". Edits in the same class with less than
// 5 seconds between them go in the same burst. This keeps the JSON file small.
public class EditTracker {

    public static final String CATEGORY = "EDIT";
    public static final String EDIT_BURST = "EDIT_BURST";
    public static final Duration DEFAULT_BURST_GAP = Duration.ofSeconds(5);

    private final Duration burstGap;

    // The burst we are currently building (className is null when there is none)
    private String className;
    private Instant start;
    private Instant last;
    private int edits;
    private int charsInserted;
    private int charsDeleted;
    private int linesAdded;
    private int linesRemoved;
    private long gapSumMs; // total time between edits, used for typing cadence

    public EditTracker() {
        this(DEFAULT_BURST_GAP);
    }

    public EditTracker(Duration burstGap) {
        this.burstGap = burstGap;
    }

    // Called for every edit. If this edit starts a new burst, the finished burst is returned.
    public synchronized List<TelemetryRecord> onEdit(String cls, Instant time,
                                                     int inserted, int deleted, int added, int removed) {
        List<TelemetryRecord> finished = new ArrayList<>();

        if (className != null) {
            boolean otherClass = !className.equals(cls);
            boolean longPause = Duration.between(last, time).compareTo(burstGap) > 0;
            if (otherClass || longPause) {
                finished = flush();
            }
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
        return finished;
    }

    // Ends the current burst and returns it as a record (or an empty list if there is none).
    public synchronized List<TelemetryRecord> flush() {
        List<TelemetryRecord> result = new ArrayList<>();
        if (className == null) {
            return result;
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
        result.add(r);

        reset();
        return result;
    }

    private void reset() {
        className = null;
        start = null;
        last = null;
        edits = 0;
        charsInserted = 0;
        charsDeleted = 0;
        linesAdded = 0;
        linesRemoved = 0;
        gapSumMs = 0;
    }
}
