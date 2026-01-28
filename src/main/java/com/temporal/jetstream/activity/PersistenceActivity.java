package com.temporal.jetstream.activity;

import com.temporal.jetstream.model.FlightStateTransition;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal Activity interface for persisting flight state transitions to MongoDB.
 * Activities handle non-deterministic operations like database writes.
 */
@ActivityInterface
public interface PersistenceActivity {

    /**
     * Save a flight state transition to MongoDB.
     *
     * @param transition the state transition to persist
     */
    @ActivityMethod
    void saveStateTransition(FlightStateTransition transition);
}
