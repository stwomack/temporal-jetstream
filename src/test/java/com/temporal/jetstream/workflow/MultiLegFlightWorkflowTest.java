package com.temporal.jetstream.workflow;

import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.model.FlightState;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiLegFlightWorkflowTest {

    @RegisterExtension
    public static final TestWorkflowExtension testWorkflowExtension =
            TestWorkflowExtension.newBuilder()
                    .setWorkflowTypes(MultiLegFlightWorkflowImpl.class, FlightWorkflowImpl.class)
                    .setDoNotStart(false)
                    .build();

    @Test
    void testThreeLegJourney(TestWorkflowEnvironment testEnv, Worker worker, MultiLegFlightWorkflow workflow) {
        // Arrange: Create 3-leg journey (ORD -> DFW -> LAX -> SFO)
        Flight leg1 = createFlight("AA100", "ORD", "DFW", "B737-800");
        Flight leg2 = createFlight("AA200", "DFW", "LAX", "B737-800");
        Flight leg3 = createFlight("AA300", "LAX", "SFO", "B737-800");

        List<Flight> flights = Arrays.asList(leg1, leg2, leg3);

        // Act: Execute journey
        List<Flight> result = workflow.executeJourney(flights);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());

        // Verify all legs completed
        assertEquals(FlightState.COMPLETED, result.get(0).getCurrentState());
        assertEquals(FlightState.COMPLETED, result.get(1).getCurrentState());
        assertEquals(FlightState.COMPLETED, result.get(2).getCurrentState());

        // Verify flight numbers match
        assertEquals("AA100", result.get(0).getFlightNumber());
        assertEquals("AA200", result.get(1).getFlightNumber());
        assertEquals("AA300", result.get(2).getFlightNumber());
    }

    @Test
    void testJourneyWithCancelJourneySignal(TestWorkflowEnvironment testEnv, Worker worker) {
        // Arrange: Create 3-leg journey
        Flight leg1 = createFlight("AA100", "ORD", "DFW", "B737-800");
        Flight leg2 = createFlight("AA200", "DFW", "LAX", "B737-800");
        Flight leg3 = createFlight("AA300", "LAX", "SFO", "B737-800");

        List<Flight> flights = Arrays.asList(leg1, leg2, leg3);

        String workflowId = "journey-test-cancel-middle";
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(worker.getTaskQueue())
                .build();

        MultiLegFlightWorkflow workflow = testEnv.getWorkflowClient().newWorkflowStub(
                MultiLegFlightWorkflow.class, options);

        // Start workflow asynchronously
        WorkflowStub.fromTyped(workflow).start(flights);

        // Wait a bit for first leg to complete
        testEnv.sleep(java.time.Duration.ofSeconds(11));

        // Send cancel journey signal to parent workflow
        workflow.cancelJourney("Weather conditions");

        // Wait for workflow to complete
        testEnv.sleep(java.time.Duration.ofSeconds(20));

        // Act: Query the journey status after completion
        List<Flight> result = workflow.getJourneyStatus();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());

        // First leg should have completed (already finished before cancellation)
        assertEquals(FlightState.COMPLETED, result.get(0).getCurrentState());

        // Remaining legs should be cancelled
        assertEquals(FlightState.CANCELLED, result.get(1).getCurrentState());
        assertEquals(FlightState.CANCELLED, result.get(2).getCurrentState());
    }

    @Test
    void testAircraftTransferBetweenLegs(TestWorkflowEnvironment testEnv, Worker worker, MultiLegFlightWorkflow workflow) {
        // Arrange: Create 2-leg journey with same aircraft
        Flight leg1 = createFlight("AA100", "ORD", "DFW", "B737-800");
        Flight leg2 = createFlight("AA200", "DFW", "LAX", null); // No aircraft assigned yet

        List<Flight> flights = Arrays.asList(leg1, leg2);

        // Act: Execute journey
        List<Flight> result = workflow.executeJourney(flights);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        // Both legs should have completed
        assertEquals(FlightState.COMPLETED, result.get(0).getCurrentState());
        assertEquals(FlightState.COMPLETED, result.get(1).getCurrentState());

        // Second leg should have received aircraft from first leg
        assertEquals("B737-800", result.get(1).getAircraft());
    }

    @Test
    void testJourneyStatusQuery(TestWorkflowEnvironment testEnv, Worker worker) {
        // Arrange: Create 2-leg journey
        Flight leg1 = createFlight("AA100", "ORD", "DFW", "B737-800");
        Flight leg2 = createFlight("AA200", "DFW", "LAX", "B737-800");

        List<Flight> flights = Arrays.asList(leg1, leg2);

        String workflowId = "journey-test-query";
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(worker.getTaskQueue())
                .build();

        MultiLegFlightWorkflow workflow = testEnv.getWorkflowClient().newWorkflowStub(
                MultiLegFlightWorkflow.class, options);

        // Start workflow asynchronously
        WorkflowStub.fromTyped(workflow).start(flights);

        // Wait for first leg to complete
        testEnv.sleep(java.time.Duration.ofSeconds(11));

        // Act: Query journey status during execution
        List<Flight> status = workflow.getJourneyStatus();
        int currentLegIndex = workflow.getCurrentLegIndex();

        // Assert
        assertNotNull(status);
        assertEquals(2, status.size());

        // At least the first leg should have started
        assertTrue(currentLegIndex >= 0);

        // Wait for journey to complete
        testEnv.sleep(java.time.Duration.ofSeconds(15));

        // Verify journey completed
        List<Flight> finalStatus = workflow.getJourneyStatus();
        assertNotNull(finalStatus);
        assertEquals(2, finalStatus.size());
    }

    @Test
    void testSingleLegJourney(TestWorkflowEnvironment testEnv, Worker worker, MultiLegFlightWorkflow workflow) {
        // Arrange: Create single-leg journey
        Flight leg1 = createFlight("AA100", "ORD", "DFW", "B737-800");
        List<Flight> flights = Arrays.asList(leg1);

        // Act: Execute journey
        List<Flight> result = workflow.executeJourney(flights);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(FlightState.COMPLETED, result.get(0).getCurrentState());
        assertEquals("AA100", result.get(0).getFlightNumber());
    }

    private Flight createFlight(String flightNumber, String departure, String arrival, String aircraft) {
        LocalDate today = LocalDate.now();
        LocalDateTime departureTime = LocalDateTime.now().plusHours(2);
        LocalDateTime arrivalTime = departureTime.plusHours(3);

        Flight flight = new Flight(
                flightNumber,
                today,
                departure,
                arrival,
                departureTime,
                arrivalTime,
                "A1",
                aircraft
        );

        return flight;
    }
}
