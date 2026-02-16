package com.nocountry.api.integration.ga4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.api.config.AppProperties;
import com.nocountry.api.integration.PurchaseIntegrationPayload;
import com.nocountry.api.service.IntegrationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class Ga4MeasurementProtocolService {

    private static final Logger log = LoggerFactory.getLogger(Ga4MeasurementProtocolService.class);

    private final RestClient restClient;
    private final AppProperties appProperties;
    private final IntegrationLogService integrationLogService;
    private final ObjectMapper objectMapper;

    public Ga4MeasurementProtocolService(
            RestClient integrationRestClient,
            AppProperties appProperties,
            IntegrationLogService integrationLogService,
            ObjectMapper objectMapper
    ) {
        this.restClient = integrationRestClient;
        this.appProperties = appProperties;
        this.integrationLogService = integrationLogService;
        this.objectMapper = objectMapper;
    }

    public void sendPurchase(PurchaseIntegrationPayload payload) {
        String referenceId = referenceId(payload);
        if (!appProperties.getIntegrations().isGa4MpEnabled()) {
            integrationLogService.logWithReference(
                    "GA4_MP",
                    referenceId,
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
            integrationLogService.logWithReference(
                    "GA4_MP",
                    referenceId,
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
            Map<String, Object> body = buildRequestBody(payload, clientId);

            ValidationResult validationResult = ValidationResult.disabled();
            if (appProperties.getGa4().isDebugValidationEnabled()) {
                validationResult = validateDebug(body);
            }

            ResponseEntity<Void> response = restClient.post()
                    .uri("https://www.google-analytics.com/mp/collect?measurement_id={measurementId}&api_secret={apiSecret}",
                            appProperties.getGa4().getMeasurementId(),
                            appProperties.getGa4().getApiSecret())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            int latencyMs = elapsedMs(startNanos);

            String status = validationResult.messages().isEmpty() ? "SENT" : "SENT_WITH_WARNINGS";
            log.info("ga4_mp status={} eventId={} validationErrors={}", status.toLowerCase(), payload.eventId(), validationResult.messages().size());

            integrationLogService.logWithReference(
                    "GA4_MP",
                    referenceId,
                    status,
                    response.getStatusCode().value(),
                    latencyMs,
                    body,
                    buildDebugLogPayload(validationResult),
                    validationResult.messages().isEmpty() ? null : "GA4 debug validation reported issues"
            );
        } catch (RestClientResponseException ex) {
            log.warn("ga4_mp status=failed eventId={} error={}", payload.eventId(), ex.getMessage());
            integrationLogService.logWithReference(
                    "GA4_MP",
                    referenceId,
                    "FAILED",
                    ex.getRawStatusCode(),
                    null,
                    null,
                    ex.getResponseBodyAsString(),
                    ex.getMessage()
            );
        } catch (Exception ex) {
            log.warn("ga4_mp status=failed eventId={} error={}", payload.eventId(), ex.getMessage());
            integrationLogService.logWithReference(
                    "GA4_MP",
                    referenceId,
                    "FAILED",
                    null,
                    null,
                    null,
                    null,
                    ex.getMessage()
            );
        }
    }

    private ValidationResult validateDebug(Map<String, Object> body) {
        try {
            ResponseEntity<String> response = restClient.post()
                    .uri("https://www.google-analytics.com/debug/mp/collect?measurement_id={measurementId}&api_secret={apiSecret}",
                            appProperties.getGa4().getMeasurementId(),
                            appProperties.getGa4().getApiSecret())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            return parseValidationResult(response.getBody(), response.getStatusCode().value());
        } catch (RestClientResponseException ex) {
            String message = "debug_endpoint_http_error status=" + ex.getRawStatusCode();
            return new ValidationResult(ex.getRawStatusCode(), List.of(message));
        } catch (Exception ex) {
            return new ValidationResult(null, List.of("debug_endpoint_error " + ex.getMessage()));
        }
    }

    private ValidationResult parseValidationResult(String rawBody, Integer httpStatus) {
        if (isBlank(rawBody)) {
            return new ValidationResult(httpStatus, List.of());
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode validationMessages = root.path("validationMessages");
            if (!validationMessages.isArray() || validationMessages.isEmpty()) {
                return new ValidationResult(httpStatus, List.of());
            }

            List<String> messages = new ArrayList<>();
            for (JsonNode messageNode : validationMessages) {
                String description = messageNode.path("description").asText(null);
                if (isBlank(description)) {
                    description = messageNode.toString();
                }
                messages.add(description);
            }
            return new ValidationResult(httpStatus, messages);
        } catch (Exception ex) {
            return new ValidationResult(httpStatus, List.of("debug_response_parse_error " + ex.getMessage()));
        }
    }

    private Map<String, Object> buildRequestBody(PurchaseIntegrationPayload payload, String clientId) {
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

        return Map.of(
                "client_id", clientId,
                "events", List.of(event)
        );
    }

    private Map<String, Object> buildDebugLogPayload(ValidationResult validationResult) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("debug_validation_enabled", appProperties.getGa4().isDebugValidationEnabled());
        payload.put("validation_messages", validationResult.messages());
        if (validationResult.httpStatus() != null) {
            payload.put("debug_http_status", validationResult.httpStatus());
        }
        return payload;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int elapsedMs(long startNanos) {
        return (int) ((System.nanoTime() - startNanos) / 1_000_000L);
    }

    private String referenceId(PurchaseIntegrationPayload payload) {
        if (payload.eventId() != null) {
            return payload.eventId().toString();
        }
        return payload.stripeSessionId();
    }

    private record ValidationResult(Integer httpStatus, List<String> messages) {
        private static ValidationResult disabled() {
            return new ValidationResult(null, List.of());
        }
    }
}
