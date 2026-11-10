package com.example;

import bluej.extensions2.BPackage;
import bluej.extensions2.event.ExtensionEvent;
import bluej.extensions2.event.PackageEvent;

import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class PackageEventAdapter implements EventAdapter {

    public static final String CATEGORY = "PROJECT";
    public static final String PROJECT_CREATED = "PROJECT_CREATED";
    public static final String PROJECT_OPENED = "PROJECT_OPENED";
    public static final String PROJECT_CLOSED = "PROJECT_CLOSED";

    /** A project whose folder is younger than this when opened is treated as newly created. */
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
            folderCreated = Files.readAttributes(pkg.getProject().getDir().toPath(), BasicFileAttributes.class)
                    .creationTime().toInstant();
            classCount = pkg.getClasses().length;
        } catch (Exception e) {
            // project may already be closing; keep what we have
        }

        String type = switch (pe.getEventType()) {
            case PACKAGE_OPENED -> openedType(folderCreated, Instant.now(clock));
            case PACKAGE_CLOSING -> PROJECT_CLOSED;
        };
        return build(type, projectName, classCount);
    }

    /** PROJECT_CREATED if the folder was created moments ago, otherwise PROJECT_OPENED. */
    static String openedType(Instant folderCreated, Instant now) {
        if (folderCreated != null && Duration.between(folderCreated, now).compareTo(NEW_PROJECT_WINDOW) <= 0) {
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