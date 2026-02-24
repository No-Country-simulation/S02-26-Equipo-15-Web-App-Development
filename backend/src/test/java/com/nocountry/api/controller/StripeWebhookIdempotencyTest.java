package com.nocountry.api.controller;

import com.nocountry.api.entity.OrderRecord;
import com.nocountry.api.entity.StripeWebhookEvent;
import com.nocountry.api.integration.PurchaseIntegrationDispatcher;
import com.nocountry.api.repository.OrderRepository;
import com.nocountry.api.repository.StripeWebhookEventRepository;
import com.nocountry.api.repository.TrackingEventRepository;
import com.nocountry.api.repository.TrackingSessionRepository;
import com.nocountry.api.support.TestPayloads;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StripeWebhookIdempotencyTest {

    private static final String STRIPE_SECRET = "test_webhook_secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StripeWebhookEventRepository stripeWebhookEventRepository;

    @Autowired
    private TrackingSessionRepository trackingSessionRepository;

    @Autowired
    private TrackingEventRepository trackingEventRepository;

    @MockBean
    private PurchaseIntegrationDispatcher purchaseIntegrationDispatcher;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        stripeWebhookEventRepository.deleteAll();
        trackingEventRepository.deleteAll();
        trackingSessionRepository.deleteAll();
    }

    @Test
    void shouldProcessSameStripeEventOnlyOnce() throws Exception {
        String stripeEventId = "evt_idem_001";
        String stripeSessionId = "cs_idem_001";
        String paymentIntentId = "pi_idem_001";

        String payload = TestPayloads.checkoutSessionCompleted(
                stripeEventId,
                stripeSessionId,
                paymentIntentId,
                "paid"
        );
        String signature = TestPayloads.buildStripeSignatureHeader(payload, STRIPE_SECRET);

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        assertEquals(1L, stripeWebhookEventRepository.count());
        assertEquals(1L, orderRepository.count());

        StripeWebhookEvent webhookEvent = stripeWebhookEventRepository.findById(stripeEventId).orElseThrow();
        assertEquals("PROCESSED", webhookEvent.getStatus());
        assertNull(webhookEvent.getError());

        OrderRecord order = orderRepository.findByStripeSessionId(stripeSessionId).orElseThrow();
        assertEquals(paymentIntentId, order.getPaymentIntentId());
        assertEquals("PAID", order.getStatus());
        assertEquals("SUCCESS", order.getBusinessStatus());
    }
}
