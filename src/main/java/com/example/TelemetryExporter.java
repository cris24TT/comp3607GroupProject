package com.example;

import java.io.IOException;
import java.io.Writer;

public interface TelemetryExporter {
    void export(SessionMetadata metadata, TelemetryAggregator stats,
                java.util.List<TelemetryRecord> records, Writer out) throws IOException;
}