package com.example;

import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.ExtensionEvent;

import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Adapter pattern: turns a BlueJ CompileEvent into a TelemetryRecord.
public class CompileEventAdapter implements EventAdapter {

    public static final String CATEGORY = "COMPILE";
    public static final String COMPILE_START = "COMPILE_START";
    public static final String COMPILE_ERROR = "COMPILE_ERROR";
    public static final String COMPILE_WARNING = "COMPILE_WARNING";
    public static final String COMPILE_SUCCESS = "COMPILE_SUCCESS";
    public static final String COMPILE_FAILED = "COMPILE_FAILED";

    private final Clock clock;

    public CompileEventAdapter() {
        this(Clock.systemDefaultZone());
    }

    // The tests pass in a fixed clock so the timestamps are always the same.
    public CompileEventAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public TelemetryRecord adapt(ExtensionEvent event) {
        CompileEvent ce = (CompileEvent) event;

        // BlueJ also compiles in the background while you type. Only count compiles the student started.
        if (!ce.isUserGeneratedCompilation()) {
            return null;
        }

        List<String> fileNames = new ArrayList<>();
        if (ce.getFiles() != null) {
            for (File f : ce.getFiles()) {
                fileNames.add(f.getName());
            }
        }

        return build(typeOf(ce.getEventType()), fileNames, ce.getErrorLineNumber(), ce.getErrorMessage());
    }

    private static String typeOf(CompileEvent.EventType type) {
        switch (type) {
            case COMPILE_START_EVENT:
                return COMPILE_START;
            case COMPILE_ERROR_EVENT:
                return COMPILE_ERROR;
            case COMPILE_WARNING_EVENT:
                return COMPILE_WARNING;
            case COMPILE_DONE_EVENT:
                return COMPILE_SUCCESS;
            default:
                return COMPILE_FAILED;
        }
    }

    // Builds the record. Separate from adapt() so the tests can call it without BlueJ.
    TelemetryRecord build(String type, List<String> fileNames, int line, String message) {
        TelemetryRecord r = new TelemetryRecord();
        r.setTimeStamp(Instant.now(clock));
        r.setCategory(CATEGORY);
        r.setEventType(type);
        r.addAttribute("files", fileNames);

        if (type.equals(COMPILE_ERROR) || type.equals(COMPILE_WARNING)) {
            r.addAttribute("line", line);
            r.addAttribute("message", message);
            r.addAttribute("errorType", errorType(message));
        }
        return r;
    }

    // BlueJ gives no error codes, so we use the first line of the message, e.g. "';' expected".
    static String errorType(String message) {
        if (message == null || message.isBlank()) {
            return "unknown";
        }
        String firstLine = message.split("\\R")[0];
        return firstLine.trim();
    }
}
