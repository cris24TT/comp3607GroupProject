package com.example;

import java.time.Duration;
import java.time.Instant;

public class SessionMetadata {
    public static final String STUDENT_ID_PLACEHOLDER = "STUDENT_ID";

    private final String studentId;
    private final String projectName;
    private final Instant sessionStart;
    private final Instant sessionEnd;
    private final long totalSessionSeconds;
    private final long totalCompilationAttempts;

    public SessionMetadata(String projectName, Instant start, Instant end, long compileAttempts) {
        this.studentId = STUDENT_ID_PLACEHOLDER;
        this.projectName = projectName;
        this.sessionStart = start;
        this.sessionEnd = end;
        this.totalSessionSeconds = Duration.between(start, end).getSeconds();
        this.totalCompilationAttempts = compileAttempts;
    }

    public long getTotalSessionSeconds() {
        return totalSessionSeconds;
    }

    public long getTotalCompilationAttempts() {
        return totalCompilationAttempts;
    }
}