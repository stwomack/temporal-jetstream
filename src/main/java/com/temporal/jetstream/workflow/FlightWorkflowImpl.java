package com.temporal.jetstream.workflow;

import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.model.FlightState;
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

    @Override
    public Flight executeFlight(Flight flight) {
        logger.info("Starting flight workflow for: {}", flight.getFlightNumber());

        // Track current flight for queries
        currentFlight = flight;

        // Initialize gate if provided
        if (flight.getGate() != null) {
            currentGate = flight.getGate();
        }

        // SCHEDULED -> BOARDING
        flight.setCurrentState(FlightState.SCHEDULED);
        logger.info("Flight {} is SCHEDULED", flight.getFlightNumber());
        Workflow.sleep(Duration.ofSeconds(2));

        // Check for cancellation
        if (cancelled) {
            flight.setCurrentState(FlightState.CANCELLED);
            logger.info("Flight {} was CANCELLED: {}", flight.getFlightNumber(), cancellationReason);
            updateFlightWithSignalData(flight);
            return flight;
        }

        // BOARDING
        flight.setCurrentState(FlightState.BOARDING);
        logger.info("Flight {} is BOARDING", flight.getFlightNumber());
        Workflow.sleep(Duration.ofSeconds(2));

        // Check for cancellation
        if (cancelled) {
            flight.setCurrentState(FlightState.CANCELLED);
            logger.info("Flight {} was CANCELLED: {}", flight.getFlightNumber(), cancellationReason);
            updateFlightWithSignalData(flight);
            return flight;
        }

        // DEPARTED
        flight.setCurrentState(FlightState.DEPARTED);
        logger.info("Flight {} has DEPARTED", flight.getFlightNumber());
        Workflow.sleep(Duration.ofSeconds(2));

        // Check for cancellation
        if (cancelled) {
            flight.setCurrentState(FlightState.CANCELLED);
            logger.info("Flight {} was CANCELLED: {}", flight.getFlightNumber(), cancellationReason);
            updateFlightWithSignalData(flight);
            return flight;
        }

        // IN_FLIGHT
        flight.setCurrentState(FlightState.IN_FLIGHT);
        logger.info("Flight {} is IN_FLIGHT", flight.getFlightNumber());
        Workflow.sleep(Duration.ofSeconds(2));

        // Check for cancellation
        if (cancelled) {
            flight.setCurrentState(FlightState.CANCELLED);
            logger.info("Flight {} was CANCELLED: {}", flight.getFlightNumber(), cancellationReason);
            updateFlightWithSignalData(flight);
            return flight;
        }

        // LANDED
        flight.setCurrentState(FlightState.LANDED);
        logger.info("Flight {} has LANDED", flight.getFlightNumber());
        Workflow.sleep(Duration.ofSeconds(2));

        // COMPLETED
        flight.setCurrentState(FlightState.COMPLETED);
        logger.info("Flight {} is COMPLETED", flight.getFlightNumber());

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
