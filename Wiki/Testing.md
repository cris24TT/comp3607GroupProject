# Testing

*Owner: Cristian (test suite and coverage). Each member wrote the tests for their own classes.*

## Running the tests

```powershell
mvn clean verify
```

This runs all **59 JUnit 5 tests** in 14 test classes and writes the JaCoCo coverage report to `target/site/jacoco/index.html`.

**Coverage: [__]%** of instructions (target 75%). Fill in from your latest report.

## How the code is made testable without BlueJ

- **BlueJ-free core.** Trackers, the aggregator, the exporters and the record model contain no BlueJ code.
- **`build()` methods.** Each adapter's conversion lives in a package-private `build(...)` method that takes plain values, so tests call it directly instead of creating BlueJ events.
- **Injected time.** Adapters and trackers take a `java.time.Clock`; tests pass `Clock.fixed(...)` so timestamps are always the same. `PdfExporter` takes a `ZoneId` for the same reason.
- **Excluded classes.** `SimpleExtension`, `TelemetryListener` and `TelemetryMenu` are excluded from the coverage report in `pom.xml`, because they only run inside BlueJ. They were tested manually (below).

## Unit tests

| Area the brief names | Test classes | Tests |
| --- | --- | --- |
| Telemetry parser and event listeners | `CompileEventAdapterTest`, `InvocationFinishedEventAdapterTest`, `ClassEventAdapterTest`, `PackageEventAdapterTest`, `EventAdapterFactoryTest`, `ClassTrackerTest`, `EditTrackerTest`, `TelemetryRecordTest` | 35 |
| JSON serialiser | `JsonExporterTest` | 3 |
| Report data aggregators | `TelemetryAggregatorTest`, `TypingStatsTest`, `CreationTimelineTest` | 11 |
| PDF report | `PdfExporterTest`, `PdfDocumentTest` | 10 |
| **Total** | **14 classes** | **59** |

Details per class are on [Event Capture](Event-Capture#tests), [Tracking and Analysis](Tracking-and-Analysis#tests) and [Export JSON and PDF](Export-JSON-and-PDF#tests).

## Manual tests in BlueJ

The BlueJ-only classes were tested with three use cases, also shown in the demo video. Each uses a new project with **Java** classes, compiled from the main BlueJ window (right-click the class › **Compile**).

| Use case | Steps | Expected report |
| --- | --- | --- |
| 1. Fully correct | `Rectangle` with `area()`: compiles first time, `new Rectangle(3, 4)`, `area()` → 12 | Success rate 100%, no errors, `RUN_NORMAL` with result `12`, `METHOD_ADDED area()` |
| 2. Partially correct | `Divider` missing a semicolon: compile error on line 3, fix, compile, `divide(10, 2)` → 5, `divide(5, 0)` → exception | Success rate 50%, `';' expected` × 1, 1 runtime exception, `METHOD_ADDED divide(int, int)` |
| 3. Completely incorrect | `Counter`: three compiles with three different errors, never fixed | Success rate 0%, three error types, no runs, no methods in the timeline |

All three produced a JSON and a PDF in the project's `telemetry` folder, and the Tools menu export showed both file paths.
