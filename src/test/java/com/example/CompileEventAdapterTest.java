package com.example;
 
import org.junit.jupiter.api.Test;
 
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
 
import static org.junit.jupiter.api.Assertions.*;
 
class CompileEventAdapterTest {
 
    private static final Instant NOW = Instant.parse("2026-11-09T20:00:00Z");
    private final CompileEventAdapter adapter =
            new CompileEventAdapter(Clock.fixed(NOW, ZoneOffset.UTC));
 
    @Test
    void errorRecordHasLineMessageAndErrorType() {
        TelemetryRecord r = adapter.build(CompileEventAdapter.COMPILE_ERROR,
                List.of("Shape.java"), 7, "cannot find symbol\n  symbol: variable x");
 
        assertEquals(NOW, r.getTimestamp());
        assertEquals(CompileEventAdapter.CATEGORY, r.getCategory());
        assertEquals(CompileEventAdapter.COMPILE_ERROR, r.getEventType());
 
        Map<String, Object> a = r.getAttributes();
        assertEquals(List.of("Shape.java"), a.get("files"));
        assertEquals(7, a.get("line"));
        assertEquals("cannot find symbol\n  symbol: variable x", a.get("message"));
        assertEquals("cannot find symbol", a.get("errorType"));
    }
 
    @Test
    void warningRecordAlsoHasErrorDetails() {
        TelemetryRecord r = adapter.build(CompileEventAdapter.COMPILE_WARNING,
                List.of("A.java"), 3, "unchecked call");
 
        assertEquals(3, r.getAttributes().get("line"));
        assertEquals("unchecked call", r.getAttributes().get("errorType"));
    }
 
    @Test
    void successRecordHasFilesOnly() {
        TelemetryRecord r = adapter.build(CompileEventAdapter.COMPILE_SUCCESS,
                List.of("A.java", "B.java"), 0, null);
 
        Map<String, Object> a = r.getAttributes();
        assertEquals(List.of("A.java", "B.java"), a.get("files"));
        assertFalse(a.containsKey("line"));
        assertFalse(a.containsKey("message"));
        assertFalse(a.containsKey("errorType"));
    }
 
    @Test
    void errorWithoutMessageIsUnknownType() {
        TelemetryRecord r = adapter.build(CompileEventAdapter.COMPILE_ERROR,
                List.of(), 1, null);
 
        assertFalse(r.getAttributes().containsKey("message"));
        assertEquals("unknown", r.getAttributes().get("errorType"));
    }
 
    @Test
    void errorTypeUsesFirstLineOfMessage() {
        assertEquals("unknown", CompileEventAdapter.errorType(null));
        assertEquals("unknown", CompileEventAdapter.errorType("   "));
        assertEquals("';' expected", CompileEventAdapter.errorType("';' expected"));
        assertEquals("incompatible types", CompileEventAdapter.errorType("  incompatible types  \r\nmore"));
    }
 
    @Test
    void defaultConstructorWorks() {
        TelemetryRecord r = new CompileEventAdapter().build(CompileEventAdapter.COMPILE_START,
                List.of(), 0, null);
        assertNotNull(r.getTimestamp());
    }
}
