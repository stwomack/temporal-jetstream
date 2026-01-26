package com.temporal.jetstream.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Flight {
    private String flightNumber;
    private LocalDate flightDate;
    private String departureStation;
    private String arrivalStation;
    private LocalDateTime scheduledDeparture;
    private LocalDateTime scheduledArrival;
    private FlightState currentState;
    private String gate;
    private String aircraft;
    private int delay; // in minutes

    public Flight() {
        this.currentState = FlightState.SCHEDULED;
        this.delay = 0;
    }

    public Flight(String flightNumber, LocalDate flightDate, String departureStation,
                  String arrivalStation, LocalDateTime scheduledDeparture,
                  LocalDateTime scheduledArrival, String gate, String aircraft) {
        this.flightNumber = flightNumber;
        this.flightDate = flightDate;
        this.departureStation = departureStation;
        this.arrivalStation = arrivalStation;
        this.scheduledDeparture = scheduledDeparture;
        this.scheduledArrival = scheduledArrival;
        this.gate = gate;
        this.aircraft = aircraft;
        this.currentState = FlightState.SCHEDULED;
        this.delay = 0;
    }

    // Getters and Setters
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

    public String getDepartureStation() {
        return departureStation;
    }

    public void setDepartureStation(String departureStation) {
        this.departureStation = departureStation;
    }

    public String getArrivalStation() {
        return arrivalStation;
    }

    public void setArrivalStation(String arrivalStation) {
        this.arrivalStation = arrivalStation;
    }

    public LocalDateTime getScheduledDeparture() {
        return scheduledDeparture;
    }

    public void setScheduledDeparture(LocalDateTime scheduledDeparture) {
        this.scheduledDeparture = scheduledDeparture;
    }

    public LocalDateTime getScheduledArrival() {
        return scheduledArrival;
    }

    public void setScheduledArrival(LocalDateTime scheduledArrival) {
        this.scheduledArrival = scheduledArrival;
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

    public String getAircraft() {
        return aircraft;
    }

    public void setAircraft(String aircraft) {
        this.aircraft = aircraft;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public String toString() {
        return "Flight{" +
                "flightNumber='" + flightNumber + '\'' +
                ", flightDate=" + flightDate +
                ", departureStation='" + departureStation + '\'' +
                ", arrivalStation='" + arrivalStation + '\'' +
                ", currentState=" + currentState +
                ", gate='" + gate + '\'' +
                ", aircraft='" + aircraft + '\'' +
                ", delay=" + delay +
                '}';
    }
}
