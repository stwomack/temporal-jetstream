package com.temporal.jetstream.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class StartJourneyRequest {
    @NotNull(message = "Journey ID is required")
    private String journeyId;

    @NotEmpty(message = "At least one flight leg is required")
    @Valid
    private List<StartFlightRequest> flights;

    public StartJourneyRequest() {
    }

    public String getJourneyId() {
        return journeyId;
    }

    public void setJourneyId(String journeyId) {
        this.journeyId = journeyId;
    }

    public List<StartFlightRequest> getFlights() {
        return flights;
    }

    public void setFlights(List<StartFlightRequest> flights) {
        this.flights = flights;
    }
}
