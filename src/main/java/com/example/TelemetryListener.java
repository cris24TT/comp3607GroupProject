package com.example;

import bluej.extensions2.BClass;
import bluej.extensions2.BMethod;
import bluej.extensions2.BPackage;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ExtensionEvent;
import bluej.extensions2.event.ExtensionEventListener;
import bluej.extensions2.event.PackageEvent;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Receives every event from BlueJ, turns it into records and exports them.
// This is the only class (with SimpleExtension and TelemetryMenu) that needs BlueJ running.
public class TelemetryListener implements ExtensionEventListener {

    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private final EventAdapterFactory factory = new EventAdapterFactory();
    private final ClassTracker classTracker = new ClassTracker();
    private final EditTracker editTracker = new EditTracker();

    // Strategy pattern: every exporter in this list writes its own file (JSON and PDF).
    private final List<TelemetryExporter> exporters = List.of(new JsonExporter(), new PdfExporter());

    private List<TelemetryRecord> records = new ArrayList<>();
    private Set<String> watchedEditors = new HashSet<>();        // classes we already listen to for typing
    private Map<String, BClass> methodsToCheck = new HashMap<>(); // compiled classes whose methods we still need to read

    private Instant sessionStart = Instant.now();
    private String projectName = "unknown";
    private Path outputDir = Path.of(System.getProperty("user.home"), "bluej-telemetry"); // used until a project opens
    private boolean projectReady = false; // class events are ignored until the project is open

    @Override
    public synchronized void eventOccurred(ExtensionEvent event) {
        checkMethods(); // classes compiled earlier may be readable now

        if (isPackageEvent(event, PackageEvent.EventType.PACKAGE_OPENED)) {
            rememberProject(((PackageEvent) event).getPackage());
        }

        // Factory pattern: the factory picks the adapter for this kind of event
        EventAdapter adapter = factory.adapterFor(event);
        if (adapter == null) {
            return; // we don't record this kind of event
        }

        TelemetryRecord record = adapter.adapt(event);
        if (record != null) {
            if (record.getEventType().equals(CompileEventAdapter.COMPILE_START)) {
                records.addAll(editTracker.flush()); // finish the typing burst before the compile
            }
            records.add(record);
        }

        if (event instanceof ClassEvent) {
            trackClassesAndMethods((ClassEvent) event);
        }

        if (isPackageEvent(event, PackageEvent.EventType.PACKAGE_CLOSING)) {
            projectReady = false;
            exportNow();
            watchedEditors.clear();
            methodsToCheck.clear();
        }
    }

    private static boolean isPackageEvent(ExtensionEvent event, PackageEvent.EventType type) {
        return event instanceof PackageEvent && ((PackageEvent) event).getEventType() == type;
    }

    // Export

    // When the project closes: write the files, then start a new session.
    public synchronized List<Path> exportNow() {
        return export(true);
    }

    // Tools > Export telemetry now: write the files but keep recording.
    public synchronized List<Path> exportSnapshot() {
        return export(false);
    }

    private List<Path> export(boolean endSession) {
        checkMethods();
        records.addAll(editTracker.flush());
        if (records.isEmpty()) {
            return new ArrayList<>();
        }

        Instant end = Instant.now();
        TelemetryAggregator stats = new TelemetryAggregator(records);
        SessionMetadata meta = new SessionMetadata(projectName, sessionStart, end, stats.compileAttempts());
        String fileName = "telemetry-" + FILE_TIME.format(end);

        List<Path> written = new ArrayList<>();
        for (TelemetryExporter exporter : exporters) {
            Path file = outputDir.resolve(fileName + "." + exporter.fileExtension());
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
            records = new ArrayList<>();
            sessionStart = Instant.now();
        }
        return written;
    }

    public synchronized List<TelemetryRecord> getRecords() {
        return new ArrayList<>(records);
    }

    // Project, classes and typing

