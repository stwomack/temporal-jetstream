package com.temporal.jetstream.dto;

import com.temporal.jetstream.model.FlightState;

public class FlightStateResponse {
    private String flightNumber;
    private FlightState currentState;

    public FlightStateResponse() {
    }

    public FlightStateResponse(String flightNumber, FlightState currentState) {
        this.flightNumber = flightNumber;
        this.currentState = currentState;
    }

    // Getters and Setters
    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public FlightState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(FlightState currentState) {
        this.currentState = currentState;
    }
}
