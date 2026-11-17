package com.example;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

// Strategy pattern: each exporter writes the session in a different format (JSON, PDF).
// TelemetryListener just loops over its exporters, so a new format doesn't change it.
public interface TelemetryExporter {

    // e.g. "json" or "pdf"
    String fileExtension();

    // Writes the session to out. The caller opens and closes the stream.
    void export(SessionMetadata metadata, TelemetryAggregator stats,
                List<TelemetryRecord> records, OutputStream out) throws IOException;
}