    private void rememberProject(BPackage pkg) {
        try {
            projectName = pkg.getProject().getName();
            outputDir = pkg.getProject().getDir().toPath().resolve("telemetry");
            classTracker.baseline(classesAtOpen(pkg));
            watchEditors(pkg);
            projectReady = true;
        } catch (Exception e) {
            System.out.println("Could not read project info: " + e.getMessage());
        }
    }

    // BlueJ has no "class created" or "method added" event, so ClassTracker compares lists for us.
    private void trackClassesAndMethods(ClassEvent ce) {
        if (!projectReady) {
            return;
        }
        try {
            Set<String> names = classNames(ce.getPackage());
            records.addAll(classTracker.updateClasses(names));
            methodsToCheck.keySet().retainAll(names); // forget classes that were deleted
            watchEditors(ce.getPackage());            // new classes get a typing listener too
        } catch (Exception e) {
            System.out.println("Class tracking failed: " + e);
        }

        if (ce.getEventType() == ClassEvent.EventType.STATE_CHANGED && ce.isClassCompiled()) {
            try {
                BClass compiled = ce.getBClass();
                methodsToCheck.put(compiled.getName(), compiled);
            } catch (Exception e) {
                System.out.println("Method tracking failed: " + e);
            }
            checkMethods();
        }
    }

    // Right after a compile, BlueJ may not have loaded the new class yet, so reading its
    // methods throws ClassNotFoundException. We keep the class in methodsToCheck and try
    // again on the next event (or at export) until it works.
    private void checkMethods() {
        for (String name : new ArrayList<>(methodsToCheck.keySet())) {
            try {
                Set<String> methods = methodSignatures(methodsToCheck.get(name));
                records.addAll(classTracker.updateMethods(name, methods));
                methodsToCheck.remove(name);
            } catch (Exception e) {
                // not loaded yet, try again next time
            }
        }
    }

    // Adds a typing listener to every class editor we aren't listening to yet.
    private void watchEditors(BPackage pkg) throws Exception {
        for (BClass c : pkg.getClasses()) {
            String name = c.getName();
            if (watchedEditors.contains(name)) {
                continue; // already listening to this one
            }
            JavaEditor editor = c.getJavaEditor();
            if (editor == null) {
                continue; // not a Java class (e.g. Stride)
            }
            editor.addDocumentListener((start, replaced, replacement, linesRemoved, linesAdded) ->
                    onTyping(name, replaced, replacement, linesRemoved, linesAdded));
            watchedEditors.add(name);
        }
    }

    private synchronized void onTyping(String className, String replaced, String replacement,
                                       int linesRemoved, int linesAdded) {
        int inserted = (replacement == null) ? 0 : replacement.length();
        int deleted = (replaced == null) ? 0 : replaced.length();
        records.addAll(editTracker.onEdit(className, Instant.now(), inserted, deleted, linesAdded, linesRemoved));
    }

    private static Set<String> classNames(BPackage pkg) throws Exception {
        Set<String> names = new HashSet<>();
        for (BClass c : pkg.getClasses()) {
            names.add(c.getName());
        }
        return names;
    }

    // The classes that exist when the project opens: BlueJ's list plus every .java file in the folder.
    private static Set<String> classesAtOpen(BPackage pkg) throws Exception {
        Set<String> names = classNames(pkg);
        File[] files = pkg.getDir().listFiles();
        if (files != null) {
            for (File f : files) {
                String fileName = f.getName();
                if (fileName.endsWith(".java")) {
                    names.add(fileName.substring(0, fileName.length() - ".java".length()));
                }
            }
        }
        return names;
    }

    // e.g. "divide(int, int)"
    private static Set<String> methodSignatures(BClass bc) throws Exception {
        Set<String> signatures = new HashSet<>();
        for (BMethod m : bc.getDeclaredMethods()) {
            String params = "";
            for (Class<?> p : m.getParameterTypes()) {
                if (!params.isEmpty()) {
                    params += ", ";
                }
                params += p.getSimpleName();
            }
            signatures.add(m.getName() + "(" + params + ")");
        }
        return signatures;
    }
}
