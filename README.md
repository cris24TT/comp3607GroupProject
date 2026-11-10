# BlueJ Telemetry Extension (COMP 3607 Group Project)

A BlueJ 6 extension that records how a student works in BlueJ and exports the session as a JSON file (and a PDF report).

## What it records
| Area | Events |
|---|---|
| Project lifecycle | `PROJECT_CREATED`, `PROJECT_OPENED`, `PROJECT_CLOSED` |
| Class lifecycle | `CLASS_CREATED`, `CLASS_MODIFIED`, `CLASS_COMPILED`, `CLASS_RENAMED`, `CLASS_REMOVED`, `CLASS_DELETED`, `METHOD_ADDED`, `METHOD_REMOVED` |
| Editing | `EDIT_BURST` (edits within 5 s grouped: edit count, characters inserted/deleted, lines, timing) |
| Compilation | `COMPILE_START`, `COMPILE_ERROR` (line, message, error type), `COMPILE_WARNING`, `COMPILE_SUCCESS`, `COMPILE_FAILED` |
| Execution | `RUN_NORMAL`, `RUN_EXCEPTION`, `RUN_TERMINATED`, `RUN_UNKNOWN` (class, method, parameters, result summary) |

The JSON summary includes: compile attempts, success rate, error count, errors per attempt, error frequency by type, run and exception counts, active vs idle seconds, edit bursts, total edits, typing cadence (ms between edits), edits per active minute, and a creation timeline of the project, classes and methods.

## Requirements
- BlueJ 6.0.1 (bundled Java 21)
- JDK 21+ and Maven to build

## Build
Run `mvn clean verify` in the project folder. This compiles the code, runs all JUnit 5 tests, builds `target/SimpleExtension.jar` (with Gson bundled), and writes the coverage report to `target/site/jacoco/index.html`.

## Install
1. Quit BlueJ.
2. Copy `target/SimpleExtension.jar` to `%USERPROFILE%\bluej\extensions2\` (Windows) or `~/.bluej/extensions2/` (macOS/Linux).
3. Start BlueJ. *Help > Installed Extensions* lists **BlueJ Telemetry**.

## Use
- Work in BlueJ as normal. Telemetry is recorded automatically.
- **Tools > Export telemetry now** writes a snapshot at any time.
- Closing a project (or quitting BlueJ) writes the full session.
- Files are saved in `<your project>/telemetry/telemetry-yyyyMMdd-HHmmss.json` (and `.pdf`).

## Design
- **Adapter**: each `*EventAdapter` converts a BlueJ event into a `TelemetryRecord`.
- **Factory**: `EventAdapterFactory` selects the adapter for an event type; new event types are registered, not hard-coded (Open/Closed).
- **Strategy**: `TelemetryExporter` with `JsonExporter` (and `PdfExporter`); the listener runs every exporter in its list.
- BlueJ-free core (`TelemetryAggregator`, `ClassTracker`, `EditTracker`, exporters, adapters' `build` methods) so it can be unit-tested without the BlueJ GUI. Time is injected with `java.time.Clock` for deterministic tests.

## Tests and coverage
- JUnit 5 suites cover adapters, the factory, the record model, class/method tracking, edit tracking, aggregator calculations and JSON serialisation.
- Coverage (JaCoCo): ~87% instructions. `SimpleExtension`, `TelemetryListener` and `TelemetryMenu` are excluded from the report: they are the BlueJ integration layer, need a running BlueJ, and were verified by manual testing in BlueJ.

## Known limitations (BlueJ extensions API)
- No javac error codes are exposed; errors are grouped by the first line of the message.
- BlueJ usually reports only the first compile error per attempt.
- No exception message or console output is available; the method's return value is used as the output summary.
- The method name is not reported for runs that throw or are terminated.
- Project deletion happens outside BlueJ and cannot be detected. Project creation is inferred from the folder's creation time.
- A brand-new class gets its typing listener once BlueJ reports it (usually at its first compile).
- Idle time is inferred from gaps of more than 5 minutes between events.