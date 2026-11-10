package com.example;

import java.io.IOException;
import java.io.OutputStream;
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
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.event.CompileEvent;
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
    private final List<TelemetryExporter> exporters = List.of(new JsonExporter());
    private final ClassTracker classTracker = new ClassTracker();
    private boolean projectReady = false; // ignore class events until the project is baselined
    private final EditTracker editTracker = new EditTracker();
    private final Set<String> watchedEditors = new HashSet<>();

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
        if (record != null && CompileEventAdapter.COMPILE_START.equals(record.getEventType())) {
            addAll(editTracker.flush()); // close the typing burst before the compile
        }
        if (record != null) {
            records.add(record);
            System.out.println(
                    "RECORD: " + record.getCategory() + " " + record.getEventType() + " " + record.getAttributes());
        }

        if (event instanceof ClassEvent ce) {
            trackLifecycle(ce);
        }

        if (event instanceof PackageEvent pe && pe.getEventType() == PackageEvent.EventType.PACKAGE_CLOSING) {
            projectReady = false;
            exportNow();
        }
    }

    private void rememberProject(BPackage pkg) {
        try {
            projectName = pkg.getProject().getName();
            outputDir = pkg.getProject().getDir().toPath().resolve("telemetry");
            classTracker.baseline(classNames(pkg));
            watchEditors(pkg);
            projectReady = true;
        } catch (Exception e) {
            System.out.println("Could not read project info: " + e.getMessage());
        }
    }

    /**
     * Adds CLASS_CREATED / CLASS_DELETED / METHOD_ADDED / METHOD_REMOVED records.
     */
    /**
     * Adds CLASS_CREATED / CLASS_DELETED / METHOD_ADDED / METHOD_REMOVED records.
     */
    private void trackLifecycle(ClassEvent ce) {
        if (!projectReady) {
            return;
        }
        try {
            addAll(classTracker.updateClasses(classNames(ce.getPackage())));
            watchEditors(ce.getPackage()); // new classes get a listener too
        } catch (Exception e) {
            System.out.println("Class tracking failed: " + e);
        }
        if (ce.getEventType() == ClassEvent.EventType.STATE_CHANGED && ce.isClassCompiled()) {
            try {
                BClass bc = ce.getBClass();
                addAll(classTracker.updateMethods(bc.getName(), methodSignatures(bc)));
            } catch (Exception e) {
                System.out.println("Method tracking failed: " + e);
            }
        }
    }

    private void addAll(List<TelemetryRecord> found) {
        for (TelemetryRecord r : found) {
            records.add(r);
            System.out.println("RECORD: " + r.getCategory() + " " + r.getEventType() + " " + r.getAttributes());
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
    /** Writes the session with every exporter, then starts a fresh session. */
    public synchronized void exportNow() {
        addAll(editTracker.flush());
        if (records.isEmpty()) {
            return;
        }
        Instant end = Instant.now();
        TelemetryAggregator stats = new TelemetryAggregator(records);
        SessionMetadata meta = new SessionMetadata(projectName, sessionStart, end, stats.compileAttempts());
        String baseName = "telemetry-" + FILE_TIME.format(end);

        boolean anyWritten = false;
        for (TelemetryExporter exporter : exporters) {
            Path file = outputDir.resolve(baseName + "." + exporter.fileExtension());
            try {
                Files.createDirectories(outputDir);
                try (OutputStream out = Files.newOutputStream(file)) {
                    exporter.export(meta, stats, records, out);
                }
                System.out.println("TELEMETRY EXPORTED: " + file);
                anyWritten = true;
            } catch (IOException e) {
                System.out.println("TELEMETRY EXPORT FAILED (" + exporter.fileExtension() + "): " + e.getMessage());
            }
        }
        if (anyWritten) {
            records.clear();
            sessionStart = Instant.now();
        }
    }

    /** Attaches a typing listener to every class editor not yet watched. */
    private void watchEditors(BPackage pkg) throws Exception {
        for (BClass c : pkg.getClasses()) {
            String name = c.getName();
            if (watchedEditors.contains(name)) {
                continue;
            }
            JavaEditor editor = c.getJavaEditor();
            if (editor == null) {
                continue; // not a Java class (e.g. Stride)
            }
            editor.addDocumentListener((start, replaced, replacement, linesRemoved, linesAdded) -> onTextReplaced(name,
                    replaced, replacement, linesRemoved, linesAdded));
            watchedEditors.add(name);
        }
    }

    private synchronized void onTextReplaced(String cls, String replaced, String replacement,
            int linesRemoved, int linesAdded) {
        int inserted = replacement == null ? 0 : replacement.length();
        int deleted = replaced == null ? 0 : replaced.length();
        addAll(editTracker.onEdit(cls, Instant.now(), inserted, deleted, linesAdded, linesRemoved));
    }
}