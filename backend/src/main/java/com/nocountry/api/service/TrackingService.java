package com.nocountry.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.api.config.AppProperties;
import com.nocountry.api.dto.TrackRequest;
import com.nocountry.api.entity.TrackingEvent;
import com.nocountry.api.entity.TrackingSession;
import com.nocountry.api.repository.TrackingEventRepository;
import com.nocountry.api.repository.TrackingSessionRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    private final TrackingSessionRepository trackingSessionRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final AppProperties appProperties;

    public TrackingService(
            TrackingSessionRepository trackingSessionRepository,
            TrackingEventRepository trackingEventRepository,
            ObjectMapper objectMapper,
            Clock clock,
            AppProperties appProperties
    ) {
        this.trackingSessionRepository = trackingSessionRepository;
        this.trackingEventRepository = trackingEventRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.appProperties = appProperties;
    }

    @Transactional
    public UUID track(TrackRequest request, RequestMetadata metadata) {
        UUID eventId = parseOrGenerateEventId(request.getEventId());

        if (!appProperties.getTracking().isEnabled()) {
            log.info("track_event disabled=true eventId={} eventType={}", eventId, request.getEventType());
            return eventId;
        }

        Instant now = Instant.now(clock);

        TrackingSession session = trackingSessionRepository.findById(eventId)
                .orElseGet(() -> newSession(eventId, now, request, metadata));

        if (session.getCreatedAt() == null) {
            session = newSession(eventId, now, request, metadata);
        } else {
            applyFirstTouch(session, request);
            applyMetadataIfMissing(session, metadata);
            if (isBlank(session.getLandingPath())) {
                session.setLandingPath(request.getLandingPath());
            }
            session.setLastSeenAt(now);
        }

        saveSessionWithRetry(session, eventId, request, metadata, now);

        TrackingEvent event = new TrackingEvent();
        event.setId(buildEventIdempotencyId(eventId, request.getEventType()));
        event.setEventId(eventId);
        event.setEventType(request.getEventType());
        event.setCreatedAt(now);
        event.setPayloadJson(serializeSafe(request));
        saveEventIdempotent(event, eventId, request.getEventType());

        log.info("track_event eventId={} eventType={}", eventId, request.getEventType());
        return eventId;
    }

    @Transactional
    public void recordPurchaseEvent(UUID eventId, BigDecimal amount, String currency, String payloadJson) {
        if (eventId == null || !appProperties.getTracking().isEnabled()) {
            return;
        }

        Instant now = Instant.now(clock);

        TrackingSession session = trackingSessionRepository.findById(eventId)
                .orElseGet(() -> {
                    TrackingSession s = new TrackingSession();
                    s.setEventId(eventId);
                    s.setCreatedAt(now);
                    s.setLastSeenAt(now);
                    return s;
                });

        session.setLastSeenAt(now);
        savePurchaseSessionWithRetry(session, eventId, now);

        TrackingEvent purchaseEvent = new TrackingEvent();
        purchaseEvent.setId(buildEventIdempotencyId(eventId, "purchase"));
        purchaseEvent.setEventId(eventId);
        purchaseEvent.setEventType("purchase");
        purchaseEvent.setCreatedAt(now);
        purchaseEvent.setCurrency(currency);
        purchaseEvent.setValue(amount);
        purchaseEvent.setPayloadJson(payloadJson);
        saveEventIdempotent(purchaseEvent, eventId, "purchase");

        log.info("track_event eventId={} eventType=purchase", eventId);
    }

    private TrackingSession newSession(UUID eventId, Instant now, TrackRequest request, RequestMetadata metadata) {
        TrackingSession session = new TrackingSession();
        session.setEventId(eventId);
        session.setCreatedAt(now);
        session.setLastSeenAt(now);
        session.setUtmSource(request.getUtmSource());
        session.setUtmMedium(request.getUtmMedium());
        session.setUtmCampaign(request.getUtmCampaign());
        session.setUtmTerm(request.getUtmTerm());
        session.setUtmContent(request.getUtmContent());
        session.setGclid(request.getGclid());
        session.setFbclid(request.getFbclid());
        session.setLandingPath(request.getLandingPath());
        session.setUserAgent(metadata.userAgent());
        session.setIpHash(metadata.ipHash());
        return session;
    }

    private void applyFirstTouch(TrackingSession session, TrackRequest request) {
        session.setUtmSource(coalesce(session.getUtmSource(), request.getUtmSource()));
        session.setUtmMedium(coalesce(session.getUtmMedium(), request.getUtmMedium()));
        session.setUtmCampaign(coalesce(session.getUtmCampaign(), request.getUtmCampaign()));
        session.setUtmTerm(coalesce(session.getUtmTerm(), request.getUtmTerm()));
        session.setUtmContent(coalesce(session.getUtmContent(), request.getUtmContent()));
        session.setGclid(coalesce(session.getGclid(), request.getGclid()));
        session.setFbclid(coalesce(session.getFbclid(), request.getFbclid()));
    }

    private void applyMetadataIfMissing(TrackingSession session, RequestMetadata metadata) {
        if (isBlank(session.getUserAgent())) {
            session.setUserAgent(metadata.userAgent());
        }
        if (isBlank(session.getIpHash())) {
            session.setIpHash(metadata.ipHash());
        }
    }

    private String coalesce(String existing, String incoming) {
        return isBlank(existing) ? incoming : existing;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private UUID parseOrGenerateEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(eventId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("eventId must be a valid UUID");
        }
    }

    private String serializeSafe(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private void saveSessionWithRetry(
            TrackingSession session,
            UUID eventId,
            TrackRequest request,
            RequestMetadata metadata,
            Instant now
    ) {
        try {
            trackingSessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException ex) {
            TrackingSession existing = trackingSessionRepository.findById(eventId)
                    .orElseThrow(() -> ex);
            applyFirstTouch(existing, request);
            applyMetadataIfMissing(existing, metadata);
            if (isBlank(existing.getLandingPath())) {
                existing.setLandingPath(request.getLandingPath());
            }
            existing.setLastSeenAt(now);
            trackingSessionRepository.saveAndFlush(existing);
            log.info("track_event idempotent_session eventId={} eventType={}", eventId, request.getEventType());
        }
    }

    private void savePurchaseSessionWithRetry(TrackingSession session, UUID eventId, Instant now) {
        try {
            trackingSessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException ex) {
            TrackingSession existing = trackingSessionRepository.findById(eventId)
                    .orElseThrow(() -> ex);
            existing.setLastSeenAt(now);
            trackingSessionRepository.saveAndFlush(existing);
            log.info("track_event idempotent_session eventId={} eventType=purchase", eventId);
        }
    }

    private void saveEventIdempotent(TrackingEvent event, UUID eventId, String eventType) {
        try {
            trackingEventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException ex) {
            log.info("track_event idempotent_event eventId={} eventType={}", eventId, eventType);
        }
    }

    private UUID buildEventIdempotencyId(UUID eventId, String eventType) {
        String key = eventId + "|" + eventType;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }
}
