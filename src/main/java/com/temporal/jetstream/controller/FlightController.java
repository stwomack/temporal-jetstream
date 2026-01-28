package com.temporal.jetstream.controller;

import com.temporal.jetstream.dto.*;
import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.model.FlightState;
import com.temporal.jetstream.service.FlightEventProducer;
import com.temporal.jetstream.service.FlightEventService;
import com.temporal.jetstream.service.HistoryService;
import com.temporal.jetstream.workflow.FlightWorkflow;
import com.temporal.jetstream.workflow.MultiLegFlightWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
@Tag(name = "Flights", description = "Flight lifecycle management endpoints")
public class FlightController {

    private static final Logger logger = LoggerFactory.getLogger(FlightController.class);

    @Autowired
    private WorkflowClient workflowClient;

    @Autowired
    private FlightEventService flightEventService;

    @Autowired
    private FlightEventProducer flightEventProducer;

    @Autowired
    private HistoryService historyService;

    @Value("${temporal.task-queue}")
    private String taskQueue;

    @Operation(summary = "Start a multi-leg journey", description = "Creates a multi-leg flight journey workflow with linked flight segments")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Journey workflow started successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to start journey workflow")
    })
    @PostMapping("/journey")
    public ResponseEntity<?> startJourney(@Valid @RequestBody StartJourneyRequest request) {
        try {
            // Create Flight objects from request
            List<Flight> flights = new ArrayList<>();
            for (int i = 0; i < request.getFlights().size(); i++) {
                StartFlightRequest flightRequest = request.getFlights().get(i);
                Flight flight = new Flight(
                        flightRequest.getFlightNumber(),
                        flightRequest.getFlightDate(),
                        flightRequest.getDepartureStation(),
                        flightRequest.getArrivalStation(),
                        flightRequest.getScheduledDeparture(),
                        flightRequest.getScheduledArrival(),
                        flightRequest.getGate(),
                        flightRequest.getAircraft()
                );

                // Set linkage between flights
                if (i > 0) {
                    flight.setPreviousFlightNumber(request.getFlights().get(i - 1).getFlightNumber());
                }
                if (i < request.getFlights().size() - 1) {
                    flight.setNextFlightNumber(request.getFlights().get(i + 1).getFlightNumber());
                }

                flights.add(flight);
            }

            // Generate workflow ID
            String workflowId = String.format("journey-%s", request.getJourneyId());

            // Create workflow options
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId(workflowId)
                    .setTaskQueue(taskQueue)
                    .build();

            // Start multi-leg workflow asynchronously
            MultiLegFlightWorkflow workflow = workflowClient.newWorkflowStub(MultiLegFlightWorkflow.class, options);
            WorkflowClient.start(workflow::executeJourney, flights);

            logger.info("Started multi-leg journey workflow: {} with {} legs, ID: {}",
                    request.getJourneyId(), flights.size(), workflowId);

            return ResponseEntity.ok(new StartJourneyResponse(
                    workflowId,
                    request.getJourneyId(),
                    flights.size()
            ));

        } catch (Exception e) {
            logger.error("Error starting journey workflow: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("WORKFLOW_START_ERROR", e.getMessage()));
        }
    }

    @Operation(summary = "Start a single flight workflow", description = "Creates and starts a Temporal workflow for a single flight")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight workflow started successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to start flight workflow")
    })
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

            // Publish event to WebSocket clients
            flightEventService.publishStateChange(request.getFlightNumber(), FlightState.SCHEDULED, "Flight workflow started");

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

    @Operation(summary = "Announce a flight delay", description = "Signals the flight workflow with a delay in minutes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delay announced successfully"),
            @ApiResponse(responseCode = "404", description = "Flight workflow not found"),
            @ApiResponse(responseCode = "500", description = "Failed to announce delay")
    })
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

            // Query workflow for current state
            FlightWorkflow queryWorkflow = getWorkflowStub(workflowId);
            Flight updatedFlight = queryWorkflow.getFlightDetails();

            // Publish event to WebSocket clients
            flightEventService.publishFlightUpdate(updatedFlight);

            // Publish event to Kafka
            flightEventProducer.publishStateChange(
                flightNumber,
                updatedFlight.getCurrentState(),
                updatedFlight.getCurrentState(),
                updatedFlight.getGate(),
                request.getMinutes()
            );

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

    @Operation(summary = "Change the gate assignment", description = "Signals the flight workflow with a new gate assignment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gate changed successfully"),
            @ApiResponse(responseCode = "404", description = "Flight workflow not found"),
            @ApiResponse(responseCode = "500", description = "Failed to change gate")
    })
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

            // Query workflow for current state
            FlightWorkflow queryWorkflow = getWorkflowStub(workflowId);
            Flight updatedFlight = queryWorkflow.getFlightDetails();

            // Publish event to WebSocket clients
            flightEventService.publishFlightUpdate(updatedFlight);

            // Publish event to Kafka
            flightEventProducer.publishStateChange(
                flightNumber,
                updatedFlight.getCurrentState(),
                updatedFlight.getCurrentState(),
                request.getNewGate(),
                updatedFlight.getDelay()
            );

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

    @Operation(summary = "Cancel a flight", description = "Signals the flight workflow to cancel with a reason")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Flight workflow not found"),
            @ApiResponse(responseCode = "500", description = "Failed to cancel flight")
    })
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

            // Query workflow for current state
            FlightWorkflow queryWorkflow = getWorkflowStub(workflowId);
            Flight updatedFlight = queryWorkflow.getFlightDetails();

            // Publish event to WebSocket clients
            flightEventService.publishStateChange(flightNumber, FlightState.CANCELLED, "Flight cancelled: " + request.getReason());

            // Publish event to Kafka
            flightEventProducer.publishStateChange(
                flightNumber,
                updatedFlight.getCurrentState(),
                FlightState.CANCELLED,
                updatedFlight.getGate(),
                updatedFlight.getDelay()
            );

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

    @Operation(summary = "Get flight state", description = "Queries the current state of a flight workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight state retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Flight workflow not found"),
            @ApiResponse(responseCode = "500", description = "Failed to query flight state")
    })
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

    @Operation(summary = "Get flight details", description = "Queries the full details of a flight workflow including all flight data")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Flight workflow not found"),
            @ApiResponse(responseCode = "500", description = "Failed to query flight details")
    })
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

    @Operation(summary = "Get flight workflow history", description = "Retrieves the Temporal workflow event history for a flight")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Flight workflow not found"),
            @ApiResponse(responseCode = "500", description = "Failed to retrieve flight history")
    })
    @GetMapping("/{flightNumber}/history")
    public ResponseEntity<?> getFlightHistory(
            @PathVariable String flightNumber,
            @RequestParam(required = false) String flightDate) {
        try {
            String workflowId = buildWorkflowId(flightNumber, flightDate);

            List<WorkflowHistoryEvent> history = historyService.getWorkflowHistory(workflowId);

            logger.info("Retrieved {} history events for flight {}", history.size(), flightNumber);

            return ResponseEntity.ok(history);

        } catch (WorkflowNotFoundException e) {
            logger.error("Workflow not found for flight: {}", flightNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("WORKFLOW_NOT_FOUND", "Flight not found: " + flightNumber));
        } catch (Exception e) {
            logger.error("Error retrieving history for flight {}: {}", flightNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("HISTORY_ERROR", e.getMessage()));
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
