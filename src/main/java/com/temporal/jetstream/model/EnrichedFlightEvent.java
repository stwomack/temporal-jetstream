package com.temporal.jetstream.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Enriched flight event with calculated fields added by Flink processing.
 */
public class EnrichedFlightEvent {
    private FlightEventType eventType;
    private String flightNumber;
    private LocalDate flightDate;
    private String data; // Original event data (JSON string)

    // Enriched fields added by Flink
    private Integer estimatedDelay; // Calculated delay estimate
    private String riskScore; // Risk level: LOW, MEDIUM, HIGH
    private LocalDateTime enrichedTimestamp; // Timestamp when enrichment occurred

    public EnrichedFlightEvent() {
    }

    public EnrichedFlightEvent(FlightEvent originalEvent, Integer estimatedDelay, String riskScore, LocalDateTime enrichedTimestamp) {
        this.eventType = originalEvent.getEventType();
        this.flightNumber = originalEvent.getFlightNumber();
        this.flightDate = originalEvent.getFlightDate();
        this.data = originalEvent.getData();
        this.estimatedDelay = estimatedDelay;
        this.riskScore = riskScore;
        this.enrichedTimestamp = enrichedTimestamp;
    }

    // Getters and Setters
    public FlightEventType getEventType() {
        return eventType;
    }

    public void setEventType(FlightEventType eventType) {
        this.eventType = eventType;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public LocalDate getFlightDate() {
        return flightDate;
    }

    public void setFlightDate(LocalDate flightDate) {
        this.flightDate = flightDate;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Integer getEstimatedDelay() {
        return estimatedDelay;
    }

    public void setEstimatedDelay(Integer estimatedDelay) {
        this.estimatedDelay = estimatedDelay;
    }

    public String getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(String riskScore) {
        this.riskScore = riskScore;
    }

    public LocalDateTime getEnrichedTimestamp() {
        return enrichedTimestamp;
    }

    public void setEnrichedTimestamp(LocalDateTime enrichedTimestamp) {
        this.enrichedTimestamp = enrichedTimestamp;
    }

    @Override
    public String toString() {
        return "EnrichedFlightEvent{" +
                "eventType=" + eventType +
                ", flightNumber='" + flightNumber + '\'' +
                ", flightDate=" + flightDate +
                ", data='" + data + '\'' +
                ", estimatedDelay=" + estimatedDelay +
                ", riskScore='" + riskScore + '\'' +
                ", enrichedTimestamp=" + enrichedTimestamp +
                '}';
    }
}
