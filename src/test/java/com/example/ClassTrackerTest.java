package com.example;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClassTrackerTest {

    @Test
    void newClassIsReportedAsCreated() {
        ClassTracker t = new ClassTracker();
        t.baseline(Set.of("A"));

        List<TelemetryRecord> out = t.updateClasses(Set.of("A", "B"));

        assertEquals(1, out.size());
        assertEquals(ClassTracker.CLASS_CREATED, out.get(0).getEventType());
        assertEquals("B", out.get(0).getAttributes().get("className"));
    }

    @Test
    void missingClassIsReportedAsDeleted() {
        ClassTracker t = new ClassTracker();
        t.baseline(Set.of("A", "B"));

        List<TelemetryRecord> out = t.updateClasses(Set.of("A"));

        assertEquals(1, out.size());
        assertEquals(ClassTracker.CLASS_DELETED, out.get(0).getEventType());
    }

    @Test
    void existingClassMethodsAreBaselinedThenChangesReported() {
        ClassTracker t = new ClassTracker();
        t.baseline(Set.of("A"));

        assertTrue(t.updateMethods("A", Set.of("foo()")).isEmpty());

        List<TelemetryRecord> out = t.updateMethods("A", Set.of("bar(int)"));

        assertEquals(2, out.size());
        assertEquals(ClassTracker.METHOD_ADDED, out.get(0).getEventType());
        assertEquals("bar(int)", out.get(0).getAttributes().get("method"));
        assertEquals(ClassTracker.METHOD_REMOVED, out.get(1).getEventType());
    }

    @Test
    void allMethodsOfANewClassAreReportedAdded() {
        ClassTracker t = new ClassTracker();
        t.baseline(Set.of());
        t.updateClasses(Set.of("B"));

        List<TelemetryRecord> out = t.updateMethods("B", Set.of("x()", "y()"));

        assertEquals(2, out.size());
        assertTrue(out.stream().allMatch(r -> ClassTracker.METHOD_ADDED.equals(r.getEventType())));
    }
}