package com.example;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// One thing that happened in BlueJ, e.g. a compile error or a method run.
public class TelemetryRecord {

    private Instant timestamp;
    private String category;
    private String eventType;
    private String details;

    // Extra information that depends on the event, e.g. "line" -> 7 for a compile error.
    // LinkedHashMap keeps the keys in the order they were added, so the JSON is easy to read.
    private Map<String, Object> attributes = new LinkedHashMap<>();

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimeStamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void addAttribute(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Attribute key cannot be null");
        }
        if (value != null) { // nothing to record for a null value
            attributes.put(key, value);
        }
    }

    // Read-only, so other classes can't change a record's attributes by accident.
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
