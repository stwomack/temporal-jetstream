package com.temporal.jetstream.workflow;

import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.model.FlightState;
import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MultiLegFlightWorkflowImpl implements MultiLegFlightWorkflow {

    private static final Logger logger = Workflow.getLogger(MultiLegFlightWorkflowImpl.class);

    private List<Flight> journeyFlights = new ArrayList<>();
    private int currentLegIndex = 0;
    private boolean journeyCancelled = false;
    private String cancellationReason = null;

    @Override
    public List<Flight> executeJourney(List<Flight> flights) {
        logger.info("Starting multi-leg journey with {} legs", flights.size());

        journeyFlights = new ArrayList<>(flights);

        for (int i = 0; i < flights.size(); i++) {
            currentLegIndex = i;
            Flight currentLeg = flights.get(i);

            logger.info("Starting leg {}/{}: {} -> {}",
                i + 1, flights.size(),
                currentLeg.getDepartureStation(),
                currentLeg.getArrivalStation());

            // Check for journey cancellation before starting leg
            if (journeyCancelled) {
                logger.info("Journey cancelled before leg {}. Reason: {}", i + 1, cancellationReason);
                currentLeg.setCurrentState(FlightState.CANCELLED);
                journeyFlights.set(i, currentLeg);
                // Cancel remaining legs
                for (int j = i + 1; j < flights.size(); j++) {
                    Flight remainingLeg = flights.get(j);
                    remainingLeg.setCurrentState(FlightState.CANCELLED);
                    journeyFlights.set(j, remainingLeg);
                }
                break;
            }

            // Create child workflow for this leg
            ChildWorkflowOptions options = ChildWorkflowOptions.newBuilder()
                .setWorkflowId("flight-" + currentLeg.getFlightNumber() + "-" + currentLeg.getFlightDate())
                .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
                .build();

            FlightWorkflow childWorkflow = Workflow.newChildWorkflowStub(FlightWorkflow.class, options);

            // Execute child workflow and wait for completion
            Flight completedLeg = childWorkflow.executeFlight(currentLeg);
            journeyFlights.set(i, completedLeg);

            logger.info("Leg {}/{} completed with state: {}",
                i + 1, flights.size(), completedLeg.getCurrentState());

            // Handle compensation logic if leg was cancelled
            if (completedLeg.getCurrentState() == FlightState.CANCELLED) {
                logger.warn("Leg {}/{} was cancelled. Cancelling remaining legs.", i + 1, flights.size());

                // Cancel all subsequent legs
                for (int j = i + 1; j < flights.size(); j++) {
                    Flight subsequentLeg = flights.get(j);
                    subsequentLeg.setCurrentState(FlightState.CANCELLED);
                    journeyFlights.set(j, subsequentLeg);
                    logger.info("Cancelled subsequent leg {}/{}: {}",
                        j + 1, flights.size(), subsequentLeg.getFlightNumber());
                }
                break;
            }

            // Update aircraft/crew info for next leg if available
            if (i + 1 < flights.size() && completedLeg.getAircraft() != null) {
                Flight nextLeg = flights.get(i + 1);
                nextLeg.setAircraft(completedLeg.getAircraft());
                logger.info("Transferred aircraft {} from leg {} to leg {}",
                    completedLeg.getAircraft(), i + 1, i + 2);
            }

            // Small delay between legs to simulate turnaround time
            if (i + 1 < flights.size()) {
                logger.info("Turnaround time before next leg...");
                Workflow.sleep(Duration.ofSeconds(1));
            }
        }

        logger.info("Multi-leg journey completed. Final leg index: {}", currentLegIndex);
        return journeyFlights;
    }

    @Override
    public List<Flight> getJourneyStatus() {
        return new ArrayList<>(journeyFlights);
    }

    @Override
    public int getCurrentLegIndex() {
        return currentLegIndex;
    }

    @Override
    public void cancelJourney(String reason) {
        journeyCancelled = true;
        cancellationReason = reason;
        logger.info("Received signal: cancelJourney, reason={}", reason);
    }
}
