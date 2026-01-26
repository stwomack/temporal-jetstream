package com.temporal.jetstream.dto;

public class StartJourneyResponse {
    private String workflowId;
    private String journeyId;
    private int numberOfLegs;

    public StartJourneyResponse() {
    }

    public StartJourneyResponse(String workflowId, String journeyId, int numberOfLegs) {
        this.workflowId = workflowId;
        this.journeyId = journeyId;
        this.numberOfLegs = numberOfLegs;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getJourneyId() {
        return journeyId;
    }

    public void setJourneyId(String journeyId) {
        this.journeyId = journeyId;
    }

    public int getNumberOfLegs() {
        return numberOfLegs;
    }

    public void setNumberOfLegs(int numberOfLegs) {
        this.numberOfLegs = numberOfLegs;
    }
}
