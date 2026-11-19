# Event Capture

*Owner: Raiden*

This part listens to BlueJ and turns its events into `TelemetryRecord`s. It is the only part of the code that uses the BlueJ extensions API (`bluej.extensions2`).

## Adapter pattern

`EventAdapter` is a one-method interface: `TelemetryRecord adapt(ExtensionEvent event)`. Each adapter converts one BlueJ event class:

| Adapter | BlueJ event | Category | Event types |
| --- | --- | --- | --- |
| `CompileEventAdapter` | `CompileEvent` | `COMPILE` | `COMPILE_START`, `COMPILE_ERROR`, `COMPILE_WARNING`, `COMPILE_SUCCESS`, `COMPILE_FAILED` |
| `InvocationFinishedEventAdapter` | `InvocationFinishedEvent` | `INVOCATION` | `RUN_NORMAL`, `RUN_EXCEPTION`, `RUN_TERMINATED`, `RUN_UNKNOWN` |
| `ClassEventAdapter` | `ClassEvent` | `CLASS` | `CLASS_MODIFIED`, `CLASS_COMPILED`, `CLASS_RENAMED`, `CLASS_REMOVED` |
| `PackageEventAdapter` | `PackageEvent` | `PROJECT` | `PROJECT_CREATED`, `PROJECT_OPENED`, `PROJECT_CLOSED` |

Each adapter has a package-private `build(...)` method that does the conversion from plain values, so the tests can call it without BlueJ. `adapt()` only reads the values out of the BlueJ event and calls `build()`.

### Attributes recorded

| Event | Attributes |
| --- | --- |
| Compile (all) | `files`: names of the files compiled |
| `COMPILE_ERROR`, `COMPILE_WARNING` | `line`, `message`, `errorType` (first line of the message) |
| Runs (all) | `className`, `invocationKind` (`method` or `constructor`), `methodName`, `objectName`, `parameters`, `exception` (true/false) |
| `RUN_NORMAL` of a method | `resultSummary`: the return value as text, cut to 100 characters, or `void` |
| Class events | `className`, `oldName` (renames only), `hasError` |
| Project events | `projectName`, `classCount` |

### Design decisions

- **Background compiles are skipped.** BlueJ compiles in the background while you type. `CompileEventAdapter` returns `null` unless `isUserGeneratedCompilation()` is true, so only compiles the student started count as attempts.
- **Error type.** BlueJ gives no javac error codes, so errors are grouped by the first line of the message (for example `';' expected`).
- **Void methods.** BlueJ's `getResult()` throws a `NullPointerException` for void methods; the adapter catches it and records `void`.
- **Constructor or method.** A constructor runs on a class (no object yet). When a method throws, BlueJ gives no method name, but the object name is still set, so `invocationKind` is still `method`.
- **Project created.** BlueJ has no "project created" event. If the project folder is less than 2 minutes old when it opens, `PackageEventAdapter` records `PROJECT_CREATED` instead of `PROJECT_OPENED`.

## TelemetryListener

`TelemetryListener` implements BlueJ's `ExtensionEventListener`. For every event it:

1. Retries reading methods of recently compiled classes (see below).
2. On `PACKAGE_OPENED`: remembers the project name and folder, records which classes already exist (so they aren't reported as new), and attaches typing listeners.
3. Asks `EventAdapterFactory` for the adapter and stores the record. Before a `COMPILE_START` it closes the current typing burst.
4. On a `ClassEvent`: passes the current class list to `ClassTracker` (classes created or deleted) and attaches typing listeners to new classes.
5. On `PACKAGE_CLOSING`: exports the session and starts a new one.

**Reading methods after a compile.** BlueJ has no "method added" event, so after a class compiles the listener reads its methods (`BClass.getDeclaredMethods()`) and passes them to `ClassTracker`. Right after a compile, BlueJ may not have loaded the new class yet, and reading it throws `ClassNotFoundException`. The listener keeps the class in `methodsToCheck` and retries on every later event and at export until it succeeds.

**Typing.** For each Java class, the listener adds a document listener to its editor (`BClass.getJavaEditor()`). Every text change is passed to `EditTracker` with the characters and lines inserted and deleted (see [[Tracking and Analysis]]).

## Tools menu

`TelemetryMenu` extends BlueJ's `MenuGenerator` and adds **Tools › Export telemetry now**. It calls `exportSnapshot()`, which writes the JSON and PDF without ending the session, and shows a pop-up with the saved file paths.

## Tests

| Test class | What it checks |
| --- | --- |
| `CompileEventAdapterTest` (6) | Error and warning records carry line, message and error type; success records carry files only; `errorType()` uses the first line, or `unknown` |
| `InvocationFinishedEventAdapterTest` (6) | Normal method runs carry a result summary; void → `void`; constructors have none; exceptions are flagged; long results are cut to 100 characters |
| `ClassEventAdapterTest` (3) | Renames carry the old name; missing values are left out |
| `PackageEventAdapterTest` (5) | New folder → `PROJECT_CREATED`, old or unknown → `PROJECT_OPENED`; records carry project name and class count |
| `EventAdapterFactoryTest` (3) | Each event type maps to its adapter; unsupported events give `null`; `register` replaces an adapter |

`TelemetryListener` and `TelemetryMenu` need a running BlueJ, so they were tested manually with the three use cases in the demo video (fully correct, partially correct, completely incorrect).

## Limitations

These come from the BlueJ extensions API:

- No javac error codes: errors are grouped by the first line of the message.
- BlueJ usually reports only the first compile error per attempt.
- No exception message or console output: the method's return value is used as the output summary.
- No method name for runs that throw or are terminated.
- Project deletion happens outside BlueJ and can't be detected; project creation is inferred from the folder's age.
- A brand-new class gets its typing listener once BlueJ reports it (usually at its first compile), so typing before that may not be recorded.
- `METHOD_ADDED` is timestamped when the methods could be read, usually a few seconds after the compile.
- BlueJ's editor Compile button doesn't recompile when an error from its background check is already highlighted; it only jumps to the error. Only compiles that actually run are counted, so compile from the main window (right-click the class › **Compile**) to have every attempt recorded.
