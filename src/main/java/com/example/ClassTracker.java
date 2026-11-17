package com.example;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

// BlueJ has no "class created" or "method added" event, so we keep the last list of
// classes (and methods) we saw and compare it with the new one each time.
public class ClassTracker {

    public static final String CATEGORY = "LIFECYCLE";
    public static final String CLASS_CREATED = "CLASS_CREATED";
    public static final String CLASS_DELETED = "CLASS_DELETED";
    public static final String METHOD_ADDED = "METHOD_ADDED";
    public static final String METHOD_REMOVED = "METHOD_REMOVED";

    private final Clock clock;
    private Set<String> knownClasses = new HashSet<>();
    private Set<String> newClasses = new HashSet<>();            // created during this session
    private Map<String, Set<String>> knownMethods = new HashMap<>(); // class name -> its methods

    public ClassTracker() {
        this(Clock.systemDefaultZone());
    }

    public ClassTracker(Clock clock) {
        this.clock = clock;
    }

    // Classes that are already there when the project opens. We don't report these as new.
    public void baseline(Set<String> classes) {
        knownClasses = new HashSet<>(classes);
    }

    public List<TelemetryRecord> updateClasses(Set<String> current) {
        List<TelemetryRecord> found = new ArrayList<>();

        // TreeSet keeps the names in alphabetical order so the output is always the same
        for (String name : new TreeSet<>(current)) {
            if (!knownClasses.contains(name)) {
                found.add(makeRecord(CLASS_CREATED, name, null));
                newClasses.add(name);
            }
        }
        for (String name : new TreeSet<>(knownClasses)) {
            if (!current.contains(name)) {
                found.add(makeRecord(CLASS_DELETED, name, null));
                knownMethods.remove(name);
            }
        }

        knownClasses = new HashSet<>(current);
        return found;
    }

    public List<TelemetryRecord> updateMethods(String className, Set<String> current) {
        List<TelemetryRecord> found = new ArrayList<>();
        Set<String> previous = knownMethods.get(className);
        knownMethods.put(className, new HashSet<>(current));

        if (previous == null) {
            if (!newClasses.contains(className)) {
                // First time we see an old class: just remember its methods.
                return found;
            }
            // A class made this session: all its methods are new.
            previous = new HashSet<>();
        }

        for (String method : new TreeSet<>(current)) {
            if (!previous.contains(method)) {
                found.add(makeRecord(METHOD_ADDED, className, method));
            }
        }
        for (String method : new TreeSet<>(previous)) {
            if (!current.contains(method)) {
                found.add(makeRecord(METHOD_REMOVED, className, method));
            }
        }
        return found;
    }

    private TelemetryRecord makeRecord(String type, String className, String method) {
        TelemetryRecord r = new TelemetryRecord();
        r.setTimeStamp(Instant.now(clock));
        r.setCategory(CATEGORY);
        r.setEventType(type);
        r.addAttribute("className", className);
        r.addAttribute("method", method);
        return r;
    }
}
