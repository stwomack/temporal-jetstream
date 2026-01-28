package com.temporal.jetstream.integration;

import com.temporal.jetstream.model.FlightState;
import com.temporal.jetstream.model.FlightStateTransition;
import com.temporal.jetstream.repository.FlightStateTransitionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MongoDB persistence of flight state transitions.
 * Tests verify that state transitions are correctly saved to and retrieved from MongoDB.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.mongodb.database=temporal-jetstream-test"
})
public class MongoDBPersistenceTest {

    @Autowired
    private FlightStateTransitionRepository transitionRepository;

    @BeforeEach
    public void setUp() {
        // Clear MongoDB before each test
        transitionRepository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        // Clean up test data
        transitionRepository.deleteAll();
    }

    @Test
    public void testSaveAndRetrieveTransition() {
        // Create and save a transition
        FlightStateTransition transition = new FlightStateTransition(
                "AA9999",
                LocalDate.now(),
                FlightState.DEPARTED,
                FlightState.IN_FLIGHT,
                LocalDateTime.now(),
                "C3",
                30,
                "N99999",
                "STATE_TRANSITION",
                "Flight transitioned from DEPARTED to IN_FLIGHT"
        );

        FlightStateTransition saved = transitionRepository.save(transition);

        // Verify it was saved
        assertNotNull(saved.getId());
        assertEquals("AA9999", saved.getFlightNumber());
        assertEquals(FlightState.DEPARTED, saved.getFromState());
        assertEquals(FlightState.IN_FLIGHT, saved.getToState());
        assertEquals(30, saved.getDelay());
    }

    @Test
    public void testTransitionHistoryQuery() {
        // Create test transitions directly
        FlightStateTransition t1 = new FlightStateTransition(
                "AA5678",
                LocalDate.now(),
                null,
                FlightState.SCHEDULED,
                LocalDateTime.now().minusHours(2),
                "B2",
                0,
                "N67890",
                "STATE_TRANSITION",
                "Flight transitioned to SCHEDULED"
        );

        FlightStateTransition t2 = new FlightStateTransition(
                "AA5678",
                LocalDate.now(),
                FlightState.SCHEDULED,
                FlightState.BOARDING,
                LocalDateTime.now().minusHours(1),
                "B2",
                15,
                "N67890",
                "STATE_TRANSITION",
                "Flight transitioned from SCHEDULED to BOARDING"
        );

        transitionRepository.save(t1);
        transitionRepository.save(t2);

        // Query by flight number and date
        List<FlightStateTransition> transitions = transitionRepository
                .findByFlightNumberAndFlightDateOrderByTimestampDesc("AA5678", LocalDate.now());

        assertEquals(2, transitions.size());
        // Should be sorted by timestamp descending (most recent first)
        assertEquals(FlightState.BOARDING, transitions.get(0).getToState());
        assertEquals(FlightState.SCHEDULED, transitions.get(1).getToState());
    }

    @Test
    public void testMultipleFlightsTransitions() {
        // Create transitions for multiple flights
        FlightStateTransition f1t1 = new FlightStateTransition(
                "AA1111",
                LocalDate.now(),
                null,
                FlightState.SCHEDULED,
                LocalDateTime.now(),
                "A1",
                0,
                "N11111",
                "STATE_TRANSITION",
                "Flight transitioned to SCHEDULED"
        );

        FlightStateTransition f2t1 = new FlightStateTransition(
                "AA2222",
                LocalDate.now(),
                null,
                FlightState.SCHEDULED,
                LocalDateTime.now(),
                "B1",
                0,
                "N22222",
                "STATE_TRANSITION",
                "Flight transitioned to SCHEDULED"
        );

        transitionRepository.save(f1t1);
        transitionRepository.save(f2t1);

        // Query each flight separately
        List<FlightStateTransition> flight1Transitions = transitionRepository
                .findByFlightNumberOrderByTimestampDesc("AA1111");
        List<FlightStateTransition> flight2Transitions = transitionRepository
                .findByFlightNumberOrderByTimestampDesc("AA2222");

        assertEquals(1, flight1Transitions.size());
        assertEquals("AA1111", flight1Transitions.get(0).getFlightNumber());

        assertEquals(1, flight2Transitions.size());
        assertEquals("AA2222", flight2Transitions.get(0).getFlightNumber());
    }
}
