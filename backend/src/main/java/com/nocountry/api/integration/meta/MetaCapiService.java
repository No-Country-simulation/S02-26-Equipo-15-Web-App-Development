package com.nocountry.api.integration.meta;

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
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetaCapiService {

    private static final Logger log = LoggerFactory.getLogger(MetaCapiService.class);

    private final RestClient restClient;
    private final AppProperties appProperties;
    private final IntegrationLogService integrationLogService;
    private final ObjectMapper objectMapper;

    public MetaCapiService(
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
        if (!appProperties.getIntegrations().isMetaCapiEnabled()) {
            integrationLogService.logWithReference(
                    "META_CAPI",
                    referenceId,
                    "SKIPPED",
                    null,
                    null,
                    null,
                    null,
                    "META_CAPI_ENABLED=false"
            );
            return;
        }

        if (isBlank(appProperties.getMeta().getPixelId()) || isBlank(appProperties.getMeta().getAccessToken())) {
            log.warn("meta_capi skipped reason=missing_config eventId={}", payload.eventId());
            integrationLogService.logWithReference(
                    "META_CAPI",
                    referenceId,
                    "SKIPPED",
                    null,
                    null,
                    null,
                    null,
                    "missing META_PIXEL_ID or META_ACCESS_TOKEN"
            );
            return;
        }

        try {
            long startNanos = System.nanoTime();
            Map<String, Object> userData = new HashMap<>();
            if (!isBlank(payload.clientUserAgent())) {
                userData.put("client_user_agent", payload.clientUserAgent());
            }
            if (!isBlank(payload.clientIp())) {
                userData.put("client_ip_address", payload.clientIp());
            }

            Map<String, Object> event = new HashMap<>();
            event.put("event_name", "Purchase");
            event.put("event_time", payload.occurredAt().atZone(ZoneOffset.UTC).toEpochSecond());
            event.put("action_source", "website");
            if (payload.eventId() != null) {
                event.put("event_id", payload.eventId().toString());
            }
            event.put("user_data", userData);
            event.put("custom_data", Map.of(
                    "currency", payload.currency(),
                    "value", payload.amount()
            ));

            Map<String, Object> body = Map.of("data", List.of(event));

            ResponseEntity<String> response = restClient.post()
                    .uri("https://graph.facebook.com/v18.0/{pixelId}/events?access_token={token}",
                            appProperties.getMeta().getPixelId(),
                            appProperties.getMeta().getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);
            int latencyMs = elapsedMs(startNanos);

            log.info("meta_capi status=sent eventId={}", payload.eventId());
            integrationLogService.logWithReference(
                    "META_CAPI",
                    referenceId,
                    "SENT",
                    response.getStatusCode().value(),
                    latencyMs,
                    body,
                    parseResponsePayload(response.getBody()),
                    null
            );
        } catch (RestClientResponseException ex) {
            log.warn("meta_capi status=failed eventId={} error={}", payload.eventId(), ex.getMessage());
            integrationLogService.logWithReference(
                    "META_CAPI",
                    referenceId,
                    "FAILED",
                    ex.getRawStatusCode(),
                    null,
                    null,
                    ex.getResponseBodyAsString(),
                    ex.getMessage()
            );
        } catch (Exception ex) {
            log.warn("meta_capi status=failed eventId={} error={}", payload.eventId(), ex.getMessage());
            integrationLogService.logWithReference(
                    "META_CAPI",
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int elapsedMs(long startNanos) {
        return (int) ((System.nanoTime() - startNanos) / 1_000_000L);
    }

    private Object parseResponsePayload(String responseBody) {
        if (isBlank(responseBody)) {
            return null;
        }
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            if (json.isObject() || json.isArray()) {
                return json;
            }
            return responseBody;
        } catch (Exception ignored) {
            return responseBody;
        }
    }

    private String referenceId(PurchaseIntegrationPayload payload) {
        if (payload.eventId() != null) {
            return payload.eventId().toString();
        }
        return payload.stripeSessionId();
    }
}
