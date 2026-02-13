package com.nocountry.api.integration.pipedrive;

import com.nocountry.api.config.AppProperties;
import com.nocountry.api.integration.PurchaseIntegrationPayload;
import com.nocountry.api.service.IntegrationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PipedriveService {

    private static final Logger log = LoggerFactory.getLogger(PipedriveService.class);

    private final RestClient restClient;
    private final AppProperties appProperties;
    private final IntegrationLogService integrationLogService;

    public PipedriveService(
            RestClient integrationRestClient,
            AppProperties appProperties,
            IntegrationLogService integrationLogService
    ) {
        this.restClient = integrationRestClient;
        this.appProperties = appProperties;
        this.integrationLogService = integrationLogService;
    }

    public void sendPurchase(PurchaseIntegrationPayload payload) {
        if (!appProperties.getIntegrations().isPipedriveEnabled()) {
            integrationLogService.log(
                    "PIPEDRIVE",
                    payload.eventId(),
                    "SKIPPED",
                    null,
                    null,
                    null,
                    null,
                    "PIPEDRIVE_ENABLED=false"
            );
            return;
        }

        if (isBlank(appProperties.getPipedrive().getApiToken())) {
            log.warn("pipedrive skipped reason=missing_config eventId={}", payload.eventId());
            integrationLogService.log(
                    "PIPEDRIVE",
                    payload.eventId(),
                    "SKIPPED",
                    null,
                    null,
                    null,
                    null,
                    "missing PIPEDRIVE_API_TOKEN"
            );
            return;
        }

        try {
            long startNanos = System.nanoTime();
            String token = appProperties.getPipedrive().getApiToken();
            Long personId = upsertPerson(token, payload);
            upsertDeal(token, payload, personId);
            int latencyMs = elapsedMs(startNanos);

            log.info("pipedrive status=sent eventId={}", payload.eventId());
            integrationLogService.log(
                    "PIPEDRIVE",
                    payload.eventId(),
                    "SENT",
                    200,
                    latencyMs,
                    Map.of(
                            "stripe_session_id", payload.stripeSessionId(),
                            "amount", payload.amount(),
                            "currency", payload.currency()
                    ),
                    null,
                    null
            );
        } catch (RestClientResponseException ex) {
            log.warn("pipedrive status=failed eventId={} error={}", payload.eventId(), ex.getMessage());
            integrationLogService.log(
                    "PIPEDRIVE",
                    payload.eventId(),
                    "FAILED",
                    ex.getRawStatusCode(),
                    null,
                    null,
                    ex.getResponseBodyAsString(),
                    ex.getMessage()
            );
        } catch (Exception ex) {
            log.warn("pipedrive status=failed eventId={} error={}", payload.eventId(), ex.getMessage());
            integrationLogService.log(
                    "PIPEDRIVE",
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

    private Long upsertPerson(String token, PurchaseIntegrationPayload payload) {
        Long existingPersonId = findPersonByEmail(token, payload.customerEmail());
        if (existingPersonId != null) {
            return existingPersonId;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("name", resolveCustomerName(payload));
        if (!isBlank(payload.customerEmail())) {
            body.put("email", payload.customerEmail());
        }

        Map<?, ?> response = restClient.post()
                .uri("https://api.pipedrive.com/v1/persons?api_token={token}", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        return extractIdFromData(response);
    }

    private void upsertDeal(String token, PurchaseIntegrationPayload payload, Long personId) {
        String title = "Purchase " + payload.stripeSessionId();
        Long existingDealId = findDealByTitle(token, title);

        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("value", payload.amount());
        body.put("currency", payload.currency());
        body.put("status", "won");
        if (personId != null) {
            body.put("person_id", personId);
        }

        if (existingDealId != null) {
            restClient.put()
                    .uri("https://api.pipedrive.com/v1/deals/{dealId}?api_token={token}", existingDealId, token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return;
        }

        restClient.post()
                .uri("https://api.pipedrive.com/v1/deals?api_token={token}", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private Long findPersonByEmail(String token, String email) {
        if (isBlank(email)) {
            return null;
        }

        Map<?, ?> response = restClient.get()
                .uri("https://api.pipedrive.com/v1/persons/search?term={term}&fields=email&exact_match=true&api_token={token}", email, token)
                .retrieve()
                .body(Map.class);

        return extractFirstItemId(response);
    }

    private Long findDealByTitle(String token, String title) {
        Map<?, ?> response = restClient.get()
                .uri("https://api.pipedrive.com/v1/deals/search?term={term}&fields=title&exact_match=true&api_token={token}", title, token)
                .retrieve()
                .body(Map.class);

        return extractFirstItemId(response);
    }

    @SuppressWarnings("unchecked")
    private Long extractFirstItemId(Map<?, ?> response) {
        if (!(response != null && response.get("data") instanceof Map<?, ?> data)) {
            return null;
        }
        Object itemsObj = data.get("items");
        if (!(itemsObj instanceof List<?> items) || items.isEmpty()) {
            return null;
        }
        Object firstObj = items.get(0);
        if (!(firstObj instanceof Map<?, ?> firstMap)) {
            return null;
        }
        Object itemObj = firstMap.get("item");
        if (!(itemObj instanceof Map<?, ?> itemMap)) {
            return null;
        }
        Object idObj = itemMap.get("id");
        return asLong(idObj);
    }

    private Long extractIdFromData(Map<?, ?> response) {
        if (!(response != null && response.get("data") instanceof Map<?, ?> data)) {
            return null;
        }
        return asLong(data.get("id"));
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String resolveCustomerName(PurchaseIntegrationPayload payload) {
        if (!isBlank(payload.customerName())) {
            return payload.customerName();
        }
        if (!isBlank(payload.customerEmail())) {
            return payload.customerEmail();
        }
        return "Customer " + payload.stripeSessionId();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int elapsedMs(long startNanos) {
        return (int) ((System.nanoTime() - startNanos) / 1_000_000L);
    }
}
