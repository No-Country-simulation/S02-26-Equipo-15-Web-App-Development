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
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Transactional
    public void process(String payload, String signatureHeader, RequestMetadata metadata) {
        String stripeEventId = extractStripeEventId(payload);
        StripeWebhookEvent webhookEvent = null;

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

            if ("checkout.session.completed".equals(eventType)) {
                handleCheckoutCompleted(root, payload, metadata);
            } else if ("payment_intent.succeeded".equals(eventType)) {
                handlePaymentIntentSucceeded(root, payload, metadata);
            }

            if (webhookEvent != null) {
                webhookEvent.setStatus("PROCESSED");
                webhookEvent.setProcessedAt(Instant.now(clock));
                webhookEvent.setError(null);
                stripeWebhookEventRepository.save(webhookEvent);
            }

            log.info("stripe_webhook status=processed stripeEventId={} type={}", stripeEventId, eventType);
        } catch (Exception ex) {
            if (webhookEvent != null) {
                webhookEvent.setStatus("FAILED");
                webhookEvent.setProcessedAt(Instant.now(clock));
                webhookEvent.setError(trimError(ex.getMessage()));
                stripeWebhookEventRepository.save(webhookEvent);
            }
            log.warn("stripe_webhook status=failed stripeEventId={} error={}", stripeEventId, ex.getMessage());
        }
    }

    private void handleCheckoutCompleted(JsonNode root, String rawPayload, RequestMetadata metadata) {
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

        Optional<OrderRecord> existing = orderRepository.findByStripeSessionId(stripeSessionId);
        if (existing.isPresent()) {
            OrderRecord existingOrder = existing.get();
            boolean wasSuccessful = isSuccessfulStatus(existingOrder.getStatus());
            existingOrder.setPaymentIntentId(coalesce(existingOrder.getPaymentIntentId(), paymentIntentId));
            existingOrder.setStatus(status);
            existingOrder.setCurrency(currency);
            existingOrder.setAmount(amount);
            if (existingOrder.getEventId() == null && eventId != null) {
                existingOrder.setEventId(eventId);
            }
            orderRepository.save(existingOrder);

            if (!wasSuccessful && isSuccessfulStatus(status)) {
                processSuccessfulPayment(existingOrder, rawPayload, metadata, clientId, customerEmail, customerName);
            } else {
                log.info("stripe_order duplicate stripeSessionId={} status={}", stripeSessionId, status);
            }
            return;
        }

        OrderRecord orderRecord = new OrderRecord();
        orderRecord.setId(UUID.randomUUID());
        orderRecord.setEventId(eventId);
        orderRecord.setStripeSessionId(stripeSessionId);
        orderRecord.setPaymentIntentId(paymentIntentId);
        orderRecord.setAmount(amount);
        orderRecord.setCurrency(currency);
        orderRecord.setStatus(status);
        orderRecord.setCreatedAt(Instant.now(clock));
        orderRepository.save(orderRecord);

        if (isSuccessfulStatus(status)) {
            processSuccessfulPayment(orderRecord, rawPayload, metadata, clientId, customerEmail, customerName);
        } else {
            log.info("stripe_order pending stripeSessionId={} status={}", stripeSessionId, status);
        }
    }

    private void handlePaymentIntentSucceeded(JsonNode root, String rawPayload, RequestMetadata metadata) {
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
        UUID eventId = extractTrackingEventIdFromMetadata(paymentIntent.path("metadata"));
        String clientId = text(paymentIntent.path("metadata"), "client_id");
        String customerEmail = extractPaymentIntentCustomerEmail(paymentIntent);
        String customerName = extractPaymentIntentCustomerName(paymentIntent);

        Optional<OrderRecord> existingByPaymentIntent = orderRepository.findByPaymentIntentId(paymentIntentId);
        if (existingByPaymentIntent.isPresent()) {
            OrderRecord existingOrder = existingByPaymentIntent.get();
            boolean wasSuccessful = isSuccessfulStatus(existingOrder.getStatus());
            existingOrder.setStatus(status);
            existingOrder.setAmount(amount);
            existingOrder.setCurrency(currency);
            existingOrder.setStripeSessionId(coalesce(existingOrder.getStripeSessionId(), stripeSessionId));
            if (existingOrder.getEventId() == null && eventId != null) {
                existingOrder.setEventId(eventId);
            }
            orderRepository.save(existingOrder);

            if (!wasSuccessful && isSuccessfulStatus(status)) {
                processSuccessfulPayment(existingOrder, rawPayload, metadata, clientId, customerEmail, customerName);
            } else {
                log.info("stripe_order duplicate paymentIntentId={} status={}", paymentIntentId, status);
            }
            return;
        }

        Optional<OrderRecord> existingBySession = orderRepository.findByStripeSessionId(stripeSessionId);
        if (existingBySession.isPresent()) {
            OrderRecord existingOrder = existingBySession.get();
            boolean wasSuccessful = isSuccessfulStatus(existingOrder.getStatus());
            existingOrder.setPaymentIntentId(coalesce(existingOrder.getPaymentIntentId(), paymentIntentId));
            existingOrder.setStatus(status);
            existingOrder.setAmount(amount);
            existingOrder.setCurrency(currency);
            if (existingOrder.getEventId() == null && eventId != null) {
                existingOrder.setEventId(eventId);
            }
            orderRepository.save(existingOrder);

            if (!wasSuccessful && isSuccessfulStatus(status)) {
                processSuccessfulPayment(existingOrder, rawPayload, metadata, clientId, customerEmail, customerName);
            } else {
                log.info("stripe_order duplicate stripeSessionId={} status={}", stripeSessionId, status);
            }
            return;
        }

        OrderRecord orderRecord = new OrderRecord();
        orderRecord.setId(UUID.randomUUID());
        orderRecord.setEventId(eventId);
        orderRecord.setStripeSessionId(stripeSessionId);
        orderRecord.setPaymentIntentId(paymentIntentId);
        orderRecord.setAmount(amount);
        orderRecord.setCurrency(currency);
        orderRecord.setStatus(status);
        orderRecord.setCreatedAt(Instant.now(clock));
        orderRepository.save(orderRecord);

        if (isSuccessfulStatus(status)) {
            processSuccessfulPayment(orderRecord, rawPayload, metadata, clientId, customerEmail, customerName);
        } else {
            log.info("stripe_order pending paymentIntentId={} status={}", paymentIntentId, status);
        }
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

    private UUID extractTrackingEventId(JsonNode session) {
        String metadataEventId = text(session.path("metadata"), "eventId");
        if (!isBlank(metadataEventId)) {
            return parseUuid(metadataEventId);
        }

        String clientReferenceId = text(session, "client_reference_id");
        if (!isBlank(clientReferenceId)) {
            return parseUuid(clientReferenceId);
        }

        return null;
    }

    private UUID extractTrackingEventIdFromMetadata(JsonNode metadata) {
        String metadataEventId = text(metadata, "eventId");
        if (!isBlank(metadataEventId)) {
            return parseUuid(metadataEventId);
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

    private String coalesce(String current, String candidate) {
        if (current == null || current.isBlank()) {
            return candidate;
        }
        return current;
    }

    private void processSuccessfulPayment(
            OrderRecord orderRecord,
            String rawPayload,
            RequestMetadata metadata,
            String clientId,
            String customerEmail,
            String customerName
    ) {
        if (orderRecord.getEventId() != null) {
            trackingService.recordPurchaseEvent(
                    orderRecord.getEventId(),
                    orderRecord.getAmount(),
                    orderRecord.getCurrency(),
                    rawPayload
            );
        }

        PurchaseIntegrationPayload integrationPayload = new PurchaseIntegrationPayload(
                orderRecord.getEventId(),
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
