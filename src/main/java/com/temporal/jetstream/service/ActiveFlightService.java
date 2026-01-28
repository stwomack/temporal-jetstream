package com.temporal.jetstream.service;

import com.temporal.jetstream.dto.ActiveFlightDTO;
import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.workflow.FlightWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ActiveFlightService {

    private static final Logger logger = LoggerFactory.getLogger(ActiveFlightService.class);

    @Autowired
    private WorkflowClient workflowClient;

    @Autowired
    private WorkflowServiceStubs workflowServiceStubs;

    /**
     * Lists all active (running) flight workflows by querying Temporal for workflows with RUNNING status
     * @return List of active flights with their current state
     */
    public List<ActiveFlightDTO> getActiveFlights() {
        List<ActiveFlightDTO> activeFlights = new ArrayList<>();

        try {
            // Get namespace from WorkflowClient
            String namespace = workflowClient.getOptions().getNamespace();

            // Build query to find RUNNING workflows for FlightWorkflow type
            String query = "WorkflowType='FlightWorkflow' AND ExecutionStatus='Running'";

            // Create list request
            ListWorkflowExecutionsRequest request = ListWorkflowExecutionsRequest.newBuilder()
                    .setNamespace(namespace)
                    .setQuery(query)
                    .setPageSize(50)
                    .build();

            // Execute list query
            ListWorkflowExecutionsResponse response = workflowServiceStubs.blockingStub()
                    .listWorkflowExecutions(request);

            logger.info("Found {} running flight workflows", response.getExecutionsCount());

            // Process each workflow execution
            for (WorkflowExecutionInfo executionInfo : response.getExecutionsList()) {
                try {
                    WorkflowExecution execution = executionInfo.getExecution();
                    String workflowId = execution.getWorkflowId();

                    // Skip if not a flight workflow ID (should start with "flight-")
                    if (!workflowId.startsWith("flight-")) {
                        continue;
                    }

                    // Extract flight number from workflow ID (format: flight-AA1234-2026-01-27)
                    String flightNumber = extractFlightNumber(workflowId);

                    // Get workflow stub to query current state
                    FlightWorkflow workflow = workflowClient.newWorkflowStub(FlightWorkflow.class, workflowId);

                    // Query workflow for current details
                    Flight flightDetails = workflow.getFlightDetails();

                    // Calculate elapsed time
                    Instant startTime = Instant.ofEpochSecond(
                            executionInfo.getStartTime().getSeconds(),
                            executionInfo.getStartTime().getNanos()
                    );
                    Duration elapsedTime = Duration.between(startTime, Instant.now());

                    // Create DTO
                    ActiveFlightDTO activeFlightDTO = new ActiveFlightDTO(
                            workflowId,
                            flightNumber,
                            flightDetails.getCurrentState(),
                            flightDetails.getGate(),
                            flightDetails.getDelay(),
                            startTime,
                            elapsedTime,
                            flightDetails.isDemoMode()
                    );

                    activeFlights.add(activeFlightDTO);

                } catch (Exception e) {
                    logger.error("Error querying workflow {}: {}",
                            executionInfo.getExecution().getWorkflowId(),
                            e.getMessage());
                    // Continue processing other workflows
                }
            }

            logger.info("Successfully retrieved {} active flights", activeFlights.size());

        } catch (Exception e) {
            logger.error("Error listing active flights: {}", e.getMessage(), e);
        }

        return activeFlights;
    }

    /**
     * Extracts flight number from workflow ID
     * Expected format: flight-AA1234-2026-01-27
     * Returns: AA1234
     */
    private String extractFlightNumber(String workflowId) {
        String[] parts = workflowId.split("-");
        if (parts.length >= 2) {
            return parts[1]; // Return AA1234
        }
        return workflowId; // Fallback if format is unexpected
    }
}
