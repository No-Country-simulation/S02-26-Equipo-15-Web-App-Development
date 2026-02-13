package com.nocountry.api.integration.ga4;

import com.nocountry.api.config.AppProperties;
import com.nocountry.api.integration.PurchaseIntegrationPayload;
import com.nocountry.api.service.IntegrationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class Ga4MeasurementProtocolService {

    private static final Logger log = LoggerFactory.getLogger(Ga4MeasurementProtocolService.class);

    private final RestClient restClient;
    private final AppProperties appProperties;
    private final IntegrationLogService integrationLogService;

    public Ga4MeasurementProtocolService(
            RestClient integrationRestClient,
            AppProperties appProperties,
            IntegrationLogService integrationLogService
    ) {
        this.restClient = integrationRestClient;
        this.appProperties = appProperties;
        this.integrationLogService = integrationLogService;
    }

    public void sendPurchase(PurchaseIntegrationPayload payload) {
        if (!appProperties.getIntegrations().isGa4MpEnabled()) {
            integrationLogService.log(
                    "GA4_MP",
                    payload.eventId(),
                    "SKIPPED",
                    null,
                    null,
                    null,
                    null,
                    "GA4_MP_ENABLED=false"
            );
            return;
        }

        if (isBlank(appProperties.getGa4().getMeasurementId()) || isBlank(appProperties.getGa4().getApiSecret())) {
            log.warn("ga4_mp skipped reason=missing_config eventId={}", payload.eventId());
            integrationLogService.log(
                    "GA4_MP",
                    payload.eventId(),
                    "SKIPPED",
                    null,
                    null,
                    null,
                    null,
                    "missing GA4_MEASUREMENT_ID or GA4_API_SECRET"
            );
            return;
        }

        String clientId = payload.clientId();
        if (isBlank(clientId)) {
            UUID fallbackId = payload.eventId() == null ? UUID.randomUUID() : payload.eventId();
            clientId = "server_backup." + fallbackId;
            log.warn("ga4_mp using_server_backup_client_id eventId={}", payload.eventId());
        }

        try {
            long startNanos = System.nanoTime();
            Map<String, Object> params = Map.of(
                    "currency", payload.currency(),
                    "value", payload.amount(),
                    "transaction_id", payload.stripeSessionId(),
                    "engagement_time_msec", 1
            );

            Map<String, Object> event = Map.of(
                    "name", "purchase",
                    "params", params
            );

            Map<String, Object> body = Map.of(
                    "client_id", clientId,
                    "events", List.of(event)
            );

            ResponseEntity<Void> response = restClient.post()
                    .uri("https://www.google-analytics.com/mp/collect?measurement_id={measurementId}&api_secret={apiSecret}",
                            appProperties.getGa4().getMeasurementId(),
                            appProperties.getGa4().getApiSecret())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            int latencyMs = elapsedMs(startNanos);

            log.info("ga4_mp status=sent eventId={}", payload.eventId());
            integrationLogService.log(
                    "GA4_MP",
                    payload.eventId(),
                    "SENT",
                    response.getStatusCode().value(),
                    latencyMs,
                    body,
                    null,
                    null
            );
        } catch (RestClientResponseException ex) {
            log.warn("ga4_mp status=failed eventId={} error={}", payload.eventId(), ex.getMessage());
            integrationLogService.log(
                    "GA4_MP",
                    payload.eventId(),
                    "FAILED",
                    ex.getRawStatusCode(),
                    null,
                    null,
                    ex.getResponseBodyAsString(),
                    ex.getMessage()
            );
        } catch (Exception ex) {
            log.warn("ga4_mp status=failed eventId={} error={}", payload.eventId(), ex.getMessage());
            integrationLogService.log(
                    "GA4_MP",
                    payload.eventId(),
                    "FAILED",
                    null,
                    null,
                    null,
                    null,
                    ex.getMessage()
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int elapsedMs(long startNanos) {
        return (int) ((System.nanoTime() - startNanos) / 1_000_000L);
    }
}
