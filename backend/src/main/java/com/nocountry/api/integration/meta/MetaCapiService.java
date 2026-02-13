package com.nocountry.api.integration.meta;

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

    public MetaCapiService(
            RestClient integrationRestClient,
            AppProperties appProperties,
            IntegrationLogService integrationLogService
    ) {
        this.restClient = integrationRestClient;
        this.appProperties = appProperties;
        this.integrationLogService = integrationLogService;
    }

    public void sendPurchase(PurchaseIntegrationPayload payload) {
        if (!appProperties.getIntegrations().isMetaCapiEnabled()) {
            integrationLogService.log(
                    "META_CAPI",
                    payload.eventId(),
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
            integrationLogService.log(
                    "META_CAPI",
                    payload.eventId(),
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

            ResponseEntity<Void> response = restClient.post()
                    .uri("https://graph.facebook.com/v18.0/{pixelId}/events?access_token={token}",
                            appProperties.getMeta().getPixelId(),
                            appProperties.getMeta().getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            int latencyMs = elapsedMs(startNanos);

            log.info("meta_capi status=sent eventId={}", payload.eventId());
            integrationLogService.log(
                    "META_CAPI",
                    payload.eventId(),
                    "SENT",
                    response.getStatusCode().value(),
                    latencyMs,
                    body,
                    null,
                    null
            );
        } catch (RestClientResponseException ex) {
            log.warn("meta_capi status=failed eventId={} error={}", payload.eventId(), ex.getMessage());
            integrationLogService.log(
                    "META_CAPI",
                    payload.eventId(),
                    "FAILED",
                    ex.getRawStatusCode(),
                    null,
                    null,
                    ex.getResponseBodyAsString(),
                    ex.getMessage()
            );
        } catch (Exception ex) {
            log.warn("meta_capi status=failed eventId={} error={}", payload.eventId(), ex.getMessage());
            integrationLogService.log(
                    "META_CAPI",
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
