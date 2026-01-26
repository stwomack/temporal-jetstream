package com.temporal.jetstream.dto;

public class StartFlightResponse {
    private String workflowId;
    private String flightNumber;
    private String message;

    public StartFlightResponse() {
    }

    public StartFlightResponse(String workflowId, String flightNumber, String message) {
        this.workflowId = workflowId;
        this.flightNumber = flightNumber;
        this.message = message;
    }

    // Getters and Setters
    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
