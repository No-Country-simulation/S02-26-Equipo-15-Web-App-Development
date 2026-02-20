package com.nocountry.api.dto;

public record MetricsDto(
        long landingView,
        long clickCta,
        long beginCheckout,
        long purchase,
        double conversionRate,
        long orphanFailedOrders
) {
}
