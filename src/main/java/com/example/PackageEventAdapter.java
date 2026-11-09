package com.example;

import bluej.extensions2.BPackage;
import bluej.extensions2.event.ExtensionEvent;
import bluej.extensions2.event.PackageEvent;

public class PackageEventAdapter implements EventAdapter {

    public TelemetryRecord adapt(ExtensionEvent event){

        TelemetryRecord temp = new TelemetryRecord();
        PackageEvent packageEvent = (PackageEvent) event;
        BPackage pkg = packageEvent.getPackage();

        temp.setTimeStamp(java.time.Instant.now());
        temp.setCategory("PackageEvent");
        temp.setEventType(packageEvent.getEventType().name());
        temp.setDetails(packageEvent.toString());
        return temp;

    }

}
