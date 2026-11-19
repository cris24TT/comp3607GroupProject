# Tracking and Analysis

*Owner: Cristian*

These classes contain no BlueJ code. They turn the raw records into higher-level events and summary numbers, and are fully unit tested.

## ClassTracker: classes and methods

BlueJ has no "class created" or "method added" event. `ClassTracker` keeps the last list of classes (and each class's methods) it saw, and compares it with the new list each time.

| Method | Records |
| --- | --- |
| `baseline(classes)` | Nothing: remembers the classes that exist when the project opens, so they aren't reported as new |
| `updateClasses(current)` | `CLASS_CREATED` for each new name, `CLASS_DELETED` for each missing one |
| `updateMethods(className, methods)` | `METHOD_ADDED` / `METHOD_REMOVED` for each change. The first look at a pre-existing class records nothing; for a class created this session, every method is new |

Category `LIFECYCLE`; attributes `className` and `method`. Names are processed in alphabetical order so the output is always the same.

## EditTracker: typing

Recording every keystroke would make the JSON huge, so `EditTracker` groups edits into **edit bursts**. Edits in the same class with gaps of 5 seconds or less belong to one burst. A longer pause, a switch to another class, a compile or an export ends the burst.

Each `EDIT_BURST` record (category `EDIT`) has:

| Attribute | Meaning |
| --- | --- |
| `className` | Class being edited |
| `edits` | Number of text changes in the burst |
| `charsInserted`, `charsDeleted` | Characters typed and removed |
| `linesAdded`, `linesRemoved` | Lines added and removed |
| `durationMs` | Time from the first to the last edit |
| `gapSumMs` | Total time between consecutive edits (used for typing cadence) |
| `end` | Time of the last edit |

## TelemetryAggregator: summary numbers

`TelemetryAggregator` takes the session's records and calculates:

| Measure | Calculation |
| --- | --- |
| Compile attempts | Count of `COMPILE_START` |
| Compile success rate | `COMPILE_SUCCESS` ÷ attempts (0 if no attempts) |
| Compile errors | Count of `COMPILE_ERROR` |
| Errors per attempt | Errors ÷ attempts (0 if no attempts) |
| Error frequency | Number of errors per `errorType`, e.g. `{"';' expected": 2}` |
| Program runs | Count of `INVOCATION` records |
| Runtime exceptions | Count of `RUN_EXCEPTION` |
| Session span | First activity to last activity (an edit burst counts at its start and its `end`) |
| Idle time | Sum of gaps between activities longer than the threshold (default **5 minutes**) |
| Active time | Session span − idle time |
| Edit bursts, total edits | Count of bursts; sum of their `edits` |
| Typing cadence | Total `gapSumMs` ÷ number of gaps between edits = average ms between keystrokes |
| Edits per active minute | Total edits ÷ active minutes |
| Creation timeline | `PROJECT_CREATED`, `CLASS_CREATED` and `METHOD_ADDED` records in time order |

All calculations return 0 instead of dividing by zero.

## Tests

| Test class | What it checks |
| --- | --- |
| `ClassTrackerTest` (4) | New and missing classes; existing classes baselined silently; all methods of a new class reported |
| `EditTrackerTest` (4) | Close edits form one burst; long pause or class switch starts a new one; flushing with nothing open |
| `TelemetryAggregatorTest` (7) | Empty session gives zeros; success rate; error frequency; errors per attempt; run and exception counts; idle gaps; single event |
| `TypingStatsTest` (2) | Cadence and edits per minute from bursts; zeros with no typing |
| `CreationTimelineTest` (2) | Timeline order; deletions ignored; missing timestamps handled |
| `TelemetryRecordTest` (4) | Fields; null key rejected; null value skipped; attributes read-only and in insertion order |
