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

import bluej.extensions2.BClass;
import bluej.extensions2.BMethod;
import bluej.extensions2.BPackage;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ExtensionEvent;
import bluej.extensions2.event.ExtensionEventListener;
import bluej.extensions2.event.PackageEvent;

/** Bridge between BlueJ and the telemetry model: turns BlueJ events into records and exports them. */
public class TelemetryListener implements ExtensionEventListener {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault());

    /** Set to true to print every event and record to BlueJ's debug log. */
    private static final boolean DEBUG = false;

    private final List<TelemetryRecord> records = new ArrayList<>();
    private final EventAdapterFactory factory = new EventAdapterFactory();
    // Strategy list: add new PdfExporter() here and a .pdf is written next to the .json
    private final List<TelemetryExporter> exporters = List.of(new JsonExporter());
    private final ClassTracker classTracker = new ClassTracker();
    private final EditTracker editTracker = new EditTracker();
    private final Set<String> watchedEditors = new HashSet<>();

    private Instant sessionStart = Instant.now();
    private String projectName = "unknown";
    private Path outputDir = Path.of(System.getProperty("user.home"), "bluej-telemetry"); // fallback
    private boolean projectReady = false; // ignore class events until the project is baselined

    private static void debug(String message) {
        if (DEBUG) {
            System.out.println(message);
        }
    }

    @Override
    public synchronized void eventOccurred(ExtensionEvent event) {
        debug("RECEIVED EVENT: " + event.getClass().getName());

        if (event instanceof PackageEvent pe && pe.getEventType() == PackageEvent.EventType.PACKAGE_OPENED) {
            rememberProject(pe.getPackage());
        }

        EventAdapter adapter = factory.adapterFor(event);
        if (adapter == null) {
            debug("Unsupported event: " + event.getClass().getName());
            return;
        }

        TelemetryRecord record = adapter.adapt(event);
        if (record != null && CompileEventAdapter.COMPILE_START.equals(record.getEventType())) {
            addAll(editTracker.flush()); // close the typing burst before the compile
        }
        if (record != null) {
            addAll(List.of(record));
        }

        if (event instanceof ClassEvent ce) {
            trackLifecycle(ce);
        }

        if (event instanceof PackageEvent pe && pe.getEventType() == PackageEvent.EventType.PACKAGE_CLOSING) {
            projectReady = false;
            watchedEditors.clear();
            exportNow();
        }
    }

    // ---------- export ----------

    /** End of session: writes all files, then starts a fresh session. */
    public synchronized List<Path> exportNow() {
        return export(true);
    }

    /** Manual export: writes all files but keeps the session going. */
    public synchronized List<Path> exportSnapshot() {
        return export(false);
    }

    private List<Path> export(boolean endSession) {
        addAll(editTracker.flush());
        if (records.isEmpty()) {
            return List.of();
        }
        Instant end = Instant.now();
        TelemetryAggregator stats = new TelemetryAggregator(records);
        SessionMetadata meta = new SessionMetadata(projectName, sessionStart, end, stats.compileAttempts());
        String baseName = "telemetry-" + FILE_TIME.format(end);

        List<Path> written = new ArrayList<>();
        for (TelemetryExporter exporter : exporters) {
            Path file = outputDir.resolve(baseName + "." + exporter.fileExtension());
            try {
                Files.createDirectories(outputDir);
                try (OutputStream out = Files.newOutputStream(file)) {
                    exporter.export(meta, stats, records, out);
                }
                System.out.println("TELEMETRY EXPORTED: " + file);
                written.add(file);
            } catch (IOException e) {
                System.out.println("TELEMETRY EXPORT FAILED (" + exporter.fileExtension() + "): " + e.getMessage());
            }
        }
        if (endSession && !written.isEmpty()) {
            records.clear();
            sessionStart = Instant.now();
        }
        return written;
    }

    public synchronized List<TelemetryRecord> getRecords() {
        return new ArrayList<>(records);
    }

    // ---------- project, lifecycle and typing ----------

    private void rememberProject(BPackage pkg) {
        try {
            projectName = pkg.getProject().getName();
            outputDir = pkg.getProject().getDir().toPath().resolve("telemetry");
            classTracker.baseline(baselineNames(pkg));
            watchEditors(pkg);
            projectReady = true;
        } catch (Exception e) {
            System.out.println("Could not read project info: " + e.getMessage());
        }
    }

    /** Adds CLASS_CREATED / CLASS_DELETED / METHOD_ADDED / METHOD_REMOVED records. */
    private void trackLifecycle(ClassEvent ce) {
        if (!projectReady) {
            return;
        }
        try {
            addAll(classTracker.updateClasses(classNames(ce.getPackage())));
            watchEditors(ce.getPackage()); // new classes get a typing listener too
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
            editor.addDocumentListener((start, replaced, replacement, linesRemoved, linesAdded) ->
                    onTextReplaced(name, replaced, replacement, linesRemoved, linesAdded));
            watchedEditors.add(name);
        }
    }

    private synchronized void onTextReplaced(String cls, String replaced, String replacement,
                                             int linesRemoved, int linesAdded) {
        int inserted = replacement == null ? 0 : replacement.length();
        int deleted = replaced == null ? 0 : replaced.length();
        addAll(editTracker.onEdit(cls, Instant.now(), inserted, deleted, linesAdded, linesRemoved));
    }

    private void addAll(List<TelemetryRecord> found) {
        for (TelemetryRecord r : found) {
            records.add(r);
            debug("RECORD: " + r.getCategory() + " " + r.getEventType() + " " + r.getAttributes());
        }
    }

    private static Set<String> classNames(BPackage pkg) throws Exception {
        Set<String> names = new HashSet<>();
        for (BClass c : pkg.getClasses()) {
            names.add(c.getName());
        }
        return names;
    }

    /** Classes that exist at open: BlueJ's list plus every .java file in the folder. */
    private static Set<String> baselineNames(BPackage pkg) throws Exception {
        Set<String> names = classNames(pkg);
        try (var files = Files.list(pkg.getDir().toPath())) {
            files.map(p -> p.getFileName().toString())
                 .filter(n -> n.endsWith(".java"))
                 .map(n -> n.substring(0, n.length() - ".java".length()))
                 .forEach(names::add);
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
}