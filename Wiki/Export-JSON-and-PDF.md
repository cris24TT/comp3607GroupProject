# Export JSON and PDF

*Owner: Cristian*

## Strategy pattern

`TelemetryExporter` is the strategy interface:

```java
public interface TelemetryExporter {
    String fileExtension();   // "json" or "pdf"
    void export(SessionMetadata metadata, TelemetryAggregator stats,
                List<TelemetryRecord> records, OutputStream out) throws IOException;
}
```

`JsonExporter` and `PdfExporter` are the two strategies. `TelemetryListener` holds a list of exporters and, at export time, opens one file per exporter (`telemetry-yyyyMMdd-HHmmss.<extension>`) and calls `export` on each. Adding another format (CSV, say) means writing one new class and adding it to the list; nothing else changes.

Export happens:

- on **Project › Close** and when BlueJ quits (the session then restarts), and
- on **Tools › Export telemetry now** (a snapshot; recording continues).

## Session metadata

`SessionMetadata` holds the session-level facts written at the top of the JSON:

| Field | Meaning |
| --- | --- |
| `studentId` | Placeholder `STUDENT_ID` (no real ID is collected) |
| `projectName` | BlueJ project name |
| `sessionStart`, `sessionEnd` | ISO-8601 UTC timestamps |
| `totalSessionSeconds` | End − start |
| `totalCompilationAttempts` | Number of `COMPILE_START` events |

## JSON format

The file has three parts:

| Part | Contents |
| --- | --- |
| `sessionMetadata` | See above |
| `summary` | Every number from `TelemetryAggregator` (see [[Tracking and Analysis]]), including `creationTimeline` |
| `events` | Every record in order: `timestamp`, `category`, `eventType`, `attributes` |

Gson writes it pretty-printed. Gson can't serialise `java.time.Instant` on Java 21 by itself, so `JsonExporter` registers a serializer that writes each `Instant` as ISO-8601 text.

Example: a partially correct session (one compile error, fixed, then a normal run and a division by zero). Only 2 of the 10 events are shown.

```json
{
  "sessionMetadata": {
    "studentId": "STUDENT_ID",
    "projectName": "UC2Partial",
    "sessionStart": "2026-11-17T20:00:00Z",
    "sessionEnd": "2026-11-17T20:02:30Z",
    "totalSessionSeconds": 150,
    "totalCompilationAttempts": 2
  },
  "summary": {
    "compileAttempts": 2,
    "compileSuccessRate": 0.5,
    "compileErrorCount": 1,
    "errorsPerCompileAttempt": 0.5,
    "errorFrequency": {
      "';' expected": 1
    },
    "runCount": 2,
    "exceptionCount": 1,
    "activeSeconds": 140,
    "idleSeconds": 0,
    "idleThresholdSeconds": 300,
    "creationTimeline": [
      {
        "time": "2026-11-17T20:00:00Z",
        "event": "PROJECT_CREATED",
        "projectName": "UC2Partial",
        "classCount": 0
      },
      {
        "time": "2026-11-17T20:01:31Z",
        "event": "CLASS_CREATED",
        "className": "Divider"
      },
      {
        "time": "2026-11-17T20:01:31Z",
        "event": "METHOD_ADDED",
        "className": "Divider",
        "method": "divide(int, int)"
      }
    ],
    "editBursts": 0,
    "totalEdits": 0,
    "typingCadenceMs": 0.0,
    "editsPerActiveMinute": 0.0
  },
  "events": [
    {
      "timestamp": "2026-11-17T20:01:00Z",
      "category": "COMPILE",
      "eventType": "COMPILE_ERROR",
      "attributes": {
        "files": [
          "Divider.java"
        ],
        "line": 3,
        "message": "';' expected",
        "errorType": "';' expected"
      }
    },
    {
      "timestamp": "2026-11-17T20:02:00Z",
      "category": "INVOCATION",
      "eventType": "RUN_NORMAL",
      "attributes": {
        "className": "Divider",
        "invocationKind": "method",
        "methodName": "divide",
        "objectName": "divider1",
        "parameters": [
          "10",
          "2"
        ],
        "exception": false,
        "resultSummary": "5"
      }
    }
  ]
}
```

## PDF report

`PdfExporter` builds the report with `PdfDocument`, a small PDF writer in this project (no PDF library, so the jar stays small). It uses A4 pages, the built-in Helvetica font, tables and bar charts, and starts a new page when content runs out.

The report contains:

| Section | Shows |
| --- | --- |
| BlueJ Telemetry Report | Student ID, project, session start and end, total duration, number of events |
| Active coding time vs idle time | Bar chart of active and idle time, and the idle threshold |
| Compilation | Attempts, successful and failed compiles, success rate, errors, errors per attempt |
| Compilation error frequency | Bar chart of each error type, most common first |
| Program runs and editing | Runs, runtime exceptions, edit bursts, total edits, typing cadence, edits per active minute |
| Progression timeline | Table of project creation, class creations and method definitions with times |

Times in the PDF use the computer's time zone; times in the JSON are UTC.

## Tests

| Test class | What it checks |
| --- | --- |
| `JsonExporterTest` (3) | Session duration; exported JSON parses and contains metadata, summary and events; extension is `json` |
| `PdfExporterTest` (6) | Valid PDF with every section; cross-reference offset correct; empty session; long timeline spills onto more pages; formatting of durations and percentages |
| `PdfDocumentTest` (4) | Escaping of PDF special characters; long text shortened; new page when a table runs out of space; file starts with `%PDF-1.4` and ends with `%%EOF` |
