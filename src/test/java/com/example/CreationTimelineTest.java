package com.example;
 
import org.junit.jupiter.api.Test;
 
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
 
import static org.junit.jupiter.api.Assertions.*;
 
class CreationTimelineTest {
 
    private static final Instant T0 = Instant.parse("2026-11-09T20:00:00Z");
 
    @Test
    void timelineListsCreationsAndAddedMethodsInTimeOrder() {
        ClassTracker later = new ClassTracker(Clock.fixed(T0.plusSeconds(60), ZoneOffset.UTC));
        later.baseline(Set.of());
        List<TelemetryRecord> records = new ArrayList<>(later.updateClasses(Set.of("Shape")));
        records.addAll(later.updateMethods("Shape", Set.of("area()")));
 
        ClassTracker earlier = new ClassTracker(Clock.fixed(T0, ZoneOffset.UTC));
        earlier.baseline(Set.of());
        records.addAll(earlier.updateClasses(Set.of("Circle")));
 
        TelemetryRecord compile = new TelemetryRecord();
        compile.setTimeStamp(T0.plusSeconds(30));
        compile.setEventType(CompileEventAdapter.COMPILE_START);
        records.add(compile);
 
        List<Map<String, Object>> timeline = new TelemetryAggregator(records).creationTimeline();
 
        assertEquals(3, timeline.size());
        assertEquals("Circle", timeline.get(0).get("className"));
        assertEquals(T0.toString(), timeline.get(0).get("time"));
        assertEquals(ClassTracker.CLASS_CREATED, timeline.get(1).get("event"));
        assertEquals("Shape", timeline.get(1).get("className"));
        assertFalse(timeline.get(1).containsKey("method"));
        assertEquals(ClassTracker.METHOD_ADDED, timeline.get(2).get("event"));
        assertEquals("area()", timeline.get(2).get("method"));
    }
 
    @Test
    void timelineIgnoresDeletionsAndHandlesMissingTimestamps() {
        TelemetryRecord deleted = new TelemetryRecord();
        deleted.setEventType(ClassTracker.CLASS_DELETED);
        TelemetryRecord created = new TelemetryRecord();
        created.setEventType(ClassTracker.CLASS_CREATED);
        created.addAttribute("className", "Shape");
 
        List<Map<String, Object>> timeline =
                new TelemetryAggregator(List.of(deleted, created)).creationTimeline();
 
        assertEquals(1, timeline.size());
        assertNull(timeline.get(0).get("time"));
        assertTrue(new TelemetryAggregator(List.of()).creationTimeline().isEmpty());
    }
}
