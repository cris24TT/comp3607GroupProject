package com.example;

import bluej.extensions2.event.*;
import java.util.HashMap;
import java.util.Map;

public class EventAdapterFactory {

    private final Map<Class<? extends ExtensionEvent>, EventAdapter> adapters = new HashMap<>();

    public EventAdapterFactory() {
        register(CompileEvent.class, new CompileEventAdapter());
        register(PackageEvent.class, new PackageEventAdapter());
        register(ClassEvent.class, new ClassEventAdapter());
        register(InvocationFinishedEvent.class, new InvocationFinishedEventAdapter());
    }

    public void register(Class<? extends ExtensionEvent> type, EventAdapter adapter) {
        adapters.put(type, adapter);
    }

    
    public EventAdapter adapterFor(ExtensionEvent event) {
        return adapters.get(event.getClass());
    }
}