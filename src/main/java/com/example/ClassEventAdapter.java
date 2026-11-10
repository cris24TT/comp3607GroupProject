package com.example;

import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ExtensionEvent;

import java.time.Clock;
import java.time.Instant;

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

        String type = switch (ce.getEventType()) {
            case STATE_CHANGED -> ce.isClassCompiled() ? CLASS_COMPILED : CLASS_MODIFIED;
            case CHANGED_NAME -> CLASS_RENAMED;
            case REMOVED -> CLASS_REMOVED;
        };

        return build(type, safeName(ce), ce.getOldName(), ce.hasError());
    }

    private static String safeName(ClassEvent ce) {
        try {
            return ce.getBClass().getName();
        } catch (Exception e) {
            return "unknown";
        }
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