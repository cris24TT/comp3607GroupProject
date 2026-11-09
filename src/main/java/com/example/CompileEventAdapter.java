package com.example;

import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.ExtensionEvent;

import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

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

    public CompileEventAdapter(Clock clock) {
        this.clock = clock; 
    }

    @Override
    public TelemetryRecord adapt(ExtensionEvent event) {
        CompileEvent ce = (CompileEvent) event;

        if (!ce.isUserGeneratedCompilation()) {
            return null; 
        }

        File[] files = ce.getFiles();
        List<String> fileNames = (files == null)
                ? List.of()
                : Arrays.stream(files).map(File::getName).toList();

        String type = switch (ce.getEventType()) {
            case COMPILE_START_EVENT -> COMPILE_START;
            case COMPILE_ERROR_EVENT -> COMPILE_ERROR;
            case COMPILE_WARNING_EVENT -> COMPILE_WARNING;
            case COMPILE_DONE_EVENT -> COMPILE_SUCCESS;
            case COMPILE_FAILED_EVENT -> COMPILE_FAILED;
        };

        return build(type, fileNames, ce.getErrorLineNumber(), ce.getErrorMessage());
    }

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

    static String errorType(String message) {
        if (message == null || message.isBlank()) {
            return "unknown";
        }
        return message.split("\\R", 2)[0].trim();
    }}

    