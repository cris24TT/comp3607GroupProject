package com.example;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

/** Detects created/deleted classes and added/removed methods by comparing snapshots. No BlueJ code. */
public class ClassTracker {

    public static final String CATEGORY = "LIFECYCLE";
    public static final String CLASS_CREATED = "CLASS_CREATED";
    public static final String CLASS_DELETED = "CLASS_DELETED";
    public static final String METHOD_ADDED = "METHOD_ADDED";
    public static final String METHOD_REMOVED = "METHOD_REMOVED";

    private final Clock clock;
    private final Set<String> knownClasses = new HashSet<>();
    private final Set<String> createdThisSession = new HashSet<>();
    private final Map<String, Set<String>> knownMethods = new HashMap<>();

    public ClassTracker() {
        this(Clock.systemDefaultZone());
    }

    public ClassTracker(Clock clock) {
        this.clock = clock;
    }

    /** Classes that already exist when the project opens: remember them, report nothing. */
    public void baseline(Set<String> classes) {
        knownClasses.clear();
        knownClasses.addAll(classes);
    }

    public List<TelemetryRecord> updateClasses(Set<String> current) {
        List<TelemetryRecord> out = new ArrayList<>();
        for (String c : new TreeSet<>(current)) {
            if (!knownClasses.contains(c)) {
                out.add(record(CLASS_CREATED, c, null));
                createdThisSession.add(c);
            }
        }
        for (String c : new TreeSet<>(knownClasses)) {
            if (!current.contains(c)) {
                out.add(record(CLASS_DELETED, c, null));
                knownMethods.remove(c);
            }
        }
        knownClasses.clear();
        knownClasses.addAll(current);
        return out;
    }

    public List<TelemetryRecord> updateMethods(String className, Set<String> current) {
        Set<String> previous = knownMethods.put(className, new HashSet<>(current));
        List<TelemetryRecord> out = new ArrayList<>();
        if (previous == null) {
            if (!createdThisSession.contains(className)) {
                return out; // first look at a pre-existing class: baseline silently
            }
            previous = Set.of(); // new class: every method is new
        }
        for (String m : new TreeSet<>(current)) {
            if (!previous.contains(m)) {
                out.add(record(METHOD_ADDED, className, m));
            }
        }
        for (String m : new TreeSet<>(previous)) {
            if (!current.contains(m)) {
                out.add(record(METHOD_REMOVED, className, m));
            }
        }
        return out;
    }

    private TelemetryRecord record(String type, String className, String method) {
        TelemetryRecord r = new TelemetryRecord();
        r.setTimeStamp(Instant.now(clock));
        r.setCategory(CATEGORY);
        r.setEventType(type);
        r.addAttribute("className", className);
        r.addAttribute("method", method);
        return r;
    }
}