package com.example;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfExporterTest {

    private static final Instant T0 = Instant.parse("2026-11-09T10:00:00Z");

    private static TelemetryRecord rec(String type, Instant time) {
        TelemetryRecord r = new TelemetryRecord();
        r.setTimeStamp(time);
        r.setEventType(type);
        return r;
    }

    private static String export(List<TelemetryRecord> records) throws Exception {
        TelemetryAggregator stats = new TelemetryAggregator(records);
        SessionMetadata meta = new SessionMetadata("Demo", T0, T0.plusSeconds(1800), stats.compileAttempts());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new PdfExporter(ZoneOffset.UTC).export(meta, stats, records, out);
        return out.toString(StandardCharsets.ISO_8859_1);
    }

    @Test
    void fileExtensionIsPdf() {
        assertEquals("pdf", new PdfExporter().fileExtension());
    }

    @Test
    void reportIsAValidPdfWithAllSections() throws Exception {
        List<TelemetryRecord> records = new ArrayList<>();
        records.add(rec(CompileEventAdapter.COMPILE_START, T0));
        TelemetryRecord error = rec(CompileEventAdapter.COMPILE_ERROR, T0.plusSeconds(5));
        error.addAttribute("errorType", "';' expected");
        records.add(error);
        TelemetryRecord created = rec(ClassTracker.CLASS_CREATED, T0.plusSeconds(10));
        created.addAttribute("className", "Shape");
        records.add(created);

        String pdf = export(records);

        assertTrue(pdf.startsWith("%PDF-1.4"));
        assertTrue(pdf.trim().endsWith("%%EOF"));
        assertTrue(pdf.contains("(BlueJ Telemetry Report) Tj"));
        assertTrue(pdf.contains("(STUDENT_ID) Tj"));
        assertTrue(pdf.contains("(Active coding time vs idle time) Tj"));
        assertTrue(pdf.contains("(Compilation error frequency) Tj"));
        assertTrue(pdf.contains("(';' expected) Tj"));
        assertTrue(pdf.contains("(Progression timeline: classes and methods) Tj"));
        assertTrue(pdf.contains("(Shape) Tj"));
        assertTrue(pdf.contains("(2026-11-09 10:00:10) Tj"));
    }

    @Test
    void xrefOffsetPointsAtTheXrefTable() throws Exception {
        String pdf = export(List.of());

        int start = pdf.lastIndexOf("startxref\n") + "startxref\n".length();
        int offset = Integer.parseInt(pdf.substring(start, pdf.indexOf('\n', start)).trim());
        assertTrue(pdf.startsWith("xref", offset));
    }

    @Test
    void emptySessionStillProducesReport() throws Exception {
        String pdf = export(List.of());

        assertTrue(pdf.contains("(No compilation errors were recorded in this session.) Tj"));
        assertTrue(pdf.contains("(\\(none recorded\\)) Tj"));
    }

    @Test
    void longTimelineSpillsOntoMorePages() throws Exception {
        List<TelemetryRecord> records = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            TelemetryRecord r = rec(ClassTracker.METHOD_ADDED, T0.plusSeconds(i));
            r.addAttribute("className", "C" + i);
            r.addAttribute("method", "m" + i + "()");
            records.add(r);
        }

        String pdf = export(records);

        int count = pdf.indexOf("/Count ") + "/Count ".length();
        int pages = Integer.parseInt(pdf.substring(count, pdf.indexOf(' ', count)).trim());
        assertTrue(pages > 1, "expected several pages, got " + pages);
    }

    @Test
    void formattingHelpers() {
        assertEquals("1h 01m 05s", PdfExporter.duration(Duration.ofSeconds(3665)));
        assertEquals("50.0%", PdfExporter.percent(0.5));
        assertEquals("1.50", PdfExporter.decimal(1.5));
    }
}
