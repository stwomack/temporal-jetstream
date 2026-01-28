package com.temporal.jetstream.repository;

import com.temporal.jetstream.model.FlightStateTransition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * MongoDB repository for FlightStateTransition entities.
 * Provides CRUD operations and custom queries for flight state transition history.
 */
@Repository
public interface FlightStateTransitionRepository extends MongoRepository<FlightStateTransition, String> {

    /**
     * Find all state transitions for a specific flight, ordered by timestamp descending.
     *
     * @param flightNumber the flight number
     * @param flightDate the flight date
     * @return list of state transitions sorted by timestamp (most recent first)
     */
    List<FlightStateTransition> findByFlightNumberAndFlightDateOrderByTimestampDesc(String flightNumber, LocalDate flightDate);

    /**
     * Find all state transitions for a specific flight number across all dates,
     * ordered by timestamp descending.
     *
     * @param flightNumber the flight number
     * @return list of state transitions sorted by timestamp (most recent first)
     */
    List<FlightStateTransition> findByFlightNumberOrderByTimestampDesc(String flightNumber);
}
