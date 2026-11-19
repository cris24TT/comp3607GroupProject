# Installation and Usage

*Owner: [Author]*

## Requirements

- BlueJ 6.0.1 (it bundles Java 21)
- JDK 21 or later and Maven 3.9+, to build the extension
- Git, to clone the repository

## Build

```powershell
git clone https://github.com/cris24TT/comp3607GroupProject comp3607groupproject
cd comp3607groupproject
mvn clean verify
```

`mvn clean verify` compiles the code, runs all JUnit tests, builds `target/SimpleExtension.jar` (with Gson packed inside by the Maven Shade plugin) and writes the coverage report to `target/site/jacoco/index.html`.

BlueJ's API (`bluej.jar`) is a `provided` dependency: it is needed to compile but is not packed into the jar, because BlueJ supplies it at runtime.

## Install

1. Quit BlueJ.
2. Copy the jar into your BlueJ extensions folder:
   - Windows: `%USERPROFILE%\bluej\extensions2\`
   - macOS / Linux: `~/.bluej/extensions2/`

   ```powershell
   Copy-Item target\SimpleExtension.jar "$HOME\bluej\extensions2\" -Force
   ```
3. Start BlueJ. **Help › Installed Extensions** should list **BlueJ Telemetry 1.0**.

BlueJ finds the extension through the `Main-Class: com.example.SimpleExtension` entry in the jar's manifest (set by the Maven jar plugin).

## Use

- Work in BlueJ as normal. Recording starts automatically when a project opens.
- **Tools › Export telemetry now** writes a snapshot at any time and shows where the files were saved. Recording continues.
- **Project › Close** (or quitting BlueJ) writes the full session and starts a new one.
- Files are written to `<project folder>/telemetry/`:
  - `telemetry-yyyyMMdd-HHmmss.json`
  - `telemetry-yyyyMMdd-HHmmss.pdf`

Create classes with the class language set to **Java**: method tracking reads compiled Java classes.

## Troubleshooting

| Problem | Fix |
| --- | --- |
| BlueJ Telemetry not in Installed Extensions | Check the jar is in `extensions2` (not `extensions`) and restart BlueJ |
| Only a `.json`, no `.pdf` | An old jar is installed: quit BlueJ, rebuild and copy the jar again |
| Copy fails with "file in use" | BlueJ is still running: quit it first |
| Where did my files go? | **Help › About BlueJ › Show debug log** lists every `TELEMETRY EXPORTED:` path |
