package com.temporal.jetstream.dto;

import jakarta.validation.constraints.NotBlank;

public class ChangeGateRequest {
    @NotBlank(message = "New gate is required")
    private String newGate;

    public ChangeGateRequest() {
    }

    public ChangeGateRequest(String newGate) {
        this.newGate = newGate;
    }

    public String getNewGate() {
        return newGate;
    }

    public void setNewGate(String newGate) {
        this.newGate = newGate;
    }
}
