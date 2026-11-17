package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Writes the session as JSON: sessionMetadata, then summary, then every event.
public class JsonExporter implements TelemetryExporter {

    // Gson can't turn an Instant into JSON on Java 21 by itself,
    // so we tell it to write each Instant as text like "2026-11-09T14:00:00Z".
    private final JsonSerializer<Instant> instantAsText =
            (time, type, context) -> new JsonPrimitive(time.toString());

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Instant.class, instantAsText)
            .create();

    @Override
    public String fileExtension() {
        return "json";
    }

    @Override
    public void export(SessionMetadata metadata, TelemetryAggregator stats,
                       List<TelemetryRecord> records, OutputStream out) throws IOException {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("compileAttempts", stats.compileAttempts());
        summary.put("compileSuccessRate", stats.compileSuccessRate());
        summary.put("compileErrorCount", stats.compileErrorCount());
        summary.put("errorsPerCompileAttempt", stats.errorsPerCompileAttempt());
        summary.put("errorFrequency", stats.errorFrequency());
        summary.put("runCount", stats.runCount());
        summary.put("exceptionCount", stats.exceptionCount());
        summary.put("activeSeconds", stats.activeTime().getSeconds());
        summary.put("idleSeconds", stats.idleTime().getSeconds());
        summary.put("idleThresholdSeconds", TelemetryAggregator.DEFAULT_IDLE_THRESHOLD.getSeconds());
        summary.put("creationTimeline", stats.creationTimeline());
        summary.put("editBursts", stats.editBurstCount());
        summary.put("totalEdits", stats.totalEdits());
        summary.put("typingCadenceMs", stats.typingCadenceMs());
        summary.put("editsPerActiveMinute", stats.editsPerActiveMinute());

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("sessionMetadata", metadata);
        json.put("summary", summary);
        json.put("events", records);

        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        gson.toJson(json, writer);
        writer.flush(); // flush but don't close: TelemetryListener closes the stream
    }
}
