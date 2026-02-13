package com.nocountry.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SessionSummaryDto(
        UUID eventId,
        Instant createdAt,
        Instant lastSeenAt,
        String utmSource,
        String utmMedium,
        String utmCampaign,
        String utmTerm,
        String utmContent,
        String gclid,
        String fbclid,
        String landingPath,
        String userAgent,
        String ipHash
) {
}
