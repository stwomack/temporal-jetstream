package com.temporal.jetstream.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.temporal.jetstream.model.FlightEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Test utility for publishing flight events to Kafka topics.
 * Used in integration tests to verify Kafka consumer and workflow signal integration.
 */
@Component
public class KafkaTestProducer {

    private static final String TOPIC = "flight-events";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    public KafkaTestProducer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Publishes a delay announced event to Kafka.
     */
    public void publishDelayEvent(String flightNumber, LocalDate flightDate, int delayMinutes) throws Exception {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", FlightEventType.DELAY_ANNOUNCED.name());
        event.put("flightNumber", flightNumber);
        event.put("flightDate", flightDate.toString());

        Map<String, Object> data = new HashMap<>();
        data.put("delayMinutes", delayMinutes);
        event.put("data", objectMapper.writeValueAsString(data));

        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(TOPIC, flightNumber, message).get();
    }

    /**
     * Publishes a gate change event to Kafka.
     */
    public void publishGateChangeEvent(String flightNumber, LocalDate flightDate, String newGate) throws Exception {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", FlightEventType.GATE_CHANGED.name());
        event.put("flightNumber", flightNumber);
        event.put("flightDate", flightDate.toString());

        Map<String, Object> data = new HashMap<>();
        data.put("gate", newGate);
        event.put("data", objectMapper.writeValueAsString(data));

        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(TOPIC, flightNumber, message).get();
    }

    /**
     * Publishes a flight cancellation event to Kafka.
     */
    public void publishCancellationEvent(String flightNumber, LocalDate flightDate, String reason) throws Exception {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", FlightEventType.FLIGHT_CANCELLED.name());
        event.put("flightNumber", flightNumber);
        event.put("flightDate", flightDate.toString());

        Map<String, Object> data = new HashMap<>();
        data.put("reason", reason);
        event.put("data", objectMapper.writeValueAsString(data));

        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(TOPIC, flightNumber, message).get();
    }

    /**
     * Publishes a gate assigned event to Kafka.
     */
    public void publishGateAssignedEvent(String flightNumber, LocalDate flightDate, String gate) throws Exception {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", FlightEventType.GATE_ASSIGNED.name());
        event.put("flightNumber", flightNumber);
        event.put("flightDate", flightDate.toString());

        Map<String, Object> data = new HashMap<>();
        data.put("gate", gate);
        event.put("data", objectMapper.writeValueAsString(data));

        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(TOPIC, flightNumber, message).get();
    }
}
