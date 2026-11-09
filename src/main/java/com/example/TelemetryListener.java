package com.example;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import bluej.extensions2.BPackage;
import bluej.extensions2.event.ExtensionEvent;
import bluej.extensions2.event.ExtensionEventListener;
import bluej.extensions2.event.PackageEvent;

public class TelemetryListener implements ExtensionEventListener {

    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private final List<TelemetryRecord> records = new ArrayList<>();
    private final EventAdapterFactory factory = new EventAdapterFactory();
    private final TelemetryExporter exporter = new JsonExporter();

    private Instant sessionStart = Instant.now();
    private String projectName = "unknown";
    private Path outputDir = Path.of(System.getProperty("user.home"), "bluej-telemetry"); // fallback

    @Override
    public synchronized void eventOccurred(ExtensionEvent event) {
        System.out.println("RECEIVED EVENT: " + event.getClass().getName());

        if (event instanceof PackageEvent pe && pe.getEventType() == PackageEvent.EventType.PACKAGE_OPENED) {
            rememberProject(pe.getPackage());
        }

        EventAdapter adapter = factory.adapterFor(event);
        if (adapter == null) {
            System.out.println("Unsupported event: " + event.getClass().getName());
            return;
        }

        TelemetryRecord record = adapter.adapt(event);
        if (record != null) {
            records.add(record);
            System.out.println("RECORD: " + record.getCategory() + " " + record.getEventType() + " " + record.getAttributes());
        }

        if (event instanceof PackageEvent pe && pe.getEventType() == PackageEvent.EventType.PACKAGE_CLOSING) {
            exportNow();
        }
    }

    private void rememberProject(BPackage pkg) {
        try {
            projectName = pkg.getProject().getName();
            outputDir = pkg.getProject().getDir().toPath().resolve("telemetry");
        } catch (Exception e) {
            System.out.println("Could not read project info: " + e.getMessage());
        }
    }

    /** Writes all records collected so far to a JSON file, then starts a fresh session. */
    public synchronized void exportNow() {
        if (records.isEmpty()) {
            return;
        }
        Instant end = Instant.now();
        TelemetryAggregator stats = new TelemetryAggregator(records);
        SessionMetadata meta = new SessionMetadata(projectName, sessionStart, end, stats.compileAttempts());

        Path file = outputDir.resolve("telemetry-" + FILE_TIME.format(end) + ".json");
        try {
            Files.createDirectories(outputDir);
            try (Writer out = Files.newBufferedWriter(file)) {
                exporter.export(meta, stats, records, out);
            }
            System.out.println("TELEMETRY EXPORTED: " + file);
            records.clear();
            sessionStart = Instant.now();
        } catch (IOException e) {
            System.out.println("TELEMETRY EXPORT FAILED: " + e.getMessage());
        }
    }

    public synchronized List<TelemetryRecord> getRecords() {
        return new ArrayList<>(records);
    }
}