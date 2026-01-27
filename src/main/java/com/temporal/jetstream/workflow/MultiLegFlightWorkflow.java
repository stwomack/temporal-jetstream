package com.temporal.jetstream.workflow;

import com.temporal.jetstream.model.Flight;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.List;

@WorkflowInterface
public interface MultiLegFlightWorkflow {

    @WorkflowMethod
    List<Flight> executeJourney(List<Flight> flights);

    @QueryMethod
    List<Flight> getJourneyStatus();

    @QueryMethod
    int getCurrentLegIndex();

    @SignalMethod
    void cancelJourney(String reason);
}
