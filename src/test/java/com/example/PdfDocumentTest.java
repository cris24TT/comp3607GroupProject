package com.example;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfDocumentTest {

    @Test
    void escapesPdfSpecialCharacters() {
        assertEquals("a\\(b\\)c\\\\d", PdfDocument.escape("a(b)c\\d"));
        assertEquals("x y", PdfDocument.escape("x\ny"));
        assertEquals("caf\u00e9 ?", PdfDocument.escape("caf\u00e9 \u2192"));
    }

    @Test
    void fitCutsLongTextWithDots() {
        assertEquals("short", PdfDocument.fit("short", 10, 200));
        assertEquals("", PdfDocument.fit(null, 10, 100));

        String cut = PdfDocument.fit("x".repeat(200), 10, 100);
        assertTrue(cut.endsWith("..."));
        assertTrue(PdfDocument.textWidth(cut, 10) <= 100);
    }

    @Test
    void longTableStartsANewPage() {
        PdfDocument doc = new PdfDocument();
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            rows.add(new String[] {"row " + i, "value"});
        }

        doc.addTable(new String[] {"Measure", "Value"}, rows, new int[] {300, 195});

        assertEquals(2, doc.pageCount());
    }

    @Test
    void savedFileStartsAndEndsLikeAPdf() throws Exception {
        PdfDocument doc = new PdfDocument();
        doc.addTitle("Hello");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        doc.save(out);

        String pdf = out.toString(StandardCharsets.ISO_8859_1);
        assertTrue(pdf.startsWith("%PDF-1.4"));
        assertTrue(pdf.endsWith("%%EOF\n"));
        assertTrue(pdf.contains("(Hello) Tj"));
    }
}
