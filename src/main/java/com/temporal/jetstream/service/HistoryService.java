package com.temporal.jetstream.service;

import com.temporal.jetstream.dto.WorkflowHistoryEvent;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.history.v1.History;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryRequest;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for fetching and formatting workflow execution history.
 * Provides audit trail functionality for compliance and debugging.
 */
@Service
public class HistoryService {

    private static final Logger logger = LoggerFactory.getLogger(HistoryService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final WorkflowClient workflowClient;
    private final WorkflowServiceStubs serviceStubs;

    public HistoryService(WorkflowClient workflowClient, WorkflowServiceStubs serviceStubs) {
        this.workflowClient = workflowClient;
        this.serviceStubs = serviceStubs;
    }

    /**
     * Fetches the complete workflow history for a given workflow ID.
     *
     * @param workflowId The workflow ID (e.g., "flight-AA1234-2026-01-26")
     * @return List of formatted history events
     */
    public List<WorkflowHistoryEvent> getWorkflowHistory(String workflowId) {
        logger.info("Fetching workflow history for workflowId: {}", workflowId);

        try {
            // Get namespace from WorkflowClient options
            String namespace = workflowClient.getOptions().getNamespace();

            // Create workflow execution reference
            WorkflowExecution execution = WorkflowExecution.newBuilder()
                .setWorkflowId(workflowId)
                .build();

            // Build request to get workflow history
            GetWorkflowExecutionHistoryRequest request = GetWorkflowExecutionHistoryRequest.newBuilder()
                .setNamespace(namespace)
                .setExecution(execution)
                .build();

            // Fetch history from Temporal Server
            GetWorkflowExecutionHistoryResponse response =
                serviceStubs.blockingStub().getWorkflowExecutionHistory(request);

            History history = response.getHistory();
            List<WorkflowHistoryEvent> events = new ArrayList<>();

            // Parse and format history events
            for (HistoryEvent event : history.getEventsList()) {
                WorkflowHistoryEvent formattedEvent = formatHistoryEvent(event);
                if (formattedEvent != null) {
                    events.add(formattedEvent);
                }
            }

            logger.info("Retrieved {} history events for workflowId: {}", events.size(), workflowId);
            return events;

        } catch (Exception e) {
            logger.error("Error fetching workflow history for workflowId: {}", workflowId, e);
            throw new RuntimeException("Failed to fetch workflow history: " + e.getMessage(), e);
        }
    }

    /**
     * Formats a raw HistoryEvent into a human-readable WorkflowHistoryEvent.
     */
    private WorkflowHistoryEvent formatHistoryEvent(HistoryEvent event) {
        EventType eventType = event.getEventType();
        long eventId = event.getEventId();

        // Convert timestamp (nanoseconds since epoch) to readable format
        long timestampNanos = event.getEventTime().getSeconds() * 1_000_000_000L +
                             event.getEventTime().getNanos();
        Instant instant = Instant.ofEpochSecond(
            event.getEventTime().getSeconds(),
            event.getEventTime().getNanos()
        );
        String timestamp = DATE_TIME_FORMATTER.format(instant);

        // Create human-readable description based on event type
        String description = createEventDescription(event);
        String category = categorizeEvent(eventType);

        return WorkflowHistoryEvent.builder()
            .eventId(eventId)
            .eventType(eventType.name())
            .timestamp(timestamp)
            .description(description)
            .category(category)
            .build();
    }

    /**
     * Creates a human-readable description for each event type.
     */
    private String createEventDescription(HistoryEvent event) {
        EventType type = event.getEventType();

        switch (type) {
            case EVENT_TYPE_WORKFLOW_EXECUTION_STARTED:
                return "Workflow execution started";

            case EVENT_TYPE_WORKFLOW_EXECUTION_COMPLETED:
                return "Workflow execution completed successfully";

            case EVENT_TYPE_WORKFLOW_EXECUTION_FAILED:
                return "Workflow execution failed";

            case EVENT_TYPE_WORKFLOW_TASK_SCHEDULED:
                return "Workflow task scheduled";

            case EVENT_TYPE_WORKFLOW_TASK_STARTED:
                return "Workflow task started processing";

            case EVENT_TYPE_WORKFLOW_TASK_COMPLETED:
                return "Workflow task completed";

            case EVENT_TYPE_TIMER_STARTED:
                return "Timer started (state transition delay)";

            case EVENT_TYPE_TIMER_FIRED:
                return "Timer fired (state transition triggered)";

            case EVENT_TYPE_WORKFLOW_EXECUTION_SIGNALED:
                String signalName = event.getWorkflowExecutionSignaledEventAttributes().getSignalName();
                return String.format("Signal received: %s", signalName);

            case EVENT_TYPE_MARKER_RECORDED:
                return "Workflow marker recorded";

            case EVENT_TYPE_START_CHILD_WORKFLOW_EXECUTION_INITIATED:
                return "Child workflow start initiated";

            case EVENT_TYPE_CHILD_WORKFLOW_EXECUTION_STARTED:
                return "Child workflow execution started";

            case EVENT_TYPE_CHILD_WORKFLOW_EXECUTION_COMPLETED:
                return "Child workflow execution completed";

            case EVENT_TYPE_WORKFLOW_EXECUTION_CANCELED:
                return "Workflow execution canceled";

            case EVENT_TYPE_WORKFLOW_EXECUTION_TERMINATED:
                return "Workflow execution terminated";

            default:
                return type.name().replace("EVENT_TYPE_", "").replace("_", " ").toLowerCase();
        }
    }

    /**
     * Categorizes events into logical groups for UI display.
     */
    private String categorizeEvent(EventType type) {
        switch (type) {
            case EVENT_TYPE_WORKFLOW_EXECUTION_STARTED:
            case EVENT_TYPE_WORKFLOW_EXECUTION_COMPLETED:
            case EVENT_TYPE_WORKFLOW_EXECUTION_FAILED:
            case EVENT_TYPE_WORKFLOW_EXECUTION_CANCELED:
            case EVENT_TYPE_WORKFLOW_EXECUTION_TERMINATED:
                return "lifecycle";

            case EVENT_TYPE_WORKFLOW_EXECUTION_SIGNALED:
                return "signal";

            case EVENT_TYPE_TIMER_STARTED:
            case EVENT_TYPE_TIMER_FIRED:
                return "timer";

            case EVENT_TYPE_START_CHILD_WORKFLOW_EXECUTION_INITIATED:
            case EVENT_TYPE_CHILD_WORKFLOW_EXECUTION_STARTED:
            case EVENT_TYPE_CHILD_WORKFLOW_EXECUTION_COMPLETED:
                return "child_workflow";

            case EVENT_TYPE_WORKFLOW_TASK_SCHEDULED:
            case EVENT_TYPE_WORKFLOW_TASK_STARTED:
            case EVENT_TYPE_WORKFLOW_TASK_COMPLETED:
                return "task";

            default:
                return "other";
        }
    }
}
