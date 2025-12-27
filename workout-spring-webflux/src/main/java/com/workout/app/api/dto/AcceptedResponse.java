package com.workout.app.api.dto;

import java.util.UUID;

public record AcceptedResponse(boolean success, UUID correlationId, String message) {
    public static AcceptedResponse ok(UUID correlationId) {
        return new AcceptedResponse(true, correlationId, "Request accepted for processing");
    }
}

