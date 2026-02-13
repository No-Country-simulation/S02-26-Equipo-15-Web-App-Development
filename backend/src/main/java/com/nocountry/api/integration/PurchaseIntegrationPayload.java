package com.nocountry.api.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PurchaseIntegrationPayload(
        UUID eventId,
        String stripeSessionId,
        BigDecimal amount,
        String currency,
        Instant occurredAt,
        String clientUserAgent,
        String clientIp,
        String clientId,
        String customerEmail,
        String customerName
) {
}
