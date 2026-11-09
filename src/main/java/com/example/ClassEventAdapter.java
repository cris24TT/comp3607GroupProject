package com.example;

import bluej.extensions2.BPackage;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ExtensionEvent;
import bluej.extensions2.event.PackageEvent;

public class ClassEventAdapter implements EventAdapter {

    public TelemetryRecord adapt(ExtensionEvent event){

        TelemetryRecord temp = new TelemetryRecord();
        ClassEvent classEvent = (ClassEvent) event;

        temp.setTimeStamp(java.time.Instant.now());
        temp.setCategory("ClassEvent");
        temp.setEventType(classEvent.getEventType().name());
        temp.setDetails(String.valueOf(classEvent.getBClass()));//ClassName Probably try to get more detail here. 
        return temp;

    }

}
