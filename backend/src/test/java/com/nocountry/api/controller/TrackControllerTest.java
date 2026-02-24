package com.nocountry.api.controller;

import com.nocountry.api.entity.TrackingSession;
import com.nocountry.api.repository.TrackingEventRepository;
import com.nocountry.api.repository.TrackingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrackingSessionRepository trackingSessionRepository;

    @Autowired
    private TrackingEventRepository trackingEventRepository;

    @BeforeEach
    void cleanDatabase() {
        trackingEventRepository.deleteAll();
        trackingSessionRepository.deleteAll();
    }

    @Test
    void shouldCreateTrackingSessionAndEvent() throws Exception {
        UUID eventId = UUID.randomUUID();

        String json = """
                {
                  "eventId": "%s",
                  "eventType": "landing_view",
                  "utm_source": "google",
                  "utm_medium": "cpc",
                  "utm_campaign": "spring_sale",
                  "landing_path": "/pricing"
                }
                """.formatted(eventId);

        mockMvc.perform(post("/api/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()));

        TrackingSession session = trackingSessionRepository.findById(eventId).orElseThrow();
        assertEquals("google", session.getUtmSource());
        assertEquals("cpc", session.getUtmMedium());
        assertEquals("spring_sale", session.getUtmCampaign());
        assertEquals("/pricing", session.getLandingPath());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getLastSeenAt());

        long landingViewCount = trackingEventRepository.findAll().stream()
                .filter(event -> eventId.equals(event.getEventId()))
                .filter(event -> "landing_view".equals(event.getEventType()))
                .count();

        assertEquals(1L, landingViewCount);
        assertTrue(trackingEventRepository.findAll().stream().allMatch(event -> event.getId() != null));
    }
}
