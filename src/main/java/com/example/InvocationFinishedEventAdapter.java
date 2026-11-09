package com.example;

import bluej.extensions2.event.ExtensionEvent;
import bluej.extensions2.event.InvocationFinishedEvent;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

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

        String type = switch (ie.getEventType()) {
            case NORMAL_EXIT -> RUN_NORMAL;
            case EXCEPTION_EXIT -> RUN_EXCEPTION;
            case TERMINATED_EXIT -> RUN_TERMINATED;
            case UNKNOWN_EXIT -> RUN_UNKNOWN;
        };

        String[] params = ie.getParameters();
        List<String> paramList = (params == null) ? List.of() : Arrays.asList(params);

        Object result = type.equals(RUN_NORMAL) ? safeResult(ie) : null;

        return build(type, ie.getClassName(), ie.getMethodName(),
                     ie.getObjectName(), paramList, result);
    }

    // BlueJ's getResult() throws a NullPointerException for void methods
    private static Object safeResult(InvocationFinishedEvent ie) {
        try {
            return ie.getResult();
        } catch (RuntimeException e) {
            return null;
        }
    }

    TelemetryRecord build(String type, String className, String methodName,
                          String objectName, List<String> params, Object result) {
        TelemetryRecord r = new TelemetryRecord();
        r.setTimeStamp(Instant.now(clock));
        r.setCategory(CATEGORY);
        r.setEventType(type);

        // Constructors are called on the class (no object yet); methods on an object.
        // BlueJ gives no method name when a method throws or is terminated.
        String kind = (methodName != null || objectName != null) ? "method" : "constructor";

        r.addAttribute("className", className);
        r.addAttribute("invocationKind", kind);
        r.addAttribute("methodName", methodName);
        r.addAttribute("objectName", objectName);
        r.addAttribute("parameters", params);
        r.addAttribute("exception", type.equals(RUN_EXCEPTION));

        if (type.equals(RUN_NORMAL) && kind.equals("method")) {
            r.addAttribute("resultSummary", summarize(result));
        }
        return r;
    }

    static String summarize(Object result) {
        if (result == null) {
            return "void";
        }
        String s = String.valueOf(result);
        if (s.length() > MAX_RESULT_LENGTH) {
            return s.substring(0, MAX_RESULT_LENGTH) + "...";
        }
        return s;
    }
}