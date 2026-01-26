package com.temporal.jetstream.model;

/**
 * Enum representing types of flight events that can be received from Kafka.
 * These map to signals in the FlightWorkflow.
 */
public enum FlightEventType {
    FLIGHT_SCHEDULED,
    GATE_ASSIGNED,
    GATE_CHANGED,
    DELAY_ANNOUNCED,
    BOARDING_STARTED,
    DEPARTURE,
    ARRIVAL,
    FLIGHT_CANCELLED
}
