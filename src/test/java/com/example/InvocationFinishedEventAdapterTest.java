package com.example;
 
import org.junit.jupiter.api.Test;
 
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
 
import static org.junit.jupiter.api.Assertions.*;
 
class InvocationFinishedEventAdapterTest {
 
    private static final Instant NOW = Instant.parse("2026-11-09T20:00:00Z");
    private final InvocationFinishedEventAdapter adapter =
            new InvocationFinishedEventAdapter(Clock.fixed(NOW, ZoneOffset.UTC));
 
    @Test
    void normalMethodRunRecordsResultSummary() {
        TelemetryRecord r = adapter.build(InvocationFinishedEventAdapter.RUN_NORMAL,
                "RunTest", "square", "runTest1", List.of("3"), 9);
 
        assertEquals(NOW, r.getTimestamp());
        assertEquals(InvocationFinishedEventAdapter.CATEGORY, r.getCategory());
        Map<String, Object> a = r.getAttributes();
        assertEquals("RunTest", a.get("className"));
        assertEquals("method", a.get("invocationKind"));
        assertEquals("square", a.get("methodName"));
        assertEquals("runTest1", a.get("objectName"));
        assertEquals(List.of("3"), a.get("parameters"));
        assertEquals(false, a.get("exception"));
        assertEquals("9", a.get("resultSummary"));
    }
 
    @Test
    void voidMethodSummaryIsVoid() {
        TelemetryRecord r = adapter.build(InvocationFinishedEventAdapter.RUN_NORMAL,
                "RunTest", "sayHi", "runTest1", List.of(), null);
 
        assertEquals("void", r.getAttributes().get("resultSummary"));
    }
 
    @Test
    void constructorRunHasNoResultSummary() {
        TelemetryRecord r = adapter.build(InvocationFinishedEventAdapter.RUN_NORMAL,
                "RunTest", null, null, List.of(), null);
 
        Map<String, Object> a = r.getAttributes();
        assertEquals("constructor", a.get("invocationKind"));
        assertFalse(a.containsKey("methodName"));
        assertFalse(a.containsKey("resultSummary"));
    }
 
    @Test
    void exceptionRunIsFlagged() {
        TelemetryRecord r = adapter.build(InvocationFinishedEventAdapter.RUN_EXCEPTION,
                "RunTest", null, "runTest1", List.of(), null);
 
        Map<String, Object> a = r.getAttributes();
        assertEquals(InvocationFinishedEventAdapter.RUN_EXCEPTION, r.getEventType());
        assertEquals("method", a.get("invocationKind"));
        assertEquals(true, a.get("exception"));
        assertFalse(a.containsKey("resultSummary"));
    }
 
    @Test
    void summarizeTruncatesLongResults() {
        String exact = "x".repeat(InvocationFinishedEventAdapter.MAX_RESULT_LENGTH);
        String longer = exact + "yyy";
 
        assertEquals("void", InvocationFinishedEventAdapter.summarize(null));
        assertEquals("42", InvocationFinishedEventAdapter.summarize(42));
        assertEquals(exact, InvocationFinishedEventAdapter.summarize(exact));
        assertEquals(exact + "...", InvocationFinishedEventAdapter.summarize(longer));
    }
 
    @Test
    void defaultConstructorWorks() {
        TelemetryRecord r = new InvocationFinishedEventAdapter().build(
                InvocationFinishedEventAdapter.RUN_TERMINATED, "RunTest", null, "runTest1", List.of(), null);
        assertNotNull(r.getTimestamp());
    }
}
