package com.temporal.jetstream.workflow;

import com.temporal.jetstream.activity.FlightEventActivity;
import com.temporal.jetstream.activity.PersistenceActivity;
import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.model.FlightState;
import com.temporal.jetstream.model.FlightStateTransition;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

public class FlightWorkflowImpl implements FlightWorkflow {

    private static final Logger logger = Workflow.getLogger(FlightWorkflowImpl.class);

    // Timing range (in seconds) for randomized phase durations
    private static final int MIN_PHASE_SECONDS = 15;
    private static final int MAX_PHASE_SECONDS = 45;

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

    // Activity stub for persisting state transitions to MongoDB
    private final PersistenceActivity persistenceActivity = Workflow.newActivityStub(
        PersistenceActivity.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .build()
    );

    @Override
    public Flight executeFlight(Flight flight) {
        // Track current flight for queries
        currentFlight = flight;

        // Initialize gate if provided
        if (flight.getGate() != null) {
            currentGate = flight.getGate();
        }

        // Generate random durations for each phase (deterministic random for replay)
        Random random = Workflow.newRandom();
        Duration scheduledToBoardingDuration = randomDuration(random);
        Duration boardingToDepartedDuration = randomDuration(random);
        Duration departedToInflightDuration = randomDuration(random);
        Duration inflightToLandedDuration = randomDuration(random);
        Duration landedToCompletedDuration = randomDuration(random);

        long totalSeconds = scheduledToBoardingDuration.getSeconds() + boardingToDepartedDuration.getSeconds()
            + departedToInflightDuration.getSeconds() + inflightToLandedDuration.getSeconds()
            + landedToCompletedDuration.getSeconds();
        logger.info("Starting flight workflow for: {} [~{} total]", flight.getFlightNumber(), formatDuration(Duration.ofSeconds(totalSeconds)));

        // SCHEDULED -> BOARDING
        flight.setCurrentState(FlightState.SCHEDULED);
        logger.info("Flight {} is SCHEDULED. Sleeping for {}",
            flight.getFlightNumber(), formatDuration(scheduledToBoardingDuration));
        publishStateTransition(flight, null, FlightState.SCHEDULED);
        Workflow.sleep(scheduledToBoardingDuration);

        // Check for cancellation
        if (cancelled) {
            return handleCancellation(flight);
        }

        // BOARDING
        flight.setCurrentState(FlightState.BOARDING);
        logger.info("Flight {} is BOARDING. Sleeping for {}",
            flight.getFlightNumber(), formatDuration(boardingToDepartedDuration));
        publishStateTransition(flight, FlightState.SCHEDULED, FlightState.BOARDING);
        Workflow.sleep(boardingToDepartedDuration);

        // Check for cancellation
        if (cancelled) {
            return handleCancellation(flight);
        }

        // DEPARTED
        flight.setCurrentState(FlightState.DEPARTED);
        logger.info("Flight {} has DEPARTED. Sleeping for {}",
            flight.getFlightNumber(), formatDuration(departedToInflightDuration));
        publishStateTransition(flight, FlightState.BOARDING, FlightState.DEPARTED);
        Workflow.sleep(departedToInflightDuration);

        // Check for cancellation
        if (cancelled) {
            return handleCancellation(flight);
        }

        // IN_FLIGHT
        flight.setCurrentState(FlightState.IN_FLIGHT);
        logger.info("Flight {} is IN_FLIGHT. Sleeping for {}",
            flight.getFlightNumber(), formatDuration(inflightToLandedDuration));
        publishStateTransition(flight, FlightState.DEPARTED, FlightState.IN_FLIGHT);
        Workflow.sleep(inflightToLandedDuration);

        // Check for cancellation
        if (cancelled) {
            return handleCancellation(flight);
        }

        // LANDED
        flight.setCurrentState(FlightState.LANDED);
        logger.info("Flight {} has LANDED. Sleeping for {}",
            flight.getFlightNumber(), formatDuration(landedToCompletedDuration));
        publishStateTransition(flight, FlightState.IN_FLIGHT, FlightState.LANDED);
        Workflow.sleep(landedToCompletedDuration);

        // COMPLETED
        flight.setCurrentState(FlightState.COMPLETED);
        logger.info("Flight {} is COMPLETED", flight.getFlightNumber());
        publishStateTransition(flight, FlightState.LANDED, FlightState.COMPLETED);

        // Update flight with any signal data received during execution
        updateFlightWithSignalData(flight);

        return flight;
    }

    /**
     * Generates a random duration between MIN_PHASE_SECONDS and MAX_PHASE_SECONDS.
     */
    private Duration randomDuration(Random random) {
        int seconds = MIN_PHASE_SECONDS + random.nextInt(MAX_PHASE_SECONDS - MIN_PHASE_SECONDS + 1);
        return Duration.ofSeconds(seconds);
    }

    /**
     * Formats duration for logging in human-readable format.
     */
    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            return (seconds / 60) + " minutes";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + " hours " + (minutes > 0 ? minutes + " minutes" : "");
        }
    }

    /**
     * Handles flight cancellation and returns the cancelled flight.
     */
    private Flight handleCancellation(Flight flight) {
        FlightState previousState = flight.getCurrentState();
        flight.setCurrentState(FlightState.CANCELLED);
        logger.info("Flight {} was CANCELLED: {}", flight.getFlightNumber(), cancellationReason);
        publishStateTransition(flight, previousState, FlightState.CANCELLED);
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
     * Publishes a state transition event to Kafka and persists to MongoDB via activities.
     */
    private void publishStateTransition(Flight flight, FlightState previousState, FlightState newState) {
        try {
            String prevState = previousState != null ? previousState.toString() : null;
            String gate = currentGate != null ? currentGate : (flight.getGate() != null ? flight.getGate() : "");

            // Publish to Kafka
            flightEventActivity.publishStateChange(
                flight.getFlightNumber(),
                prevState,
                newState.toString(),
                gate,
                delayMinutes
            );

            // Persist to MongoDB
            FlightStateTransition transition = new FlightStateTransition(
                flight.getFlightNumber(),
                flight.getFlightDate(),
                previousState,
                newState,
                LocalDateTime.now(),
                gate,
                delayMinutes,
                flight.getAircraft(),
                "STATE_TRANSITION",
                String.format("Flight transitioned from %s to %s",
                    previousState != null ? previousState : "null", newState)
            );
            persistenceActivity.saveStateTransition(transition);
        } catch (Exception e) {
            logger.warn("Failed to publish state transition: {}", e.getMessage());
            // Don't fail the workflow if publishing fails
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
