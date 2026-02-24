package com.nocountry.api.controller;

import com.nocountry.api.entity.OrderRecord;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderStatusTransitionTest {

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
    void shouldMoveOrderFromPendingToPaidAfterPaymentConfirmed() throws Exception {
        String paymentIntentId = "pi_transition_001";
        String stripeSessionId = "cs_transition_001";

        String processingPayload = TestPayloads.paymentIntentEvent(
                "evt_transition_processing",
                "payment_intent.processing",
                paymentIntentId,
                stripeSessionId,
                "processing"
        );
        String processingSignature = TestPayloads.buildStripeSignatureHeader(processingPayload, STRIPE_SECRET);

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", processingSignature)
                        .content(processingPayload))
                .andExpect(status().isOk());

        OrderRecord pendingOrder = orderRepository.findByPaymentIntentId(paymentIntentId).orElseThrow();
        assertEquals("PROCESSING", pendingOrder.getStatus());
        assertEquals("PENDING", pendingOrder.getBusinessStatus());

        String paidPayload = TestPayloads.checkoutSessionCompleted(
                "evt_transition_paid",
                stripeSessionId,
                paymentIntentId,
                "paid"
        );
        String paidSignature = TestPayloads.buildStripeSignatureHeader(paidPayload, STRIPE_SECRET);

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", paidSignature)
                        .content(paidPayload))
                .andExpect(status().isOk());

        OrderRecord paidOrder = orderRepository.findByPaymentIntentId(paymentIntentId).orElseThrow();
        assertEquals("PAID", paidOrder.getStatus());
        assertEquals("SUCCESS", paidOrder.getBusinessStatus());
        assertEquals(1L, orderRepository.count());
    }
}
