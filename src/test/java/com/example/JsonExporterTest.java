package com.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonExporterTest {

    @Test
    void sessionMetadataComputesDuration() {
        Instant start = Instant.parse("2026-11-09T10:00:00Z");
        Instant end = Instant.parse("2026-11-09T10:30:00Z");

        SessionMetadata meta = new SessionMetadata("Demo", start, end, 4);

        assertEquals(1800, meta.getTotalSessionSeconds());
        assertEquals(4, meta.getTotalCompilationAttempts());
    }

    @Test
    void exportWritesMetadataSummaryAndEvents() throws Exception {
        TelemetryRecord r = new TelemetryRecord();
        r.setTimeStamp(Instant.parse("2026-11-09T10:05:00Z"));
        r.setCategory(CompileEventAdapter.CATEGORY);
        r.setEventType(CompileEventAdapter.COMPILE_START);
        List<TelemetryRecord> records = List.of(r);

        TelemetryAggregator stats = new TelemetryAggregator(records);
        SessionMetadata meta = new SessionMetadata("Demo",
                Instant.parse("2026-11-09T10:00:00Z"),
                Instant.parse("2026-11-09T10:30:00Z"),
                stats.compileAttempts());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonExporter().export(meta, stats, records, out);

        JsonObject json = JsonParser.parseString(out.toString(StandardCharsets.UTF_8)).getAsJsonObject();

        JsonObject metaJson = json.getAsJsonObject("sessionMetadata");

        assertEquals(SessionMetadata.STUDENT_ID_PLACEHOLDER, metaJson.get("studentId").getAsString());
        assertEquals(1, metaJson.get("totalCompilationAttempts").getAsInt());
        assertEquals(1, json.getAsJsonObject("summary").get("compileAttempts").getAsInt());
        assertEquals(1, json.getAsJsonArray("events").size());
        // Instant must be written as a readable ISO string
        assertEquals("2026-11-09T10:05:00Z",
                json.getAsJsonArray("events").get(0).getAsJsonObject().get("timestamp").getAsString());
    }
        @Test
    void jsonExporterUsesJsonExtension() {
        assertEquals("json", new JsonExporter().fileExtension());
    }
}