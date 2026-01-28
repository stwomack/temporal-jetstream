package com.temporal.jetstream.activity;

import com.temporal.jetstream.model.FlightState;
import com.temporal.jetstream.service.FlightEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of FlightEventActivity that uses FlightEventProducer to publish to Kafka.
 */
@Component
public class FlightEventActivityImpl implements FlightEventActivity {

    private static final Logger logger = LoggerFactory.getLogger(FlightEventActivityImpl.class);

    @Autowired
    private FlightEventProducer flightEventProducer;

    @Override
    public void publishStateChange(String flightNumber, String previousState, String newState,
                                  String gate, int delayMinutes) {
        logger.info("Activity publishing state change: {} -> {} for flight {}",
            previousState, newState, flightNumber);

        FlightState prevState = previousState != null ? FlightState.valueOf(previousState) : null;
        FlightState nextState = FlightState.valueOf(newState);

        flightEventProducer.publishStateChange(flightNumber, prevState, nextState, gate, delayMinutes);
    }
}
