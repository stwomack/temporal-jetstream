package com.temporal.jetstream.activity;

import com.temporal.jetstream.model.FlightState;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Activity interface for publishing flight events to Kafka.
 * Activities are called from workflows to perform side effects like publishing to external systems.
 */
@ActivityInterface
public interface FlightEventActivity {

    /**
     * Publishes a flight state change event to Kafka.
     *
     * @param flightNumber   The flight number
     * @param previousState  The previous state
     * @param newState       The new state
     * @param gate           The current gate (optional)
     * @param delayMinutes   The delay in minutes
     */
    @ActivityMethod
    void publishStateChange(String flightNumber, String previousState, String newState,
                          String gate, int delayMinutes);
}
