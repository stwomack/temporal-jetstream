package com.temporal.jetstream.service;

import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.model.FlightState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for publishing flight state changes to WebSocket clients.
 * Broadcasts updates to all subscribed clients in real-time.
 */
@Service
public class FlightEventService {

    private static final Logger logger = LoggerFactory.getLogger(FlightEventService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Publish a flight state update to all WebSocket subscribers.
     * Clients subscribed to /topic/flights will receive this update.
     */
    public void publishFlightUpdate(Flight flight) {
        logger.info("Publishing flight update: {} - {}", flight.getFlightNumber(), flight.getCurrentState());
        messagingTemplate.convertAndSend("/topic/flights", flight);
    }

    /**
     * Publish a flight state change event with basic details.
     */
    public void publishStateChange(String flightNumber, FlightState state, String message) {
        FlightStateChangeEvent event = new FlightStateChangeEvent(flightNumber, state, message);
        logger.info("Publishing state change: {} - {}", flightNumber, state);
        messagingTemplate.convertAndSend("/topic/flight-events", event);
    }

    /**
     * Event object for flight state changes.
     */
    public static class FlightStateChangeEvent {
        private String flightNumber;
        private FlightState state;
        private String message;
        private long timestamp;

        public FlightStateChangeEvent(String flightNumber, FlightState state, String message) {
            this.flightNumber = flightNumber;
            this.state = state;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public String getFlightNumber() {
            return flightNumber;
        }

        public FlightState getState() {
            return state;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
