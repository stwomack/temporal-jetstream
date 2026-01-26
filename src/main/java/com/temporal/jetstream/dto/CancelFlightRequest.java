package com.temporal.jetstream.dto;

import jakarta.validation.constraints.NotBlank;

public class CancelFlightRequest {
    @NotBlank(message = "Cancellation reason is required")
    private String reason;

    public CancelFlightRequest() {
    }

    public CancelFlightRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
