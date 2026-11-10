package com.example;

import java.util.List;

import bluej.extensions2.BPackage;
import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.ExtensionEvent;
import bluej.extensions2.event.ExtensionEventListener;
import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.PackageEvent;

/*
 * This is the starting point of a BlueJ Extension
 */
public class SimpleExtension extends Extension {
    /*
     * When this method is called, the extension may start its work.
     */
    @Override
    public void startup(BlueJ bluej) {

        System.out.println("EXTENSION STARTED");

        TelemetryListener listener = new TelemetryListener();

        bluej.addExtensionEventListener(listener);
        bluej.setMenuGenerator(new TelemetryMenu(listener));

        System.out.println("EVENT LISTENER REGISTERED");
    }

    @Override
    public boolean isCompatible() {
        return (getExtensionsAPIVersionMajor() >= 3);
    }

    @Override
    public String getVersion() {
        return ("1.2.3");
    }

    @Override
    public String getName() {
        return ("Simple Extension");
    }

    @Override
    public void terminate() {
        System.out.println("Simple extension terminates");
    }

    @Override
    public String getDescription() {
        return ("A simple extension");
    }

}
