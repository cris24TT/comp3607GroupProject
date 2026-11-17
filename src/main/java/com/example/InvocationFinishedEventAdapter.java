package com.example;

import bluej.extensions2.event.ExtensionEvent;
import bluej.extensions2.event.InvocationFinishedEvent;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Adapter pattern: turns a BlueJ InvocationFinishedEvent (a method or constructor run) into a TelemetryRecord.
public class InvocationFinishedEventAdapter implements EventAdapter {

    public static final String CATEGORY = "INVOCATION";
    public static final String RUN_NORMAL = "RUN_NORMAL";
    public static final String RUN_EXCEPTION = "RUN_EXCEPTION";
    public static final String RUN_TERMINATED = "RUN_TERMINATED";
    public static final String RUN_UNKNOWN = "RUN_UNKNOWN";

    static final int MAX_RESULT_LENGTH = 100;

    private final Clock clock;

    public InvocationFinishedEventAdapter() {
        this(Clock.systemDefaultZone());
    }

    public InvocationFinishedEventAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public TelemetryRecord adapt(ExtensionEvent event) {
        InvocationFinishedEvent ie = (InvocationFinishedEvent) event;
        String type = typeOf(ie.getEventType());

        List<String> params = new ArrayList<>();
        if (ie.getParameters() != null) {
            params = Arrays.asList(ie.getParameters());
        }

        Object result = null;
        if (type.equals(RUN_NORMAL)) {
            try {
                result = ie.getResult();
            } catch (RuntimeException e) {
                // BlueJ throws a NullPointerException here when the method is void
                result = null;
            }
        }

        return build(type, ie.getClassName(), ie.getMethodName(), ie.getObjectName(), params, result);
    }

    private static String typeOf(InvocationFinishedEvent.EventType type) {
        switch (type) {
            case NORMAL_EXIT:
                return RUN_NORMAL;
            case EXCEPTION_EXIT:
                return RUN_EXCEPTION;
            case TERMINATED_EXIT:
                return RUN_TERMINATED;
            default:
                return RUN_UNKNOWN;
        }
    }

    TelemetryRecord build(String type, String className, String methodName,
                          String objectName, List<String> params, Object result) {
        TelemetryRecord r = new TelemetryRecord();
        r.setTimeStamp(Instant.now(clock));
        r.setCategory(CATEGORY);
        r.setEventType(type);

        // A constructor runs on the class (there is no object yet). A method runs on an object.
        // When a method throws, BlueJ gives no method name, but the object name is still there.
        String kind = "constructor";
        if (methodName != null || objectName != null) {
            kind = "method";
        }

        r.addAttribute("className", className);
        r.addAttribute("invocationKind", kind);
        r.addAttribute("methodName", methodName);
        r.addAttribute("objectName", objectName);
        r.addAttribute("parameters", params);
        r.addAttribute("exception", type.equals(RUN_EXCEPTION));

        // BlueJ doesn't give us the console output, so the return value is our "output summary".
        if (type.equals(RUN_NORMAL) && kind.equals("method")) {
            r.addAttribute("resultSummary", summarize(result));
        }
        return r;
    }

    // Turns the return value into short text. Long values are cut to 100 characters.
    static String summarize(Object result) {
        if (result == null) {
            return "void";
        }
        String text = String.valueOf(result);
        if (text.length() > MAX_RESULT_LENGTH) {
            return text.substring(0, MAX_RESULT_LENGTH) + "...";
        }
        return text;
    }
}
