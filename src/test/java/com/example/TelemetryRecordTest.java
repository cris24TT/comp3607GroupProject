package com.example;
 
import org.junit.jupiter.api.Test;
 
import java.time.Instant;
import java.util.List;
import java.util.Map;
 
import static org.junit.jupiter.api.Assertions.*;
 
class TelemetryRecordTest {
 
    @Test
    void storesBasicFields() {
        TelemetryRecord r = new TelemetryRecord();
        Instant t = Instant.parse("2026-11-09T20:00:00Z");
        r.setTimeStamp(t);
        r.setCategory("COMPILE");
        r.setEventType("COMPILE_START");
        r.setDetails("details");
 
        assertEquals(t, r.getTimestamp());
        assertEquals("COMPILE", r.getCategory());
        assertEquals("COMPILE_START", r.getEventType());
        assertEquals("details", r.getDetails());
        assertTrue(r.getAttributes().isEmpty());
    }
 
    @Test
    void nullAttributeKeyThrows() {
        TelemetryRecord r = new TelemetryRecord();
        assertThrows(IllegalArgumentException.class, () -> r.addAttribute(null, "x"));
    }
 
    @Test
    void nullAttributeValueIsSkipped() {
        TelemetryRecord r = new TelemetryRecord();
        r.addAttribute("methodName", null);
        assertFalse(r.getAttributes().containsKey("methodName"));
    }
 
    @Test
    void attributesKeepInsertionOrderAndAreReadOnly() {
        TelemetryRecord r = new TelemetryRecord();
        r.addAttribute("b", 2);
        r.addAttribute("a", 1);
 
        Map<String, Object> attrs = r.getAttributes();
        assertEquals(List.of("b", "a"), List.copyOf(attrs.keySet()));
        assertThrows(UnsupportedOperationException.class, () -> attrs.put("c", 3));
    }
}
