package com.nocountry.api.dto;

import java.time.Instant;
import java.util.UUID;

public record IntegrationLogDto(
        UUID id,
        String integration,
        String referenceId,
        String status,
        Integer httpStatus,
        Integer latencyMs,
        String requestPayload,
        String responsePayload,
        String errorMessage,
        Instant createdAt
) {
}
