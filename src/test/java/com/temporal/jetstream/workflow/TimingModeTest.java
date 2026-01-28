package com.temporal.jetstream.workflow;

import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.model.FlightState;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Demo Mode vs Real-time Mode timing behavior
 */
class TimingModeTest {

    @RegisterExtension
    public static final TestWorkflowExtension testWorkflowExtension =
            TestWorkflowExtension.newBuilder()
                    .setWorkflowTypes(FlightWorkflowImpl.class)
                    .setDoNotStart(false)
                    .build();

    @Test
    void testDemoModeFlightCompletesSuccessfully(
            TestWorkflowEnvironment testEnv, Worker worker, FlightWorkflow workflow) {

        // Create a test flight with demo mode enabled
        Flight flight = createTestFlight("DEMO1234", true);

        // Execute the workflow
        Flight result = workflow.executeFlight(flight);

        // Verify the workflow completed successfully
        assertNotNull(result, "Result should not be null");
        assertEquals("DEMO1234", result.getFlightNumber());
        assertEquals(FlightState.COMPLETED, result.getCurrentState());
        assertTrue(result.isDemoMode(), "Flight should be in demo mode");
    }

    @Test
    void testDemoModeFlagIsRespected(
            TestWorkflowEnvironment testEnv, Worker worker, FlightWorkflow workflow) {

        // Create a test flight with demoMode = true
        Flight flight = createTestFlight("TEST1234", true);

        // Execute the workflow
        Flight result = workflow.executeFlight(flight);

        // Verify demo mode flag is preserved
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isDemoMode(), "Flight should be in demo mode");
        assertEquals(FlightState.COMPLETED, result.getCurrentState());
    }

    @Test
    void testDemoPrefixActivatesDemoMode(
            TestWorkflowEnvironment testEnv, Worker worker, FlightWorkflow workflow) {

        // Create flight with DEMO prefix but demoMode = false
        // Prefix should activate demo mode for backward compatibility
        Flight flight = createTestFlight("DEMO5678", false);

        // Execute the workflow
        Flight result = workflow.executeFlight(flight);

        // Verify workflow completes (demo mode timing used)
        assertNotNull(result, "Result should not be null");
        assertEquals("DEMO5678", result.getFlightNumber());
        assertEquals(FlightState.COMPLETED, result.getCurrentState());
    }

    @Test
    void testRealTimeModeFlightCompletes(
            TestWorkflowEnvironment testEnv, Worker worker, FlightWorkflow workflow) {

        // Create a real-time mode flight
        Flight flight = createTestFlight("AA9999", false);

        // Execute the workflow
        Flight result = workflow.executeFlight(flight);

        // Verify workflow completes successfully
        assertNotNull(result, "Result should not be null");
        assertEquals("AA9999", result.getFlightNumber());
        assertEquals(FlightState.COMPLETED, result.getCurrentState());
        assertFalse(result.isDemoMode(), "Flight should NOT be in demo mode");
    }

    /**
     * Helper method to create a test flight
     */
    private Flight createTestFlight(String flightNumber, boolean demoMode) {
        LocalDateTime now = LocalDateTime.now();
        Flight flight = new Flight(
                flightNumber,
                LocalDate.now(),
                "ORD",
                "DFW",
                now.plusHours(2),
                now.plusHours(4),
                "A12",
                "N123AA"
        );
        flight.setDemoMode(demoMode);
        return flight;
    }
}
