package com.temporal.jetstream.workflow;

import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.model.FlightState;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface FlightWorkflow {

    @WorkflowMethod
    Flight executeFlight(Flight flight);

    @SignalMethod
    void announceDelay(int minutes);

    @SignalMethod
    void changeGate(String newGate);

    @SignalMethod
    void cancelFlight(String reason);

    @QueryMethod
    FlightState getCurrentState();

    @QueryMethod
    Flight getFlightDetails();

    @QueryMethod
    int getDelayMinutes();
}
