package com.temporal.jetstream.dto;

/**
 * DTO representing a workflow history event with human-readable formatting.
 */
public class WorkflowHistoryEvent {
    private long eventId;
    private String eventType;
    private String timestamp;
    private String description;
    private String category;

    // Builder pattern for cleaner object construction
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final WorkflowHistoryEvent event = new WorkflowHistoryEvent();

        public Builder eventId(long eventId) {
            event.eventId = eventId;
            return this;
        }

        public Builder eventType(String eventType) {
            event.eventType = eventType;
            return this;
        }

        public Builder timestamp(String timestamp) {
            event.timestamp = timestamp;
            return this;
        }

        public Builder description(String description) {
            event.description = description;
            return this;
        }

        public Builder category(String category) {
            event.category = category;
            return this;
        }

        public WorkflowHistoryEvent build() {
            return event;
        }
    }

    // Getters and setters
    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
