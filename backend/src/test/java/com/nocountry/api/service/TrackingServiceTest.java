package com.nocountry.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.api.config.AppProperties;
import com.nocountry.api.dto.TrackRequest;
import com.nocountry.api.entity.TrackingSession;
import com.nocountry.api.repository.TrackingEventRepository;
import com.nocountry.api.repository.TrackingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private TrackingSessionRepository trackingSessionRepository;

    @Mock
    private TrackingEventRepository trackingEventRepository;

    private TrackingService trackingService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getTracking().setEnabled(true);

        Clock clock = Clock.fixed(Instant.parse("2026-02-13T10:00:00Z"), ZoneOffset.UTC);
        trackingService = new TrackingService(
                trackingSessionRepository,
                trackingEventRepository,
                new ObjectMapper(),
                clock,
                appProperties
        );

        when(trackingSessionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(trackingEventRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldNotOverwriteFirstTouchUtmSource() {
        UUID eventId = UUID.randomUUID();

        TrackingSession existing = new TrackingSession();
        existing.setEventId(eventId);
        existing.setCreatedAt(Instant.parse("2026-02-10T10:00:00Z"));
        existing.setLastSeenAt(Instant.parse("2026-02-10T10:00:00Z"));
        existing.setUtmSource("google");

        when(trackingSessionRepository.findById(eventId)).thenReturn(Optional.of(existing));

        TrackRequest request = new TrackRequest();
        request.setEventId(eventId.toString());
        request.setEventType("landing_view");
        request.setUtmSource("facebook");

        RequestMetadata metadata = new RequestMetadata("ua", "1.1.1.1", "hash");

        trackingService.track(request, metadata);

        ArgumentCaptor<TrackingSession> sessionCaptor = ArgumentCaptor.forClass(TrackingSession.class);
        verify(trackingSessionRepository).saveAndFlush(sessionCaptor.capture());

        TrackingSession saved = sessionCaptor.getValue();
        assertEquals("google", saved.getUtmSource());
    }

    @Test
    void shouldHandleConcurrentSessionInsertIdempotently() {
        UUID eventId = UUID.randomUUID();

        TrackingSession existing = new TrackingSession();
        existing.setEventId(eventId);
        existing.setCreatedAt(Instant.parse("2026-02-10T10:00:00Z"));
        existing.setLastSeenAt(Instant.parse("2026-02-10T10:00:00Z"));
        existing.setUtmSource("google");

        when(trackingSessionRepository.findById(eventId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));

        doThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"))
                .when(trackingSessionRepository)
                .saveAndFlush(argThat(session -> session.getCreatedAt() != null && "facebook".equals(session.getUtmSource())));

        TrackRequest request = new TrackRequest();
        request.setEventId(eventId.toString());
        request.setEventType("landing_view");
        request.setUtmSource("facebook");

        RequestMetadata metadata = new RequestMetadata("ua", "1.1.1.1", "hash");

        trackingService.track(request, metadata);

        verify(trackingSessionRepository).saveAndFlush(argThat(session -> "google".equals(session.getUtmSource())));
    }
}
