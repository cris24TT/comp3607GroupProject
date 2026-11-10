package com.example;
 
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.PackageEvent;
import org.junit.jupiter.api.Test;
 
import static org.junit.jupiter.api.Assertions.*;
 
class EventAdapterFactoryTest {
 
    @Test
    void eachSupportedEventHasItsAdapter() {
        EventAdapterFactory f = new EventAdapterFactory();
 
        assertInstanceOf(CompileEventAdapter.class, f.adapterFor(CompileEvent.class));
        assertInstanceOf(ClassEventAdapter.class, f.adapterFor(ClassEvent.class));
        assertInstanceOf(InvocationFinishedEventAdapter.class, f.adapterFor(InvocationFinishedEvent.class));
        assertInstanceOf(PackageEventAdapter.class, f.adapterFor(PackageEvent.class));
    }
 
    @Test
    void unsupportedEventGivesNull() {
        assertNull(new EventAdapterFactory().adapterFor(String.class));
    }
 
    @Test
    void registerReplacesAdapter() {
        EventAdapterFactory f = new EventAdapterFactory();
        EventAdapter custom = event -> null;
 
        f.register(CompileEvent.class, custom);
 
        assertSame(custom, f.adapterFor(CompileEvent.class));
    }
}
