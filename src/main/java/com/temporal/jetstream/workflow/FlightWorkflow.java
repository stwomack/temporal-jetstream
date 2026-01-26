package com.temporal.jetstream.workflow;

import com.temporal.jetstream.model.Flight;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface FlightWorkflow {

    @WorkflowMethod
    Flight executeFlight(Flight flight);
}
