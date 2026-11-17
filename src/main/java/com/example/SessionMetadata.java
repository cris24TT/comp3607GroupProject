package com.example;

import java.time.Duration;
import java.time.Instant;

// Information about the whole session, written at the top of the JSON file.
public class SessionMetadata {

    public static final String STUDENT_ID_PLACEHOLDER = "STUDENT_ID";

    private String studentId;
    private String projectName;
    private Instant sessionStart;
    private Instant sessionEnd;
    private long totalSessionSeconds;
    private long totalCompilationAttempts;

    public SessionMetadata(String projectName, Instant start, Instant end, long compileAttempts) {
        this.studentId = STUDENT_ID_PLACEHOLDER;
        this.projectName = projectName;
        this.sessionStart = start;
        this.sessionEnd = end;
        this.totalSessionSeconds = Duration.between(start, end).getSeconds();
        this.totalCompilationAttempts = compileAttempts;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getProjectName() {
        return projectName;
    }

    public Instant getSessionStart() {
        return sessionStart;
    }

    public Instant getSessionEnd() {
        return sessionEnd;
    }

    public long getTotalSessionSeconds() {
        return totalSessionSeconds;
    }

    public long getTotalCompilationAttempts() {
        return totalCompilationAttempts;
    }
}
