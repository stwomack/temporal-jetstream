package com.temporal.jetstream.controller;

import com.temporal.jetstream.dto.*;
import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.model.FlightState;
import com.temporal.jetstream.workflow.FlightWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private static final Logger logger = LoggerFactory.getLogger(FlightController.class);

    @Autowired
    private WorkflowClient workflowClient;

    @Value("${temporal.task-queue}")
    private String taskQueue;

    @PostMapping("/start")
    public ResponseEntity<?> startFlight(@Valid @RequestBody StartFlightRequest request) {
        try {
            // Create Flight object from request
            Flight flight = new Flight(
                    request.getFlightNumber(),
                    request.getFlightDate(),
                    request.getDepartureStation(),
                    request.getArrivalStation(),
                    request.getScheduledDeparture(),
                    request.getScheduledArrival(),
                    request.getGate(),
                    request.getAircraft()
            );

            // Generate workflow ID
            String workflowId = String.format("flight-%s-%s",
                    request.getFlightNumber(),
                    request.getFlightDate().toString());

            // Create workflow options
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId(workflowId)
                    .setTaskQueue(taskQueue)
                    .build();

            // Start workflow asynchronously
            FlightWorkflow workflow = workflowClient.newWorkflowStub(FlightWorkflow.class, options);
            WorkflowClient.start(workflow::executeFlight, flight);

            logger.info("Started flight workflow: {} with ID: {}", request.getFlightNumber(), workflowId);

            return ResponseEntity.ok(new StartFlightResponse(
                    workflowId,
                    request.getFlightNumber(),
                    "Flight workflow started successfully"
            ));

        } catch (Exception e) {
            logger.error("Error starting flight workflow: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("WORKFLOW_START_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/{flightNumber}/delay")
    public ResponseEntity<?> announceDelay(
            @PathVariable String flightNumber,
            @RequestParam(required = false) String flightDate,
            @Valid @RequestBody AnnounceDelayRequest request) {
        try {
            String workflowId = buildWorkflowId(flightNumber, flightDate);
            FlightWorkflow workflow = getWorkflowStub(workflowId);

            workflow.announceDelay(request.getMinutes());

            logger.info("Sent delay signal to flight {}: {} minutes", flightNumber, request.getMinutes());

            return ResponseEntity.ok()
                    .body(new ErrorResponse("SUCCESS", String.format("Delay of %d minutes announced", request.getMinutes())));

        } catch (WorkflowNotFoundException e) {
            logger.error("Workflow not found for flight: {}", flightNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("WORKFLOW_NOT_FOUND", "Flight not found: " + flightNumber));
        } catch (Exception e) {
            logger.error("Error announcing delay for flight {}: {}", flightNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SIGNAL_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/{flightNumber}/gate")
    public ResponseEntity<?> changeGate(
            @PathVariable String flightNumber,
            @RequestParam(required = false) String flightDate,
            @Valid @RequestBody ChangeGateRequest request) {
        try {
            String workflowId = buildWorkflowId(flightNumber, flightDate);
            FlightWorkflow workflow = getWorkflowStub(workflowId);

            workflow.changeGate(request.getNewGate());

            logger.info("Sent gate change signal to flight {}: {}", flightNumber, request.getNewGate());

            return ResponseEntity.ok()
                    .body(new ErrorResponse("SUCCESS", String.format("Gate changed to %s", request.getNewGate())));

        } catch (WorkflowNotFoundException e) {
            logger.error("Workflow not found for flight: {}", flightNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("WORKFLOW_NOT_FOUND", "Flight not found: " + flightNumber));
        } catch (Exception e) {
            logger.error("Error changing gate for flight {}: {}", flightNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SIGNAL_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/{flightNumber}/cancel")
    public ResponseEntity<?> cancelFlight(
            @PathVariable String flightNumber,
            @RequestParam(required = false) String flightDate,
            @Valid @RequestBody CancelFlightRequest request) {
        try {
            String workflowId = buildWorkflowId(flightNumber, flightDate);
            FlightWorkflow workflow = getWorkflowStub(workflowId);

            workflow.cancelFlight(request.getReason());

            logger.info("Sent cancel signal to flight {}: {}", flightNumber, request.getReason());

            return ResponseEntity.ok()
                    .body(new ErrorResponse("SUCCESS", "Flight cancelled: " + request.getReason()));

        } catch (WorkflowNotFoundException e) {
            logger.error("Workflow not found for flight: {}", flightNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("WORKFLOW_NOT_FOUND", "Flight not found: " + flightNumber));
        } catch (Exception e) {
            logger.error("Error cancelling flight {}: {}", flightNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SIGNAL_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/{flightNumber}/state")
    public ResponseEntity<?> getFlightState(
            @PathVariable String flightNumber,
            @RequestParam(required = false) String flightDate) {
        try {
            String workflowId = buildWorkflowId(flightNumber, flightDate);
            FlightWorkflow workflow = getWorkflowStub(workflowId);

            FlightState state = workflow.getCurrentState();

            logger.info("Queried state for flight {}: {}", flightNumber, state);

            return ResponseEntity.ok(new FlightStateResponse(flightNumber, state));

        } catch (WorkflowNotFoundException e) {
            logger.error("Workflow not found for flight: {}", flightNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("WORKFLOW_NOT_FOUND", "Flight not found: " + flightNumber));
        } catch (Exception e) {
            logger.error("Error querying state for flight {}: {}", flightNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("QUERY_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/{flightNumber}/details")
    public ResponseEntity<?> getFlightDetails(
            @PathVariable String flightNumber,
            @RequestParam(required = false) String flightDate) {
        try {
            String workflowId = buildWorkflowId(flightNumber, flightDate);
            FlightWorkflow workflow = getWorkflowStub(workflowId);

            Flight flight = workflow.getFlightDetails();

            logger.info("Queried details for flight {}: {}", flightNumber, flight.getCurrentState());

            return ResponseEntity.ok(flight);

        } catch (WorkflowNotFoundException e) {
            logger.error("Workflow not found for flight: {}", flightNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("WORKFLOW_NOT_FOUND", "Flight not found: " + flightNumber));
        } catch (Exception e) {
            logger.error("Error querying details for flight {}: {}", flightNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("QUERY_ERROR", e.getMessage()));
        }
    }

    private String buildWorkflowId(String flightNumber, String flightDate) {
        // If no date provided, use today's date
        String date = (flightDate != null) ? flightDate : java.time.LocalDate.now().toString();
        return String.format("flight-%s-%s", flightNumber, date);
    }

    private FlightWorkflow getWorkflowStub(String workflowId) {
        return workflowClient.newWorkflowStub(FlightWorkflow.class, workflowId);
    }
}
