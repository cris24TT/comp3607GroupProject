package com.example;

import com.google.gson.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonExporter implements TelemetryExporter {

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            // Gson can't read Instant's private fields on Java 21, so write it as an ISO string
            .registerTypeAdapter(Instant.class,
                    (JsonSerializer<Instant>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
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

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("sessionMetadata", metadata);
        root.put("summary", summary);
        root.put("events", records);

        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        gson.toJson(root, writer);
        writer.flush(); // flush, but don't close: the caller owns the stream
    }
}