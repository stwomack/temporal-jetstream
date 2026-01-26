package com.temporal.jetstream.model;

import java.time.LocalDate;

/**
 * Represents a flight event received from Kafka topics.
 * These events are mapped to workflow signals to drive flight state transitions.
 */
public class FlightEvent {
    private FlightEventType eventType;
    private String flightNumber;
    private LocalDate flightDate;
    private String data; // JSON string containing event-specific data

    public FlightEvent() {
    }

    public FlightEvent(FlightEventType eventType, String flightNumber, LocalDate flightDate, String data) {
        this.eventType = eventType;
        this.flightNumber = flightNumber;
        this.flightDate = flightDate;
        this.data = data;
    }

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

    @Override
    public String toString() {
        return "FlightEvent{" +
                "eventType=" + eventType +
                ", flightNumber='" + flightNumber + '\'' +
                ", flightDate=" + flightDate +
                ", data='" + data + '\'' +
                '}';
    }
}
