package com.example;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import bluej.extensions2.BClass;
import bluej.extensions2.BMethod;
import bluej.extensions2.BPackage;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ExtensionEvent;
import bluej.extensions2.event.ExtensionEventListener;
import bluej.extensions2.event.PackageEvent;

public class TelemetryListener implements ExtensionEventListener {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault());

    private final List<TelemetryRecord> records = new ArrayList<>();
    private final EventAdapterFactory factory = new EventAdapterFactory();
    private final TelemetryExporter exporter = new JsonExporter();
    private final ClassTracker classTracker = new ClassTracker();

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
            System.out.println(
                    "RECORD: " + record.getCategory() + " " + record.getEventType() + " " + record.getAttributes());
        }

        if (event instanceof ClassEvent ce) {
            trackLifecycle(ce);
        }

        if (event instanceof PackageEvent pe && pe.getEventType() == PackageEvent.EventType.PACKAGE_CLOSING) {
            exportNow();
        }
    }

    private void rememberProject(BPackage pkg) {
        try {
            projectName = pkg.getProject().getName();
            outputDir = pkg.getProject().getDir().toPath().resolve("telemetry");
            classTracker.baseline(classNames(pkg));
        } catch (Exception e) {
            System.out.println("Could not read project info: " + e.getMessage());
        }
    }

    /** Adds CLASS_CREATED / CLASS_DELETED / METHOD_ADDED / METHOD_REMOVED records. */
    private void trackLifecycle(ClassEvent ce) {
        try {
            List<TelemetryRecord> found = new ArrayList<>(classTracker.updateClasses(classNames(ce.getPackage())));
            if (ce.getEventType() == ClassEvent.EventType.STATE_CHANGED && ce.isClassCompiled()) {
                BClass bc = ce.getBClass();
                found.addAll(classTracker.updateMethods(bc.getName(), methodSignatures(bc)));
            }
            for (TelemetryRecord r : found) {
                records.add(r);
                System.out.println("RECORD: " + r.getCategory() + " " + r.getEventType() + " " + r.getAttributes());
            }
        } catch (Exception e) {
            System.out.println("Lifecycle tracking failed: " + e);
        }
    }

    private static Set<String> classNames(BPackage pkg) throws Exception {
        Set<String> names = new HashSet<>();
        for (BClass c : pkg.getClasses()) {
            names.add(c.getName());
        }
        return names;
    }

    private static Set<String> methodSignatures(BClass bc) throws Exception {
        Set<String> sigs = new HashSet<>();
        for (BMethod m : bc.getDeclaredMethods()) {
            StringJoiner params = new StringJoiner(", ", "(", ")");
            for (Class<?> p : m.getParameterTypes()) {
                params.add(p.getSimpleName());
            }
            sigs.add(m.getName() + params);
        }
        return sigs;
    }

    /**
     * Writes all records collected so far to a JSON file, then starts a fresh
     * session.
     */
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