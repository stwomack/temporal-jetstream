package com.temporal.jetstream.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a flight state transition stored in MongoDB for historical analysis.
 * Provides business state history separate from Temporal's workflow execution history.
 */
@Document(collection = "flight_state_transitions")
public class FlightStateTransition {

    @Id
    private String id;

    @Indexed
    private String flightNumber;

    @Indexed
    private LocalDate flightDate;

    private FlightState fromState;

    private FlightState toState;

    @Indexed
    private LocalDateTime timestamp;

    private String gate;

    private int delay;

    private String aircraft;

    private String eventType;

    private String eventDetails;

    public FlightStateTransition() {
    }

    public FlightStateTransition(String flightNumber, LocalDate flightDate, FlightState fromState,
                                FlightState toState, LocalDateTime timestamp, String gate,
                                int delay, String aircraft, String eventType, String eventDetails) {
        this.flightNumber = flightNumber;
        this.flightDate = flightDate;
        this.fromState = fromState;
        this.toState = toState;
        this.timestamp = timestamp;
        this.gate = gate;
        this.delay = delay;
        this.aircraft = aircraft;
        this.eventType = eventType;
        this.eventDetails = eventDetails;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public FlightState getFromState() {
        return fromState;
    }

    public void setFromState(FlightState fromState) {
        this.fromState = fromState;
    }

    public FlightState getToState() {
        return toState;
    }

    public void setToState(FlightState toState) {
        this.toState = toState;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getGate() {
        return gate;
    }

    public void setGate(String gate) {
        this.gate = gate;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public String getAircraft() {
        return aircraft;
    }

    public void setAircraft(String aircraft) {
        this.aircraft = aircraft;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventDetails() {
        return eventDetails;
    }

    public void setEventDetails(String eventDetails) {
        this.eventDetails = eventDetails;
    }

    @Override
    public String toString() {
        return "FlightStateTransition{" +
                "id='" + id + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", flightDate=" + flightDate +
                ", fromState=" + fromState +
                ", toState=" + toState +
                ", timestamp=" + timestamp +
                ", gate='" + gate + '\'' +
                ", delay=" + delay +
                ", aircraft='" + aircraft + '\'' +
                ", eventType='" + eventType + '\'' +
                ", eventDetails='" + eventDetails + '\'' +
                '}';
    }
}
