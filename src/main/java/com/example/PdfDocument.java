package com.example;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// A very small PDF writer, so we don't need a PDF library in the jar.
// A PDF file is a list of numbered objects (catalog, page list, fonts, pages and their
// drawing commands), followed by a table that says where each object starts in the file.
class PdfDocument {

    static final int PAGE_WIDTH = 595;  // A4 size in points
    static final int PAGE_HEIGHT = 842;
    static final int MARGIN = 50;
    static final int CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    static final int ROW_HEIGHT = 18;

    private static final String BLUE = "0.16 0.37 0.62";
    private static final String LIGHT_GREY = "0.93 0.94 0.96";

    private List<StringBuilder> pages = new ArrayList<>();
    private StringBuilder page; // drawing commands for the current page
    private int y;              // where the next line goes (in a PDF, y = 0 is the bottom of the page)

    PdfDocument() {
        newPage();
    }

    int pageCount() {
        return pages.size();
    }

    void addTitle(String text) {
        needSpace(40);
        drawText(MARGIN, y - 22, 22, true, text);
        y -= 40;
    }

    void addHeading(String text) {
        needSpace(60);
        y -= 10;
        drawText(MARGIN, y - 14, 14, true, text);
        drawLine(MARGIN, y - 20, MARGIN + CONTENT_WIDTH, y - 20, BLUE);
        y -= 30;
    }

    void addText(String text) {
        needSpace(18);
        drawText(MARGIN, y - 10, 10, false, text);
        y -= 18;
    }

    // A table with a grey header row. columnWidths should add up to CONTENT_WIDTH.
    void addTable(String[] headers, List<String[]> rows, int[] columnWidths) {
        addRow(headers, columnWidths, true);
        if (rows.isEmpty()) {
            addRow(new String[] {"(none recorded)"}, new int[] {CONTENT_WIDTH}, false);
        }
        for (String[] row : rows) {
            if (y - ROW_HEIGHT < MARGIN) {
                newPage();
                addRow(headers, columnWidths, true); // repeat the header on the new page
            }
            addRow(row, columnWidths, false);
        }
        y -= 10;
    }

    // One horizontal bar per label. The longest bar is the biggest value.
    void addBarChart(String[] labels, double[] values, String[] valueLabels) {
        double max = 0;
        for (double v : values) {
            max = Math.max(max, v);
        }
        int labelWidth = 170;
        int maxBarWidth = CONTENT_WIDTH - labelWidth - 80;

        for (int i = 0; i < labels.length; i++) {
            needSpace(ROW_HEIGHT + 4);
            int barWidth = 1;
            if (max > 0) {
                barWidth = Math.max(1, (int) (values[i] / max * maxBarWidth));
            }
            drawText(MARGIN, y - 13, 10, false, fit(labels[i], 10, labelWidth - 8));
            fillBox(MARGIN + labelWidth, y - ROW_HEIGHT + 3, barWidth, ROW_HEIGHT - 6, BLUE);
            drawText(MARGIN + labelWidth + barWidth + 6, y - 13, 10, false, valueLabels[i]);
            y -= ROW_HEIGHT + 4;
        }
        y -= 8;
    }

    void save(OutputStream out) throws IOException {
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>(); // where each object starts

        write(pdf, "%PDF-1.4\n");

        // Objects 1-4: catalog, page list, normal font, bold font. Then two objects per page.
        String kids = "";
        for (int i = 0; i < pages.size(); i++) {
            kids += (5 + 2 * i) + " 0 R ";
        }
        writeObject(pdf, offsets, "<< /Type /Catalog /Pages 2 0 R >>");
        writeObject(pdf, offsets, "<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>");
        writeObject(pdf, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        writeObject(pdf, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");

        for (int i = 0; i < pages.size(); i++) {
            int contentsNumber = 6 + 2 * i;
            writeObject(pdf, offsets, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT
                    + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentsNumber + " 0 R >>");
            String commands = pages.get(i).toString();
            writeObject(pdf, offsets, "<< /Length " + commands.length() + " >>\nstream\n" + commands + "\nendstream");
        }

        // The cross-reference table: the byte position of every object
        int xrefStart = pdf.size();
        write(pdf, "xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
        for (int offset : offsets) {
            write(pdf, String.format("%010d 00000 n \n", offset));
        }
        write(pdf, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\n");
        write(pdf, "startxref\n" + xrefStart + "\n%%EOF\n");

        pdf.writeTo(out);
    }

    // Drawing helpers. Each one adds PDF drawing commands to the current page.

    private void addRow(String[] cells, int[] columnWidths, boolean header) {
        needSpace(ROW_HEIGHT);
        if (header) {
            fillBox(MARGIN, y - ROW_HEIGHT, CONTENT_WIDTH, ROW_HEIGHT, LIGHT_GREY);
        }
        int x = MARGIN;
        for (int i = 0; i < cells.length; i++) {
            drawText(x + 4, y - 13, 9, header, fit(cells[i], 9, columnWidths[i] - 8));
            x += columnWidths[i];
        }
        drawLine(MARGIN, y - ROW_HEIGHT, MARGIN + CONTENT_WIDTH, y - ROW_HEIGHT, "0.8 0.8 0.8");
        y -= ROW_HEIGHT;
    }

    private void drawText(int x, int baseline, int size, boolean bold, String text) {
        String font = bold ? "/F2" : "/F1";
        page.append("BT " + font + " " + size + " Tf " + x + " " + baseline + " Td (" + escape(text) + ") Tj ET\n");
    }

    private void drawLine(int x1, int y1, int x2, int y2, String colour) {
        page.append(colour + " RG 0.5 w " + x1 + " " + y1 + " m " + x2 + " " + y2 + " l S\n");
    }

    private void fillBox(int x, int bottom, int width, int height, String colour) {
        page.append(colour + " rg " + x + " " + bottom + " " + width + " " + height + " re f 0 0 0 rg\n");
    }

    private void needSpace(int height) {
        if (y - height < MARGIN) {
            newPage();
        }
    }

    private void newPage() {
        page = new StringBuilder();
        pages.add(page);
        y = PAGE_HEIGHT - MARGIN;
    }

    private static void writeObject(ByteArrayOutputStream pdf, List<Integer> offsets, String body) {
        offsets.add(pdf.size());
        write(pdf, offsets.size() + " 0 obj\n" + body + "\nendobj\n");
    }

    private static void write(ByteArrayOutputStream pdf, String text) {
        pdf.writeBytes(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    // Text helpers

    // Rough width of Helvetica text: about half the font size per character.
    static int textWidth(String text, int size) {
        return (int) Math.ceil(text.length() * size * 0.52);
    }

    // Shortens text with "..." so it fits in the given width.
    static String fit(String text, int size, int width) {
        if (text == null) {
            return "";
        }
        if (textWidth(text, size) <= width) {
            return text;
        }
        int maxChars = (int) (width / (size * 0.52)) - 3;
        return text.substring(0, Math.max(0, Math.min(maxChars, text.length()))) + "...";
    }

    // In a PDF, ( ) and \ inside text must have a \ in front of them.
    // Characters the built-in font can't show become "?".
    static String escape(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '(' || c == ')' || c == '\\') {
                result.append('\\').append(c);
            } else if (c == '\n' || c == '\r' || c == '\t') {
                result.append(' ');
            } else if (c < 32 || c > 255) {
                result.append('?');
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
