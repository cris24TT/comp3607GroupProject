package com.example;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Strategy pattern: writes the session as a PDF report instead of JSON.
public class PdfExporter implements TelemetryExporter {

    private final DateTimeFormatter timeFormat;

    public PdfExporter() {
        this(ZoneId.systemDefault());
    }

    // The time zone can be set so the tests always get the same times.
    public PdfExporter(ZoneId zone) {
        timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone);
    }

    @Override
    public String fileExtension() {
        return "pdf";
    }

    @Override
    public void export(SessionMetadata meta, TelemetryAggregator stats,
                       List<TelemetryRecord> records, OutputStream out) throws IOException {
        PdfDocument pdf = new PdfDocument();
        int[] twoColumns = {300, 195};

        pdf.addTitle("BlueJ Telemetry Report");
        List<String[]> session = new ArrayList<>();
        session.add(new String[] {"Student ID", meta.getStudentId()});
        session.add(new String[] {"Project", meta.getProjectName()});
        session.add(new String[] {"Session start", formatTime(meta.getSessionStart())});
        session.add(new String[] {"Session end", formatTime(meta.getSessionEnd())});
        session.add(new String[] {"Total session duration", duration(Duration.ofSeconds(meta.getTotalSessionSeconds()))});
        session.add(new String[] {"Events recorded", String.valueOf(records.size())});
        pdf.addTable(new String[] {"Measure", "Value"}, session, twoColumns);

        pdf.addHeading("Active coding time vs idle time");
        Duration active = stats.activeTime();
        Duration idle = stats.idleTime();
        pdf.addBarChart(new String[] {"Active", "Idle"},
                new double[] {active.getSeconds(), idle.getSeconds()},
                new String[] {duration(active), duration(idle)});
        pdf.addText("Idle time is the sum of gaps longer than "
                + TelemetryAggregator.DEFAULT_IDLE_THRESHOLD.toMinutes() + " minutes between recorded events.");

        pdf.addHeading("Compilation");
        List<String[]> compiling = new ArrayList<>();
        compiling.add(new String[] {"Compile attempts", String.valueOf(stats.compileAttempts())});
        compiling.add(new String[] {"Successful compiles", String.valueOf(stats.compileSuccesses())});
        compiling.add(new String[] {"Failed compiles", String.valueOf(stats.compileFailures())});
        compiling.add(new String[] {"Compile success rate", percent(stats.compileSuccessRate())});
        compiling.add(new String[] {"Compile errors", String.valueOf(stats.compileErrorCount())});
        compiling.add(new String[] {"Errors per attempt", decimal(stats.errorsPerCompileAttempt())});
        pdf.addTable(new String[] {"Measure", "Value"}, compiling, twoColumns);

        pdf.addHeading("Compilation error frequency");
        addErrorChart(pdf, stats.errorFrequency());

        pdf.addHeading("Program runs and editing");
        List<String[]> running = new ArrayList<>();
        running.add(new String[] {"Program runs", String.valueOf(stats.runCount())});
        running.add(new String[] {"Runtime exceptions", String.valueOf(stats.exceptionCount())});
        running.add(new String[] {"Edit bursts", String.valueOf(stats.editBurstCount())});
        running.add(new String[] {"Total edits", String.valueOf(stats.totalEdits())});
        running.add(new String[] {"Typing cadence (ms between edits)", decimal(stats.typingCadenceMs())});
        running.add(new String[] {"Edits per active minute", decimal(stats.editsPerActiveMinute())});
        pdf.addTable(new String[] {"Measure", "Value"}, running, twoColumns);

        pdf.addHeading("Progression timeline: classes and methods");
        List<String[]> timeline = new ArrayList<>();
        for (Map<String, Object> entry : stats.creationTimeline()) {
            Object name = entry.get("className");
            if (name == null) {
                name = entry.get("projectName"); // PROJECT_CREATED has a project name instead
            }
            timeline.add(new String[] {
                    formatTime(entry.get("time")),
                    text(entry.get("event")),
                    text(name),
                    text(entry.get("method"))});
        }
        pdf.addTable(new String[] {"Time", "Event", "Class / project", "Method"}, timeline,
                new int[] {135, 115, 120, 125});

        pdf.save(out);
        out.flush(); // TelemetryListener closes the stream
    }

    // Bar chart of error types, most common first.
    private void addErrorChart(PdfDocument pdf, Map<String, Long> frequency) {
        if (frequency.isEmpty()) {
            pdf.addText("No compilation errors were recorded in this session.");
            return;
        }
        List<Map.Entry<String, Long>> errors = new ArrayList<>(frequency.entrySet());
        errors.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        String[] labels = new String[errors.size()];
        double[] counts = new double[errors.size()];
        String[] countLabels = new String[errors.size()];
        for (int i = 0; i < errors.size(); i++) {
            labels[i] = errors.get(i).getKey();
            counts[i] = errors.get(i).getValue();
            countLabels[i] = String.valueOf(errors.get(i).getValue());
        }
        pdf.addBarChart(labels, counts, countLabels);
    }

    // Accepts an Instant or an ISO text like "2026-11-09T14:00:00Z" (the timeline uses text).
    private String formatTime(Object time) {
        if (time == null) {
            return "-";
        }
        if (time instanceof Instant instant) {
            return timeFormat.format(instant);
        }
        return timeFormat.format(Instant.parse(time.toString()));
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    // Locale.US makes sure decimals use a dot on every computer.

    static String duration(Duration d) {
        long s = d.getSeconds();
        return String.format(Locale.US, "%dh %02dm %02ds", s / 3600, (s % 3600) / 60, s % 60);
    }

    static String percent(double ratio) {
        return String.format(Locale.US, "%.1f%%", ratio * 100);
    }

    static String decimal(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
