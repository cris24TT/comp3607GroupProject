package com.example;
 
import org.junit.jupiter.api.Test;
 
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
 
import static org.junit.jupiter.api.Assertions.*;
 
class ClassEventAdapterTest {
 
    private static final Instant NOW = Instant.parse("2026-11-09T20:00:00Z");
    private final ClassEventAdapter adapter =
            new ClassEventAdapter(Clock.fixed(NOW, ZoneOffset.UTC));
 
    @Test
    void renameRecordsOldAndNewName() {
        TelemetryRecord r = adapter.build(ClassEventAdapter.CLASS_RENAMED, "Circle", "Shape", false);
 
        assertEquals(NOW, r.getTimestamp());
        assertEquals(ClassEventAdapter.CATEGORY, r.getCategory());
        assertEquals(ClassEventAdapter.CLASS_RENAMED, r.getEventType());
        Map<String, Object> a = r.getAttributes();
        assertEquals("Circle", a.get("className"));
        assertEquals("Shape", a.get("oldName"));
        assertEquals(false, a.get("hasError"));
    }
 
    @Test
    void modifiedRecordSkipsMissingOldName() {
        TelemetryRecord r = adapter.build(ClassEventAdapter.CLASS_MODIFIED, "Shape", null, true);
 
        assertFalse(r.getAttributes().containsKey("oldName"));
        assertEquals(true, r.getAttributes().get("hasError"));
    }
 
    @Test
    void defaultConstructorWorks() {
        TelemetryRecord r = new ClassEventAdapter().build(ClassEventAdapter.CLASS_REMOVED, "Shape", null, false);
        assertNotNull(r.getTimestamp());
    }
}
