package com.temporal.jetstream.dto;

import com.temporal.jetstream.model.FlightState;

import java.time.Duration;
import java.time.Instant;

public class ActiveFlightDTO {
    private String workflowId;
    private String flightNumber;
    private FlightState currentState;
    private String gate;
    private int delay;
    private Instant startTime;
    private Duration elapsedTime;

    public ActiveFlightDTO() {
    }

    public ActiveFlightDTO(String workflowId, String flightNumber, FlightState currentState,
                          String gate, int delay, Instant startTime, Duration elapsedTime) {
        this.workflowId = workflowId;
        this.flightNumber = flightNumber;
        this.currentState = currentState;
        this.gate = gate;
        this.delay = delay;
        this.startTime = startTime;
        this.elapsedTime = elapsedTime;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

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

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Duration getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(Duration elapsedTime) {
        this.elapsedTime = elapsedTime;
    }
}
