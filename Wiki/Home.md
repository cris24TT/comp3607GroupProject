# BlueJ Telemetry Extension

BlueJ Telemetry is an extension for **BlueJ 6.0.1** that records how a student works in BlueJ (creating projects and classes, typing, compiling and running code) and exports each session as a **JSON file** and a **PDF report**. It was built for COMP 3607 (Object Oriented Programming II), 2026/2027 Semester 1.

## Team

| Member | Area | Wiki pages |
| --- | --- | --- |
| [Author] | Original design: extension entry point, adapter interface, adapter factory, record model | [[Architecture and Design]], [[Installation and Usage]] |
| Raiden | Event capture: adapters, listener, Tools menu | [[Event Capture]] |
| Cristian Rampersad | Tracking, analysis, JSON and PDF export, test suite | [[Tracking and Analysis]], [[Export JSON and PDF]], [[Testing]] |

See [[Contributions]] for the full breakdown.

## Scope

The extension records five areas of student activity:

| Area | Events recorded |
| --- | --- |
| Project lifecycle | `PROJECT_CREATED`, `PROJECT_OPENED`, `PROJECT_CLOSED` |
| Class lifecycle | `CLASS_CREATED`, `CLASS_MODIFIED`, `CLASS_COMPILED`, `CLASS_RENAMED`, `CLASS_REMOVED`, `CLASS_DELETED`, `METHOD_ADDED`, `METHOD_REMOVED` |
| Editing | `EDIT_BURST` (typing grouped into bursts: edits, characters inserted and deleted, lines, timing) |
| Compilation | `COMPILE_START`, `COMPILE_ERROR` (line, message, error type), `COMPILE_WARNING`, `COMPILE_SUCCESS`, `COMPILE_FAILED` |
| Execution | `RUN_NORMAL`, `RUN_EXCEPTION`, `RUN_TERMINATED`, `RUN_UNKNOWN` (class, method, parameters, result summary) |

Out of scope: project deletion (happens outside BlueJ), Stride classes, console output and exception messages (not available through the BlueJ extensions API). See [Event Capture](Event-Capture#limitations).

## Requirements met

| Requirement | Where |
| --- | --- |
| Java, Maven project, GitHub repository | This repository (`pom.xml`) |
| At least three design patterns, no Singleton | Adapter, Factory, Strategy: [[Architecture and Design]] |
| SOLID principles | [Architecture and Design](Architecture-and-Design#solid-principles) |
| JSON export with session metadata | [[Export JSON and PDF]] |
| PDF report with summaries | [Export JSON and PDF](Export-JSON-and-PDF#pdf-report) |
| JUnit 5 tests, 75%+ coverage, independent of the BlueJ GUI | [[Testing]] |

## Pages

1. [[Installation and Usage]]
2. [[Architecture and Design]]
3. [[Event Capture]]
4. [[Tracking and Analysis]]
5. [[Export JSON and PDF]]
6. [[Testing]]
7. [[Contributions]]
