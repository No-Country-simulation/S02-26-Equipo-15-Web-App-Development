package com.nocountry.api.dto;

import java.util.List;

public record SessionDetailDto(
        SessionSummaryDto session,
        List<EventDto> events,
        List<OrderDto> orders,
        List<IntegrationLogDto> integrations
) {
}
