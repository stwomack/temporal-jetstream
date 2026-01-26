package com.temporal.jetstream.workflow;

import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.model.FlightState;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class FlightWorkflowImpl implements FlightWorkflow {

    private static final Logger logger = Workflow.getLogger(FlightWorkflowImpl.class);

    @Override
    public Flight executeFlight(Flight flight) {
        logger.info("Starting flight workflow for: {}", flight.getFlightNumber());

        // SCHEDULED -> BOARDING
        flight.setCurrentState(FlightState.SCHEDULED);
        logger.info("Flight {} is SCHEDULED", flight.getFlightNumber());
        Workflow.sleep(Duration.ofSeconds(2));

        // BOARDING
        flight.setCurrentState(FlightState.BOARDING);
        logger.info("Flight {} is BOARDING", flight.getFlightNumber());
        Workflow.sleep(Duration.ofSeconds(2));

        // DEPARTED
        flight.setCurrentState(FlightState.DEPARTED);
        logger.info("Flight {} has DEPARTED", flight.getFlightNumber());
        Workflow.sleep(Duration.ofSeconds(2));

        // IN_FLIGHT
        flight.setCurrentState(FlightState.IN_FLIGHT);
        logger.info("Flight {} is IN_FLIGHT", flight.getFlightNumber());
        Workflow.sleep(Duration.ofSeconds(2));

        // LANDED
        flight.setCurrentState(FlightState.LANDED);
        logger.info("Flight {} has LANDED", flight.getFlightNumber());
        Workflow.sleep(Duration.ofSeconds(2));

        // COMPLETED
        flight.setCurrentState(FlightState.COMPLETED);
        logger.info("Flight {} is COMPLETED", flight.getFlightNumber());

        return flight;
    }
}
