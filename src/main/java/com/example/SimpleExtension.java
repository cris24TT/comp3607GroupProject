package com.example;

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;

/** Entry point of the BlueJ Telemetry extension. BlueJ finds it through Main-Class in the jar manifest. */
public class SimpleExtension extends Extension {

    private TelemetryListener listener;

    @Override
    public void startup(BlueJ bluej) {
        listener = new TelemetryListener();
        bluej.addExtensionEventListener(listener);
        bluej.setMenuGenerator(new TelemetryMenu(listener));
        System.out.println("BlueJ Telemetry extension started");
    }

    @Override
    public void terminate() {
        if (listener != null) {
            listener.exportNow();
        }
        System.out.println("BlueJ Telemetry extension stopped");
    }

    @Override
    public boolean isCompatible() {
        return getExtensionsAPIVersionMajor() >= 3;
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getName() {
        return "BlueJ Telemetry";
    }

    @Override
    public String getDescription() {
        return "Records project, class, editing, compile and run activity and exports it as JSON (and PDF).";
    }
}