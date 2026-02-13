package com.nocountry.api.controller;

import com.nocountry.api.config.GlobalExceptionHandler;
import com.nocountry.api.dto.TrackRequest;
import com.nocountry.api.service.RequestMetadata;
import com.nocountry.api.service.RequestMetadataResolver;
import com.nocountry.api.service.TrackRateLimiterService;
import com.nocountry.api.service.TrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TrackController.class)
@Import(GlobalExceptionHandler.class)
class TrackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrackingService trackingService;

    @MockBean
    private RequestMetadataResolver requestMetadataResolver;

    @MockBean
    private TrackRateLimiterService trackRateLimiterService;

    @Test
    void shouldTrackValidRequest() throws Exception {
        UUID eventId = UUID.randomUUID();

        when(requestMetadataResolver.resolve(any())).thenReturn(new RequestMetadata("ua", "1.1.1.1", "hash"));
        when(trackRateLimiterService.allow(any())).thenReturn(true);
        when(trackingService.track(any(TrackRequest.class), any(RequestMetadata.class))).thenReturn(eventId);

        String json = """
                {
                  "eventType": "landing_view",
                  "utm_source": "google",
                  "utm_medium": "cpc",
                  "utm_campaign": "camp",
                  "utm_term": "term",
                  "utm_content": "content",
                  "gclid": "gclid",
                  "fbclid": "fbclid",
                  "landing_path": "/"
                }
                """;

        mockMvc.perform(post("/api/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()));
    }

    @Test
    void shouldRejectInvalidEventType() throws Exception {
        String json = """
                {
                  "eventType": "invalid_event",
                  "utm_source": "google",
                  "landing_path": "/"
                }
                """;

        mockMvc.perform(post("/api/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
