package com.temporal.jetstream.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AnnounceDelayRequest {
    @NotNull(message = "Delay minutes is required")
    @Min(value = 1, message = "Delay must be at least 1 minute")
    private Integer minutes;

    public AnnounceDelayRequest() {
    }

    public AnnounceDelayRequest(Integer minutes) {
        this.minutes = minutes;
    }

    public Integer getMinutes() {
        return minutes;
    }

    public void setMinutes(Integer minutes) {
        this.minutes = minutes;
    }
}
