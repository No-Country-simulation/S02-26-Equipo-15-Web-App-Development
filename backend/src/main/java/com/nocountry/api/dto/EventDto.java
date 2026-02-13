package com.nocountry.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EventDto(
        UUID id,
        UUID eventId,
        String eventType,
        Instant createdAt,
        String currency,
        BigDecimal value,
        String payloadJson
) {
}
