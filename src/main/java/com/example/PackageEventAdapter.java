package com.example;

import bluej.extensions2.BPackage;
import bluej.extensions2.event.ExtensionEvent;
import bluej.extensions2.event.PackageEvent;

import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

// Adapter pattern: turns a BlueJ PackageEvent (project opened or closing) into a TelemetryRecord.
public class PackageEventAdapter implements EventAdapter {

    public static final String CATEGORY = "PROJECT";
    public static final String PROJECT_CREATED = "PROJECT_CREATED";
    public static final String PROJECT_OPENED = "PROJECT_OPENED";
    public static final String PROJECT_CLOSED = "PROJECT_CLOSED";

    // BlueJ has no "project created" event. If the project folder is less than
    // 2 minutes old when it opens, we count it as newly created.
    static final Duration NEW_PROJECT_WINDOW = Duration.ofMinutes(2);

    private final Clock clock;

    public PackageEventAdapter() {
        this(Clock.systemDefaultZone());
    }

    public PackageEventAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public TelemetryRecord adapt(ExtensionEvent event) {
        PackageEvent pe = (PackageEvent) event;
        BPackage pkg = pe.getPackage();

        String projectName = "unknown";
        Instant folderCreated = null;
        Integer classCount = null;
        try {
            projectName = pkg.getProject().getName();
            BasicFileAttributes folder = Files.readAttributes(
                    pkg.getProject().getDir().toPath(), BasicFileAttributes.class);
            folderCreated = folder.creationTime().toInstant();
            classCount = pkg.getClasses().length;
        } catch (Exception e) {
            // The project may already be closing. Keep whatever we managed to read.
        }

        String type = PROJECT_CLOSED;
        if (pe.getEventType() == PackageEvent.EventType.PACKAGE_OPENED) {
            type = openedType(folderCreated, Instant.now(clock));
        }
        return build(type, projectName, classCount);
    }

    static String openedType(Instant folderCreated, Instant now) {
        if (folderCreated == null) {
            return PROJECT_OPENED;
        }
        Duration age = Duration.between(folderCreated, now);
        if (age.compareTo(NEW_PROJECT_WINDOW) <= 0) {
            return PROJECT_CREATED;
        }
        return PROJECT_OPENED;
    }

    TelemetryRecord build(String type, String projectName, Integer classCount) {
        TelemetryRecord r = new TelemetryRecord();
        r.setTimeStamp(Instant.now(clock));
        r.setCategory(CATEGORY);
        r.setEventType(type);
        r.addAttribute("projectName", projectName);
        r.addAttribute("classCount", classCount);
        return r;
    }
}
