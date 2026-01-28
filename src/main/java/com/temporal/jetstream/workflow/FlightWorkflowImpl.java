package com.temporal.jetstream.workflow;

import com.temporal.jetstream.activity.FlightEventActivity;
import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.model.FlightState;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class FlightWorkflowImpl implements FlightWorkflow {

    private static final Logger logger = Workflow.getLogger(FlightWorkflowImpl.class);

    // Instance variables to track signal data
    private int delayMinutes = 0;
    private String currentGate = null;
    private boolean cancelled = false;
    private String cancellationReason = null;

    // Instance variable to track current flight state for queries
    private Flight currentFlight = null;

    // Activity stub for publishing state changes to Kafka
    private final FlightEventActivity flightEventActivity = Workflow.newActivityStub(
        FlightEventActivity.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .build()
    );

    @Override
    public Flight executeFlight(Flight flight) {
        logger.info("Starting flight workflow for: {}", flight.getFlightNumber());

        // Track current flight for queries
        currentFlight = flight;

        // Initialize gate if provided
        if (flight.getGate() != null) {
            currentGate = flight.getGate();
        }

        // Determine sleep duration - use longer duration for demo flights
        Duration sleepDuration = flight.getFlightNumber().startsWith("DEMO")
            ? Duration.ofSeconds(240)
            : Duration.ofSeconds(60);

        // SCHEDULED -> BOARDING
        flight.setCurrentState(FlightState.SCHEDULED);
        logger.info("Flight {} is SCHEDULED", flight.getFlightNumber());
        publishStateTransition(flight, null, FlightState.SCHEDULED);
        Workflow.sleep(sleepDuration);

        // Check for cancellation
        if (cancelled) {
            FlightState previousState = flight.getCurrentState();
            flight.setCurrentState(FlightState.CANCELLED);
            logger.info("Flight {} was CANCELLED: {}", flight.getFlightNumber(), cancellationReason);
            publishStateTransition(flight, previousState, FlightState.CANCELLED);
            updateFlightWithSignalData(flight);
            return flight;
        }

        // BOARDING
        flight.setCurrentState(FlightState.BOARDING);
        logger.info("Flight {} is BOARDING", flight.getFlightNumber());
        publishStateTransition(flight, FlightState.SCHEDULED, FlightState.BOARDING);
        Workflow.sleep(sleepDuration);

        // Check for cancellation
        if (cancelled) {
            FlightState previousState = flight.getCurrentState();
            flight.setCurrentState(FlightState.CANCELLED);
            logger.info("Flight {} was CANCELLED: {}", flight.getFlightNumber(), cancellationReason);
            publishStateTransition(flight, previousState, FlightState.CANCELLED);
            updateFlightWithSignalData(flight);
            return flight;
        }

        // DEPARTED
        flight.setCurrentState(FlightState.DEPARTED);
        logger.info("Flight {} has DEPARTED", flight.getFlightNumber());
        publishStateTransition(flight, FlightState.BOARDING, FlightState.DEPARTED);
        Workflow.sleep(sleepDuration);

        // Check for cancellation
        if (cancelled) {
            FlightState previousState = flight.getCurrentState();
            flight.setCurrentState(FlightState.CANCELLED);
            logger.info("Flight {} was CANCELLED: {}", flight.getFlightNumber(), cancellationReason);
            publishStateTransition(flight, previousState, FlightState.CANCELLED);
            updateFlightWithSignalData(flight);
            return flight;
        }

        // IN_FLIGHT
        flight.setCurrentState(FlightState.IN_FLIGHT);
        logger.info("Flight {} is IN_FLIGHT", flight.getFlightNumber());
        publishStateTransition(flight, FlightState.DEPARTED, FlightState.IN_FLIGHT);
        Workflow.sleep(sleepDuration);

        // Check for cancellation
        if (cancelled) {
            FlightState previousState = flight.getCurrentState();
            flight.setCurrentState(FlightState.CANCELLED);
            logger.info("Flight {} was CANCELLED: {}", flight.getFlightNumber(), cancellationReason);
            publishStateTransition(flight, previousState, FlightState.CANCELLED);
            updateFlightWithSignalData(flight);
            return flight;
        }

        // LANDED
        flight.setCurrentState(FlightState.LANDED);
        logger.info("Flight {} has LANDED", flight.getFlightNumber());
        publishStateTransition(flight, FlightState.IN_FLIGHT, FlightState.LANDED);
        Workflow.sleep(sleepDuration);

        // COMPLETED
        flight.setCurrentState(FlightState.COMPLETED);
        logger.info("Flight {} is COMPLETED", flight.getFlightNumber());
        publishStateTransition(flight, FlightState.LANDED, FlightState.COMPLETED);

        // Update flight with any signal data received during execution
        updateFlightWithSignalData(flight);

        return flight;
    }

    @Override
    public void announceDelay(int minutes) {
        delayMinutes = minutes;
        logger.info("Received signal: announceDelay, delay={} minutes", minutes);
    }

    @Override
    public void changeGate(String newGate) {
        currentGate = newGate;
        logger.info("Received signal: changeGate, gate={}", newGate);
    }

    @Override
    public void cancelFlight(String reason) {
        cancelled = true;
        cancellationReason = reason;
        logger.info("Received signal: cancelFlight, reason={}", reason);
    }

    private void updateFlightWithSignalData(Flight flight) {
        if (delayMinutes > 0) {
            flight.setDelay(delayMinutes);
        }
        if (currentGate != null) {
            flight.setGate(currentGate);
        }
    }

    /**
     * Publishes a state transition event to Kafka via activity.
     */
    private void publishStateTransition(Flight flight, FlightState previousState, FlightState newState) {
        try {
            String prevState = previousState != null ? previousState.toString() : null;
            String gate = currentGate != null ? currentGate : (flight.getGate() != null ? flight.getGate() : "");
            flightEventActivity.publishStateChange(
                flight.getFlightNumber(),
                prevState,
                newState.toString(),
                gate,
                delayMinutes
            );
        } catch (Exception e) {
            logger.warn("Failed to publish state transition to Kafka: {}", e.getMessage());
            // Don't fail the workflow if Kafka publishing fails
        }
    }

    @Override
    public FlightState getCurrentState() {
        return currentFlight != null ? currentFlight.getCurrentState() : null;
    }

    @Override
    public Flight getFlightDetails() {
        if (currentFlight == null) {
            return null;
        }
        // Update flight with latest signal data before returning
        Flight details = new Flight();
        details.setFlightNumber(currentFlight.getFlightNumber());
        details.setFlightDate(currentFlight.getFlightDate());
        details.setDepartureStation(currentFlight.getDepartureStation());
        details.setArrivalStation(currentFlight.getArrivalStation());
        details.setScheduledDeparture(currentFlight.getScheduledDeparture());
        details.setScheduledArrival(currentFlight.getScheduledArrival());
        details.setCurrentState(currentFlight.getCurrentState());
        details.setAircraft(currentFlight.getAircraft());
        // Include signal data
        details.setDelay(delayMinutes);
        details.setGate(currentGate != null ? currentGate : currentFlight.getGate());
        return details;
    }

    @Override
    public int getDelayMinutes() {
        return delayMinutes;
    }
}
