package com.nocountry.api.controller;

import com.nocountry.api.config.GlobalExceptionHandler;
import com.nocountry.api.config.SecurityConfig;
import com.nocountry.api.dto.TrackRequest;
import com.nocountry.api.service.AdminQueryService;
import com.nocountry.api.service.RequestMetadata;
import com.nocountry.api.service.RequestMetadataResolver;
import com.nocountry.api.service.StripeWebhookService;
import com.nocountry.api.service.TrackRateLimiterService;
import com.nocountry.api.service.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AdminController.class,
        TrackController.class,
        StripeWebhookController.class
})
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = {
        "ADMIN_USER=admin",
        "ADMIN_PASS=admin123"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminQueryService adminQueryService;

    @MockBean
    private TrackingService trackingService;

    @MockBean
    private RequestMetadataResolver requestMetadataResolver;

    @MockBean
    private TrackRateLimiterService trackRateLimiterService;

    @MockBean
    private StripeWebhookService stripeWebhookService;

    @BeforeEach
    void setUp() {
        when(requestMetadataResolver.resolve(any())).thenReturn(new RequestMetadata("ua", "1.1.1.1", "hash"));
        when(trackRateLimiterService.allow(any())).thenReturn(true);
        when(trackingService.track(any(TrackRequest.class), any(RequestMetadata.class)))
                .thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void shouldRequireAuthForAdminHealth() throws Exception {
        mockMvc.perform(get("/api/admin/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAdminHealthWithValidBasicAuth() throws Exception {
        mockMvc.perform(get("/api/admin/health")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ok\":true}"));
    }

    @Test
    void shouldKeepTrackPublic() throws Exception {
        String body = """
                {
                  "eventType": "landing_view",
                  "landing_path": "/"
                }
                """;

        mockMvc.perform(post("/api/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void shouldKeepStripeWebhookPublic() throws Exception {
        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"evt_test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));
    }
}
