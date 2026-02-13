package com.nocountry.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderDto(
        UUID id,
        UUID eventId,
        String stripeSessionId,
        String paymentIntentId,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt
) {
}
