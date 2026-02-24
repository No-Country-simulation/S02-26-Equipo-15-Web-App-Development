package com.nocountry.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.api.config.AppProperties;
import com.nocountry.api.entity.OrderRecord;
import com.nocountry.api.entity.StripeWebhookEvent;
import com.nocountry.api.integration.PurchaseIntegrationDispatcher;
import com.nocountry.api.integration.PurchaseIntegrationPayload;
import com.nocountry.api.integration.stripe.StripeSignatureVerifier;
import com.nocountry.api.repository.OrderRepository;
import com.nocountry.api.repository.StripeWebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final StripeWebhookEventRepository stripeWebhookEventRepository;
    private final OrderRepository orderRepository;
    private final TrackingService trackingService;
    private final StripeSignatureVerifier stripeSignatureVerifier;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final PurchaseIntegrationDispatcher purchaseIntegrationDispatcher;
    private final Clock clock;

    public StripeWebhookService(
            StripeWebhookEventRepository stripeWebhookEventRepository,
            OrderRepository orderRepository,
            TrackingService trackingService,
            StripeSignatureVerifier stripeSignatureVerifier,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            PurchaseIntegrationDispatcher purchaseIntegrationDispatcher,
            Clock clock
    ) {
        this.stripeWebhookEventRepository = stripeWebhookEventRepository;
        this.orderRepository = orderRepository;
        this.trackingService = trackingService;
        this.stripeSignatureVerifier = stripeSignatureVerifier;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.purchaseIntegrationDispatcher = purchaseIntegrationDispatcher;
        this.clock = clock;
    }

    public void process(String payload, String signatureHeader, RequestMetadata metadata) {
        String stripeEventId = extractStripeEventId(payload);
        StripeWebhookEvent webhookEvent = null;
        UUID resolvedEventId = null;

        try {
            webhookEvent = findOrCreateEvent(stripeEventId);
            if (webhookEvent != null && "PROCESSED".equals(webhookEvent.getStatus())) {
                log.info("stripe_webhook duplicate_processed stripeEventId={}", stripeEventId);
                return;
            }

            boolean validSignature = stripeSignatureVerifier.verify(payload, signatureHeader, appProperties.getStripe().getWebhookSecret());
            if (!validSignature) {
                throw new IllegalArgumentException("Invalid Stripe signature");
            }

            JsonNode root = objectMapper.readTree(payload);
            String eventType = text(root, "type");
            resolvedEventId = extractTrackingEventIdFromRoot(root, eventType);

            if ("checkout.session.completed".equals(eventType)) {
                UUID handledEventId = handleCheckoutCompleted(root, payload, metadata);
                if (handledEventId != null) {
                    resolvedEventId = handledEventId;
                }
            } else if ("payment_intent.succeeded".equals(eventType)
                    || "payment_intent.payment_failed".equals(eventType)
                    || "payment_intent.requires_action".equals(eventType)
                    || "payment_intent.processing".equals(eventType)) {
                UUID handledEventId = handlePaymentIntent(root, payload, metadata);
                if (handledEventId != null) {
                    resolvedEventId = handledEventId;
                }
            }

            if (webhookEvent != null) {
                webhookEvent.setStatus("PROCESSED");
                webhookEvent.setProcessedAt(Instant.now(clock));
                webhookEvent.setError(null);
                if (resolvedEventId != null) {
                    webhookEvent.setEventId(resolvedEventId);
                }
                stripeWebhookEventRepository.save(webhookEvent);
            }

            log.info("stripe_webhook status=processed stripeEventId={} type={}", stripeEventId, eventType);
        } catch (Exception ex) {
            if (webhookEvent != null) {
                webhookEvent.setStatus("FAILED");
                webhookEvent.setProcessedAt(Instant.now(clock));
                webhookEvent.setError(trimError(ex.getMessage()));
                if (resolvedEventId != null) {
                    webhookEvent.setEventId(resolvedEventId);
                }
                stripeWebhookEventRepository.save(webhookEvent);
            }
            log.warn("stripe_webhook status=failed stripeEventId={} error={}", stripeEventId, ex.getMessage());
            if (ex instanceof IllegalArgumentException badRequest) {
                throw badRequest;
            }
            throw new IllegalStateException("Stripe webhook processing failed", ex);
        }
    }

    private UUID handleCheckoutCompleted(JsonNode root, String rawPayload, RequestMetadata metadata) {
        JsonNode session = root.path("data").path("object");

        String stripeSessionId = text(session, "id");
        if (stripeSessionId == null || stripeSessionId.isBlank()) {
            throw new IllegalArgumentException("Missing checkout session id");
        }

        String paymentIntentId = text(session, "payment_intent");
        String currency = uppercaseOrDefault(text(session, "currency"), "USD");
        BigDecimal amount = amountFromMinorUnits(session.path("amount_total").asLong(0));
        String status = uppercaseOrDefault(text(session, "payment_status"), "UNKNOWN");

        UUID eventId = extractTrackingEventId(session);
        String clientId = text(session.path("metadata"), "client_id");
        String customerEmail = extractCheckoutCustomerEmail(session);
        String customerName = extractCheckoutCustomerName(session);

        if (!isBlank(paymentIntentId)) {
            Optional<OrderRecord> existingByPaymentIntent = orderRepository.findByPaymentIntentId(paymentIntentId);
            if (existingByPaymentIntent.isPresent()) {
                OrderRecord existingOrder = existingByPaymentIntent.get();
                boolean wasSuccessful = isSuccessfulStatus(existingOrder.getStatus());
                boolean gainedEventId = false;
                upsertStripeSessionId(existingOrder, stripeSessionId, paymentIntentId);
                existingOrder.setPaymentIntentId(coalesce(existingOrder.getPaymentIntentId(), paymentIntentId));
                existingOrder.setStatus(status);
                existingOrder.setBusinessStatus(toBusinessStatus(status));
                existingOrder.setCurrency(currency);
                existingOrder.setAmount(amount);
                if (existingOrder.getEventId() == null && eventId != null) {
                    existingOrder.setEventId(eventId);
                    gainedEventId = true;
                }
                saveOrderResilient(existingOrder, paymentIntentId, stripeSessionId);

                if (shouldDispatchSuccessfulPayment(wasSuccessful, status, gainedEventId)) {
                    processSuccessfulPayment(existingOrder, rawPayload, metadata, clientId, customerEmail, customerName);
                } else {
                    log.info("stripe_order duplicate paymentIntentId={} status={}", paymentIntentId, status);
                }
                return existingOrder.getEventId();
            }
        }

        Optional<OrderRecord> existing = orderRepository.findByStripeSessionId(stripeSessionId);
        if (existing.isPresent()) {
            OrderRecord existingOrder = existing.get();
            boolean wasSuccessful = isSuccessfulStatus(existingOrder.getStatus());
            boolean gainedEventId = false;
            existingOrder.setPaymentIntentId(coalesce(existingOrder.getPaymentIntentId(), paymentIntentId));
            existingOrder.setStatus(status);
            existingOrder.setBusinessStatus(toBusinessStatus(status));
            existingOrder.setCurrency(currency);
            existingOrder.setAmount(amount);
            if (existingOrder.getEventId() == null && eventId != null) {
                existingOrder.setEventId(eventId);
                gainedEventId = true;
            }
            saveOrderResilient(existingOrder, paymentIntentId, stripeSessionId);

            if (shouldDispatchSuccessfulPayment(wasSuccessful, status, gainedEventId)) {
                processSuccessfulPayment(existingOrder, rawPayload, metadata, clientId, customerEmail, customerName);
            } else {
                log.info("stripe_order duplicate stripeSessionId={} status={}", stripeSessionId, status);
            }
            return existingOrder.getEventId();
        }

        OrderRecord orderRecord = new OrderRecord();
        orderRecord.setId(UUID.randomUUID());
        orderRecord.setEventId(eventId);
        orderRecord.setStripeSessionId(stripeSessionId);
        orderRecord.setPaymentIntentId(paymentIntentId);
        orderRecord.setAmount(amount);
        orderRecord.setCurrency(currency);
        orderRecord.setStatus(status);
        orderRecord.setBusinessStatus(toBusinessStatus(status));
        orderRecord.setCreatedAt(Instant.now(clock));
        OrderRecord persistedOrder = saveOrderResilient(orderRecord, paymentIntentId, stripeSessionId);

        if (!orderRecord.getId().equals(persistedOrder.getId())) {
            boolean wasSuccessful = isSuccessfulStatus(persistedOrder.getStatus());
            boolean gainedEventId = false;
            persistedOrder.setPaymentIntentId(coalesce(persistedOrder.getPaymentIntentId(), paymentIntentId));
            persistedOrder.setStatus(status);
            persistedOrder.setBusinessStatus(toBusinessStatus(status));
            persistedOrder.setCurrency(currency);
            persistedOrder.setAmount(amount);
            if (persistedOrder.getEventId() == null && eventId != null) {
                persistedOrder.setEventId(eventId);
                gainedEventId = true;
            }
            saveOrderResilient(persistedOrder, paymentIntentId, stripeSessionId);

            if (shouldDispatchSuccessfulPayment(wasSuccessful, status, gainedEventId)) {
                processSuccessfulPayment(persistedOrder, rawPayload, metadata, clientId, customerEmail, customerName);
            } else {
                log.info("stripe_order duplicate stripeSessionId={} status={}", stripeSessionId, status);
            }
            return persistedOrder.getEventId();
        }

        if (isSuccessfulStatus(status)) {
            processSuccessfulPayment(persistedOrder, rawPayload, metadata, clientId, customerEmail, customerName);
        } else {
            log.info("stripe_order pending stripeSessionId={} status={}", stripeSessionId, status);
        }
        return persistedOrder.getEventId();
    }

    private UUID handlePaymentIntent(JsonNode root, String rawPayload, RequestMetadata metadata) {
        JsonNode paymentIntent = root.path("data").path("object");

        String paymentIntentId = text(paymentIntent, "id");
        if (isBlank(paymentIntentId)) {
            throw new IllegalArgumentException("Missing payment intent id");
        }

        String stripeSessionId = text(paymentIntent.path("metadata"), "checkout_session_id");
        if (isBlank(stripeSessionId)) {
            stripeSessionId = paymentIntentId;
        }

        String currency = uppercaseOrDefault(text(paymentIntent, "currency"), "USD");
        long amountMinorUnits = paymentIntent.path("amount_received").asLong(paymentIntent.path("amount").asLong(0));
        BigDecimal amount = amountFromMinorUnits(amountMinorUnits);
        String status = uppercaseOrDefault(text(paymentIntent, "status"), "SUCCEEDED");
        String businessStatus = toBusinessStatus(status);
        UUID eventId = extractTrackingEventIdFromMetadata(paymentIntent.path("metadata"));
        String clientId = text(paymentIntent.path("metadata"), "client_id");
        String customerEmail = extractPaymentIntentCustomerEmail(paymentIntent);
        String customerName = extractPaymentIntentCustomerName(paymentIntent);

        Optional<OrderRecord> existingByPaymentIntent = orderRepository.findByPaymentIntentId(paymentIntentId);
        if (existingByPaymentIntent.isPresent()) {
            OrderRecord existingOrder = existingByPaymentIntent.get();
            boolean wasSuccessful = isSuccessfulStatus(existingOrder.getStatus());
            boolean gainedEventId = false;
            eventId = resolveEventIdWithOrphanFallback(eventId, existingOrder.getEventId(), paymentIntentId, businessStatus);
            existingOrder.setStatus(status);
            existingOrder.setBusinessStatus(businessStatus);
            existingOrder.setAmount(amount);
            existingOrder.setCurrency(currency);
            upsertStripeSessionId(existingOrder, stripeSessionId, paymentIntentId);
            if (existingOrder.getEventId() == null && eventId != null) {
                existingOrder.setEventId(eventId);
                gainedEventId = true;
            }
            saveOrderResilient(existingOrder, paymentIntentId, stripeSessionId);

            if (shouldDispatchSuccessfulPayment(wasSuccessful, status, gainedEventId)) {
                processSuccessfulPayment(existingOrder, rawPayload, metadata, clientId, customerEmail, customerName);
            } else {
                log.info("stripe_order duplicate paymentIntentId={} status={}", paymentIntentId, status);
            }
            return existingOrder.getEventId();
        }

        Optional<OrderRecord> existingBySession = orderRepository.findByStripeSessionId(stripeSessionId);
        if (existingBySession.isPresent()) {
            OrderRecord existingOrder = existingBySession.get();
            boolean wasSuccessful = isSuccessfulStatus(existingOrder.getStatus());
            boolean gainedEventId = false;
            eventId = resolveEventIdWithOrphanFallback(eventId, existingOrder.getEventId(), paymentIntentId, businessStatus);
            existingOrder.setPaymentIntentId(coalesce(existingOrder.getPaymentIntentId(), paymentIntentId));
            existingOrder.setStatus(status);
            existingOrder.setBusinessStatus(businessStatus);
            existingOrder.setAmount(amount);
            existingOrder.setCurrency(currency);
            if (existingOrder.getEventId() == null && eventId != null) {
                existingOrder.setEventId(eventId);
                gainedEventId = true;
            }
            saveOrderResilient(existingOrder, paymentIntentId, stripeSessionId);

            if (shouldDispatchSuccessfulPayment(wasSuccessful, status, gainedEventId)) {
                processSuccessfulPayment(existingOrder, rawPayload, metadata, clientId, customerEmail, customerName);
            } else {
                log.info("stripe_order duplicate stripeSessionId={} status={}", stripeSessionId, status);
            }
            return existingOrder.getEventId();
        }

        OrderRecord orderRecord = new OrderRecord();
        eventId = resolveEventIdWithOrphanFallback(eventId, null, paymentIntentId, businessStatus);
        orderRecord.setId(UUID.randomUUID());
        orderRecord.setEventId(eventId);
        orderRecord.setStripeSessionId(stripeSessionId);
        orderRecord.setPaymentIntentId(paymentIntentId);
        orderRecord.setAmount(amount);
        orderRecord.setCurrency(currency);
        orderRecord.setStatus(status);
        orderRecord.setBusinessStatus(businessStatus);
        orderRecord.setCreatedAt(Instant.now(clock));
        OrderRecord persistedOrder = saveOrderResilient(orderRecord, paymentIntentId, stripeSessionId);

        if (!orderRecord.getId().equals(persistedOrder.getId())) {
            boolean wasSuccessful = isSuccessfulStatus(persistedOrder.getStatus());
            boolean gainedEventId = false;
            eventId = resolveEventIdWithOrphanFallback(eventId, persistedOrder.getEventId(), paymentIntentId, businessStatus);
            persistedOrder.setPaymentIntentId(coalesce(persistedOrder.getPaymentIntentId(), paymentIntentId));
            persistedOrder.setStatus(status);
            persistedOrder.setBusinessStatus(businessStatus);
            persistedOrder.setAmount(amount);
            persistedOrder.setCurrency(currency);
            upsertStripeSessionId(persistedOrder, stripeSessionId, paymentIntentId);
            if (persistedOrder.getEventId() == null && eventId != null) {
                persistedOrder.setEventId(eventId);
                gainedEventId = true;
            }
            saveOrderResilient(persistedOrder, paymentIntentId, stripeSessionId);

            if (shouldDispatchSuccessfulPayment(wasSuccessful, status, gainedEventId)) {
                processSuccessfulPayment(persistedOrder, rawPayload, metadata, clientId, customerEmail, customerName);
            } else {
                log.info("stripe_order duplicate paymentIntentId={} status={}", paymentIntentId, status);
            }
            return persistedOrder.getEventId();
        }

        if (isSuccessfulStatus(status)) {
            processSuccessfulPayment(persistedOrder, rawPayload, metadata, clientId, customerEmail, customerName);
        } else {
            log.info("stripe_order pending paymentIntentId={} status={}", paymentIntentId, status);
        }
        return persistedOrder.getEventId();
    }

    private UUID resolveEventIdWithOrphanFallback(
            UUID webhookEventId,
            UUID existingOrderEventId,
            String paymentIntentId,
            String businessStatus
    ) {
        if (existingOrderEventId != null) {
            return existingOrderEventId;
        }
        if (webhookEventId != null) {
            return webhookEventId;
        }
        if (("FAILED".equalsIgnoreCase(businessStatus) || "PENDING".equalsIgnoreCase(businessStatus))
                && !isBlank(paymentIntentId)) {
            UUID syntheticEventId = UUID.nameUUIDFromBytes(
                    ("stripe_orphan|" + paymentIntentId).getBytes(StandardCharsets.UTF_8)
            );
            log.info(
                    "stripe_order generated_orphan_event_id paymentIntentId={} businessStatus={} eventId={}",
                    paymentIntentId,
                    businessStatus,
                    syntheticEventId
            );
            return syntheticEventId;
        }
        return null;
    }

    private StripeWebhookEvent findOrCreateEvent(String stripeEventId) {
        if (stripeEventId == null || stripeEventId.isBlank()) {
            return null;
        }

        return stripeWebhookEventRepository.findById(stripeEventId)
                .orElseGet(() -> {
                    StripeWebhookEvent e = new StripeWebhookEvent();
                    e.setStripeEventId(stripeEventId);
                    e.setReceivedAt(Instant.now(clock));
                    e.setStatus("RECEIVED");
                    return stripeWebhookEventRepository.save(e);
                });
    }

    private UUID extractTrackingEventIdFromRoot(JsonNode root, String eventType) {
        JsonNode objectNode = root.path("data").path("object");
        if (objectNode.isMissingNode() || objectNode.isNull()) {
            return null;
        }

        if ("checkout.session.completed".equals(eventType)) {
            return extractTrackingEventId(objectNode);
        }

        if ("payment_intent.succeeded".equals(eventType)
                || "payment_intent.payment_failed".equals(eventType)
                || "payment_intent.requires_action".equals(eventType)
                || "payment_intent.processing".equals(eventType)) {
            return extractTrackingEventIdFromMetadata(objectNode.path("metadata"));
        }

        return null;
    }

    private UUID extractTrackingEventId(JsonNode session) {
        UUID fromMetadata = extractTrackingEventIdFromMetadata(session.path("metadata"));
        if (fromMetadata != null) {
            return fromMetadata;
        }

        String clientReferenceId = text(session, "client_reference_id");
        if (!isBlank(clientReferenceId)) {
            return parseUuid(clientReferenceId);
        }

        return null;
    }

    private UUID extractTrackingEventIdFromMetadata(JsonNode metadata) {
        String[] keys = {"eventId", "event_id", "client_reference_id", "tracking_event_id"};
        for (String key : keys) {
            String candidate = text(metadata, key);
            if (!isBlank(candidate)) {
                UUID parsed = parseUuid(candidate);
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        return null;
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal amountFromMinorUnits(long amountMinorUnits) {
        return BigDecimal.valueOf(amountMinorUnits)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String extractStripeEventId(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            return text(root, "id");
        } catch (Exception ex) {
            return null;
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode child = node.path(fieldName);
        if (child.isMissingNode() || child.isNull()) {
            return null;
        }
        String value = child.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String uppercaseOrDefault(String value, String fallback) {
        return (value == null || value.isBlank())
                ? fallback
                : value.toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > 1500 ? error.substring(0, 1500) : error;
    }

    private String extractCheckoutCustomerEmail(JsonNode session) {
        String email = text(session.path("customer_details"), "email");
        if (!isBlank(email)) {
            return email;
        }
        email = text(session, "customer_email");
        if (!isBlank(email)) {
            return email;
        }
        return text(session.path("metadata"), "customer_email");
    }

    private String extractCheckoutCustomerName(JsonNode session) {
        String name = text(session.path("customer_details"), "name");
        if (!isBlank(name)) {
            return name;
        }
        return text(session.path("metadata"), "customer_name");
    }

    private String extractPaymentIntentCustomerEmail(JsonNode paymentIntent) {
        String email = text(paymentIntent, "receipt_email");
        if (!isBlank(email)) {
            return email;
        }
        JsonNode firstCharge = paymentIntent.path("charges").path("data").path(0);
        email = text(firstCharge.path("billing_details"), "email");
        if (!isBlank(email)) {
            return email;
        }
        return text(paymentIntent.path("metadata"), "customer_email");
    }

    private String extractPaymentIntentCustomerName(JsonNode paymentIntent) {
        JsonNode firstCharge = paymentIntent.path("charges").path("data").path(0);
        String name = text(firstCharge.path("billing_details"), "name");
        if (!isBlank(name)) {
            return name;
        }
        return text(paymentIntent.path("metadata"), "customer_name");
    }

    private boolean isSuccessfulStatus(String status) {
        if (isBlank(status)) {
            return false;
        }
        return "SUCCEEDED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status);
    }

    private String toBusinessStatus(String stripeStatus) {
        if (isBlank(stripeStatus)) {
            return "UNKNOWN";
        }

        String normalized = stripeStatus.toUpperCase(Locale.ROOT);
        if ("SUCCEEDED".equals(normalized) || "PAID".equals(normalized)) {
            return "SUCCESS";
        }

        if ("FAILED".equals(normalized)
                || "CANCELED".equals(normalized)
                || "CANCELLED".equals(normalized)
                || "UNPAID".equals(normalized)
                || "REQUIRES_PAYMENT_METHOD".equals(normalized)) {
            return "FAILED";
        }

        if ("PROCESSING".equals(normalized)
                || "REQUIRES_ACTION".equals(normalized)
                || "REQUIRES_CONFIRMATION".equals(normalized)
                || "REQUIRES_CAPTURE".equals(normalized)
                || "PENDING".equals(normalized)
                || "OPEN".equals(normalized)
                || "UNKNOWN".equals(normalized)) {
            return "PENDING";
        }

        return "UNKNOWN";
    }

    private String coalesce(String current, String candidate) {
        if (current == null || current.isBlank()) {
            return candidate;
        }
        return current;
    }

    private boolean shouldDispatchSuccessfulPayment(boolean wasSuccessful, String newStatus, boolean gainedEventId) {
        if (!isSuccessfulStatus(newStatus)) {
            return false;
        }
        return !wasSuccessful || gainedEventId;
    }

    private void upsertStripeSessionId(OrderRecord order, String stripeSessionId, String paymentIntentId) {
        if (isBlank(stripeSessionId)) {
            return;
        }
        String current = order.getStripeSessionId();
        if (isBlank(current) || current.equals(paymentIntentId)) {
            order.setStripeSessionId(stripeSessionId);
        }
    }

    private OrderRecord saveOrderResilient(OrderRecord candidate, String paymentIntentId, String stripeSessionId) {
        try {
            return orderRepository.upsert(candidate);
        } catch (DataIntegrityViolationException ex) {
            DataIntegrityViolationException effectiveException = ex;

            if (candidate.getEventId() != null && isMissingTrackingSessionForeignKey(ex)) {
                trackingService.ensureSessionExists(candidate.getEventId());
                try {
                    return orderRepository.upsert(candidate);
                } catch (DataIntegrityViolationException retryEx) {
                    effectiveException = retryEx;
                }
            }

            if (!isBlank(paymentIntentId)) {
                Optional<OrderRecord> byPaymentIntent = orderRepository.findByPaymentIntentId(paymentIntentId);
                if (byPaymentIntent.isPresent()) {
                    log.info("stripe_order concurrent_duplicate paymentIntentId={} stripeSessionId={}", paymentIntentId, stripeSessionId);
                    return byPaymentIntent.get();
                }
            }

            Optional<OrderRecord> bySession = orderRepository.findByStripeSessionId(stripeSessionId);
            if (bySession.isPresent()) {
                log.info("stripe_order concurrent_duplicate stripeSessionId={} paymentIntentId={}", stripeSessionId, paymentIntentId);
                return bySession.get();
            }
            throw effectiveException;
        }
    }

    private boolean isMissingTrackingSessionForeignKey(DataIntegrityViolationException ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null &&
                    (message.contains("orders_event_id_fkey")
                            || message.contains("is not present in table \"tracking_session\""))) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private void processSuccessfulPayment(
            OrderRecord orderRecord,
            String rawPayload,
            RequestMetadata metadata,
            String clientId,
            String customerEmail,
            String customerName
    ) {
        UUID eventId = orderRecord.getEventId();
        if (eventId == null) {
            log.warn("stripe_order success_deferred_missing_event_id stripeSessionId={} paymentIntentId={}",
                    orderRecord.getStripeSessionId(), orderRecord.getPaymentIntentId());
        } else {
            trackingService.recordPurchaseEvent(
                    eventId,
                    orderRecord.getAmount(),
                    orderRecord.getCurrency(),
                    rawPayload
            );
        }

        PurchaseIntegrationPayload integrationPayload = new PurchaseIntegrationPayload(
                eventId,
                orderRecord.getStripeSessionId(),
                orderRecord.getAmount(),
                orderRecord.getCurrency(),
                Instant.now(clock),
                metadata.userAgent(),
                metadata.clientIp(),
                clientId,
                customerEmail,
                customerName
        );
        purchaseIntegrationDispatcher.dispatchPurchase(integrationPayload);
    }
}
