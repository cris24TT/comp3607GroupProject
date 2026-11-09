package com.example;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TelemetryAggregator {

    private final List<TelemetryRecord> records;

    public TelemetryAggregator(List<TelemetryRecord> records) {
        this.records = List.copyOf(records);
    }

   
    long count(String eventType) {
        return records.stream()
                .filter(r -> eventType.equals(r.getEventType()))
                .count();
    }

    

    public long compileAttempts() {
        return count(CompileEventAdapter.COMPILE_START);
    }

    public long compileSuccesses() {
        return count(CompileEventAdapter.COMPILE_SUCCESS);
    }

    public long compileFailures() {
        return count(CompileEventAdapter.COMPILE_FAILED);
    }

    public double compileSuccessRate() {
        long attempts = compileAttempts();
        if (attempts == 0) {
            return 0.0;
        }
        return (double) compileSuccesses() / attempts;
    }

    public long compileErrorCount() {
        return count(CompileEventAdapter.COMPILE_ERROR);
    }

    public double errorsPerCompileAttempt() {
        long attempts = compileAttempts();
        if (attempts == 0) {
            return 0.0;
        }
        return (double) compileErrorCount() / attempts;
    }

    public Map<String, Long> errorFrequency() {
        return records.stream()
                .filter(r -> CompileEventAdapter.COMPILE_ERROR.equals(r.getEventType()))
                .collect(Collectors.groupingBy(
                        r -> String.valueOf(r.getAttributes().get("errorType")),
                        Collectors.counting()));
    }

    

    public long runCount() {
        return records.stream()
                .filter(r -> InvocationFinishedEventAdapter.CATEGORY.equals(r.getCategory()))
                .count();
    }

    public long exceptionCount() {
        return count(InvocationFinishedEventAdapter.RUN_EXCEPTION);
    }
}