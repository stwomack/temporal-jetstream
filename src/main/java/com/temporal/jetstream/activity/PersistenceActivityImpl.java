package com.temporal.jetstream.activity;

import com.temporal.jetstream.model.FlightStateTransition;
import com.temporal.jetstream.repository.FlightStateTransitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of PersistenceActivity that saves flight state transitions to MongoDB.
 * Temporal will automatically retry this activity if MongoDB operations fail.
 */
@Component
public class PersistenceActivityImpl implements PersistenceActivity {

    private static final Logger logger = LoggerFactory.getLogger(PersistenceActivityImpl.class);

    @Autowired
    private FlightStateTransitionRepository repository;

    @Override
    public void saveStateTransition(FlightStateTransition transition) {
        try {
            logger.info("Persisting state transition to MongoDB: {} -> {} for flight {}",
                    transition.getFromState(),
                    transition.getToState(),
                    transition.getFlightNumber());

            FlightStateTransition saved = repository.save(transition);

            logger.info("Successfully saved state transition with ID: {}", saved.getId());
        } catch (Exception e) {
            logger.error("Failed to save state transition to MongoDB for flight {}: {}",
                    transition.getFlightNumber(), e.getMessage(), e);
            throw e; // Let Temporal handle retry
        }
    }
}
