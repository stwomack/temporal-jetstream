package com.temporal.jetstream.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporal.jetstream.model.FlightState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for publishing flight state change events to Kafka.
 * Publishes to 'flight-state-changes' topic whenever a flight transitions states.
 */
@Service
public class FlightEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(FlightEventProducer.class);
    private static final String TOPIC = "flight-state-changes";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Publishes a flight state change event to Kafka.
     *
     * @param flightNumber   The flight number
     * @param previousState  The previous state
     * @param newState       The new state
     * @param gate           The current gate (optional)
     * @param delayMinutes   The delay in minutes
     */
    public void publishStateChange(String flightNumber, FlightState previousState, FlightState newState,
                                   String gate, int delayMinutes) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("flightNumber", flightNumber);
            event.put("previousState", previousState != null ? previousState.toString() : null);
            event.put("newState", newState.toString());
            event.put("timestamp", LocalDateTime.now().toString());
            event.put("gate", gate);
            event.put("delay", delayMinutes);

            String eventJson = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(TOPIC, flightNumber, eventJson)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("Published state change to Kafka: {} -> {} for flight {}",
                            previousState, newState, flightNumber);
                    } else {
                        logger.error("Failed to publish state change to Kafka for flight {}: {}",
                            flightNumber, ex.getMessage());
                    }
                });

        } catch (Exception e) {
            logger.error("Error creating state change event for flight {}: {}", flightNumber, e.getMessage());
        }
    }

    /**
     * Publishes a simple state transition event (used by workflows).
     *
     * @param flightNumber   The flight number
     * @param previousState  The previous state
     * @param newState       The new state
     */
    public void publishStateChange(String flightNumber, FlightState previousState, FlightState newState) {
        publishStateChange(flightNumber, previousState, newState, null, 0);
    }
}
