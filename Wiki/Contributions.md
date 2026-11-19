# Contributions

| Member | Code produced | Tests | Pattern |
| --- | --- | --- | --- |
| [Author] | Original skeleton: `SimpleExtension` (entry point), `EventAdapter` interface, `EventAdapterFactory`, `TelemetryRecord` fields, first versions of the listener and the class and package adapters, starter Maven setup | none | Factory |
| Raiden | `CompileEventAdapter`, `InvocationFinishedEventAdapter`, `ClassEventAdapter` and `PackageEventAdapter` (rewritten), `TelemetryListener` (rewritten), `TelemetryMenu` | `CompileEventAdapterTest`, `InvocationFinishedEventAdapterTest`, `ClassEventAdapterTest`, `PackageEventAdapterTest`, `EventAdapterFactoryTest` | Adapter |
| Cristian | `ClassTracker`, `EditTracker`, `TelemetryAggregator`, `TelemetryExporter`, `JsonExporter`, `SessionMetadata`, `TelemetryRecord` attributes, `PdfExporter`, `PdfDocument`; JaCoCo setup; README | `ClassTrackerTest`, `EditTrackerTest`, `TelemetryAggregatorTest`, `TypingStatsTest`, `CreationTimelineTest`, `JsonExporterTest`, `TelemetryRecordTest`, `PdfExporterTest`, `PdfDocumentTest` | Strategy |

## Notes

- **Pair programming.** Raiden's and Cristian's code from Nov 9, 2026 was written together on one device and pushed from Cristian's account, so the commit history shows those commits under Cristian. The split above is by area of responsibility.
- **Nov 17, 2026.** Cristian added the PDF report and simplified his classes; Raiden simplified his classes (commit `0c33c13`).

## Other contributions

| Member | Contribution |
| --- | --- |
| [Author] | [e.g. scope document, wiki pages, video editing] |
| Raiden | [e.g. manual testing in BlueJ, demo video use cases] |
| Cristian | [e.g. video script, wiki pages] |
