package com.example;

import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ExtensionEvent;

import java.time.Clock;
import java.time.Instant;

// Adapter pattern: turns a BlueJ ClassEvent (class changed, compiled, renamed or removed) into a TelemetryRecord.
public class ClassEventAdapter implements EventAdapter {

    public static final String CATEGORY = "CLASS";
    public static final String CLASS_MODIFIED = "CLASS_MODIFIED";
    public static final String CLASS_COMPILED = "CLASS_COMPILED";
    public static final String CLASS_RENAMED = "CLASS_RENAMED";
    public static final String CLASS_REMOVED = "CLASS_REMOVED";

    private final Clock clock;

    public ClassEventAdapter() {
        this(Clock.systemDefaultZone());
    }

    public ClassEventAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public TelemetryRecord adapt(ExtensionEvent event) {
        ClassEvent ce = (ClassEvent) event;

        String type;
        if (ce.getEventType() == ClassEvent.EventType.CHANGED_NAME) {
            type = CLASS_RENAMED;
        } else if (ce.getEventType() == ClassEvent.EventType.REMOVED) {
            type = CLASS_REMOVED;
        } else if (ce.isClassCompiled()) {
            type = CLASS_COMPILED;
        } else {
            type = CLASS_MODIFIED; // the class was edited and needs compiling again
        }

        String className;
        try {
            className = ce.getBClass().getName();
        } catch (Exception e) {
            className = "unknown"; // e.g. the class was already removed
        }

        return build(type, className, ce.getOldName(), ce.hasError());
    }

    TelemetryRecord build(String type, String className, String oldName, boolean hasError) {
        TelemetryRecord r = new TelemetryRecord();
        r.setTimeStamp(Instant.now(clock));
        r.setCategory(CATEGORY);
        r.setEventType(type);
        r.addAttribute("className", className);
        r.addAttribute("oldName", oldName);
        r.addAttribute("hasError", hasError);
        return r;
    }
}
