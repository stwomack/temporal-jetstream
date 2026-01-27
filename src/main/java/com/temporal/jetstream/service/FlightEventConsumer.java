package com.temporal.jetstream.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.temporal.jetstream.model.FlightEvent;
import com.temporal.jetstream.model.FlightEventType;
import com.temporal.jetstream.workflow.FlightWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Kafka consumer service that listens to flight events and sends appropriate signals to workflows.
 * Demonstrates integration between Kafka streaming architecture and Temporal orchestration.
 */
@Service
public class FlightEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(FlightEventConsumer.class);
    private static final String TOPIC = "flight-events";

    private final WorkflowClient workflowClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public FlightEventConsumer(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Listens to flight-events topic and processes incoming flight events.
     * Each event is deserialized and mapped to the appropriate workflow signal.
     *
     * @param message JSON message from Kafka topic
     */
    @KafkaListener(topics = TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consumeFlightEvent(String message) {
        logger.info("Received Kafka message: {}", message);

        try {
            // Deserialize the flight event
            FlightEvent event = deserializeFlightEvent(message);
            logger.info("Received Kafka event: {} for flight {}", event.getEventType(), event.getFlightNumber());

            // Send signal to workflow based on event type
            sendSignalToWorkflow(event);

        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize Kafka message: {}", message, e);
        } catch (Exception e) {
            logger.error("Failed to process Kafka event: {}", message, e);
        }
    }

    /**
     * Deserializes JSON message into FlightEvent object.
     */
    private FlightEvent deserializeFlightEvent(String message) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(message);

        FlightEventType eventType = FlightEventType.valueOf(rootNode.get("eventType").asText());
        String flightNumber = rootNode.get("flightNumber").asText();
        LocalDate flightDate = LocalDate.parse(rootNode.get("flightDate").asText());
        String data = rootNode.has("data") ? rootNode.get("data").asText() : null;

        return new FlightEvent(eventType, flightNumber, flightDate, data);
    }

    /**
     * Maps Kafka event types to workflow signals and sends them to the appropriate workflow.
     */
    private void sendSignalToWorkflow(FlightEvent event) {
        try {
            String workflowId = buildWorkflowId(event.getFlightNumber(), event.getFlightDate());
            FlightWorkflow workflow = workflowClient.newWorkflowStub(FlightWorkflow.class, workflowId);

            switch (event.getEventType()) {
                case DELAY_ANNOUNCED -> {
                    int delayMinutes = extractDelayMinutes(event.getData());
                    workflow.announceDelay(delayMinutes);
                    logger.info("Sent announceDelay signal to workflow {} with {} minutes", workflowId, delayMinutes);
                }
                case GATE_CHANGED -> {
                    String newGate = extractGate(event.getData());
                    workflow.changeGate(newGate);
                    logger.info("Sent changeGate signal to workflow {} with gate {}", workflowId, newGate);
                }
                case FLIGHT_CANCELLED -> {
                    String reason = extractReason(event.getData());
                    workflow.cancelFlight(reason);
                    logger.info("Sent cancelFlight signal to workflow {} with reason: {}", workflowId, reason);
                }
                case GATE_ASSIGNED -> {
                    String gate = extractGate(event.getData());
                    workflow.changeGate(gate);
                    logger.info("Sent changeGate signal to workflow {} for gate assignment: {}", workflowId, gate);
                }
                default -> logger.warn("Event type {} not mapped to any signal", event.getEventType());
            }

        } catch (WorkflowNotFoundException e) {
            logger.error("Workflow not found for flight {} on {}", event.getFlightNumber(), event.getFlightDate());
        } catch (Exception e) {
            logger.error("Failed to send signal to workflow for event: {}", event, e);
        }
    }

    /**
     * Builds workflow ID in the format: flight-{flightNumber}-{flightDate}
     */
    private String buildWorkflowId(String flightNumber, LocalDate flightDate) {
        return "flight-" + flightNumber + "-" + flightDate;
    }

    /**
     * Extracts delay minutes from event data JSON.
     */
    private int extractDelayMinutes(String data) {
        try {
            JsonNode dataNode = objectMapper.readTree(data);
            return dataNode.has("delayMinutes") ? dataNode.get("delayMinutes").asInt() : 0;
        } catch (Exception e) {
            logger.warn("Failed to extract delayMinutes from data: {}", data, e);
            return 0;
        }
    }

    /**
     * Extracts gate from event data JSON.
     */
    private String extractGate(String data) {
        try {
            JsonNode dataNode = objectMapper.readTree(data);
            return dataNode.has("gate") ? dataNode.get("gate").asText() : "";
        } catch (Exception e) {
            logger.warn("Failed to extract gate from data: {}", data, e);
            return "";
        }
    }

    /**
     * Extracts cancellation reason from event data JSON.
     */
    private String extractReason(String data) {
        try {
            JsonNode dataNode = objectMapper.readTree(data);
            return dataNode.has("reason") ? dataNode.get("reason").asText() : "Unknown reason";
        } catch (Exception e) {
            logger.warn("Failed to extract reason from data: {}", data, e);
            return "Unknown reason";
        }
    }
}
