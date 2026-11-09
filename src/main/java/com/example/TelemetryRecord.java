package com.example;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;


public class TelemetryRecord {
    private Instant timestamp;
    private String category;
    private String eventType;
    private String details;
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getCategory() {
        return category;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDetails() {
        return details;
    }

     public void setTimeStamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setDetails(String details) {
        this.details = details;
    }
public void addAttribute(String key, Object value) {
    if (key == null) {
        throw new IllegalArgumentException("Attribute key cannot be null");
    }
    if (value == null) {
        return; // nothing to record, skip it
    }
    attributes.put(key, value);
}
    public Map<String, Object> getAttributes() {
       return Collections.unmodifiableMap(attributes);
        }
    }


