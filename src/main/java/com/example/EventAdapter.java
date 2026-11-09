package com.example;

import bluej.extensions2.event.ExtensionEvent;

public interface EventAdapter {
    public TelemetryRecord adapt(ExtensionEvent event);
}
