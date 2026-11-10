package com.example;
 
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.ExtensionEvent;
import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.PackageEvent;
 
import java.util.HashMap;
import java.util.Map;
 
/**
 * Factory that maps each BlueJ event type to the adapter that turns it into a TelemetryRecord.
 * New event types are supported by registering a new adapter (Open/Closed Principle).
 */
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
 
    /** Returns the adapter for this event, or null if the event type is not supported. */
    public EventAdapter adapterFor(ExtensionEvent event) {
        return adapterFor(event.getClass());
    }
 
    /** Returns the adapter registered for this event class, or null if none. */
    public EventAdapter adapterFor(Class<?> eventType) {
        return adapters.get(eventType);
    }
}
