package com.example;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/** Strategy: one way of writing a session out (JSON, PDF, ...). */
public interface TelemetryExporter {

    /** File extension without the dot, e.g. "json" or "pdf". */
    String fileExtension();

    /** Writes the session to out. Must not close out; the caller does. */
    void export(SessionMetadata metadata, TelemetryAggregator stats,
                List<TelemetryRecord> records, OutputStream out) throws IOException;
}