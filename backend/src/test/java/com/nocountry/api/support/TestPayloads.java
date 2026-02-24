package com.nocountry.api.support;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public final class TestPayloads {

    private TestPayloads() {
    }

    public static String checkoutSessionCompleted(
            String stripeEventId,
            String stripeSessionId,
            String paymentIntentId,
            String paymentStatus
    ) {
        return """
                {
                  "id": "%s",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "%s",
                      "payment_intent": "%s",
                      "currency": "usd",
                      "amount_total": 4999,
                      "payment_status": "%s",
                      "metadata": {
                        "client_id": "client-test"
                      }
                    }
                  }
                }
                """.formatted(stripeEventId, stripeSessionId, paymentIntentId, paymentStatus);
    }

    public static String paymentIntentEvent(
            String stripeEventId,
            String eventType,
            String paymentIntentId,
            String stripeSessionId,
            String status
    ) {
        UUID trackingEventId = UUID.nameUUIDFromBytes(paymentIntentId.getBytes(StandardCharsets.UTF_8));
        return """
                {
                  "id": "%s",
                  "type": "%s",
                  "data": {
                    "object": {
                      "id": "%s",
                      "currency": "usd",
                      "amount": 4999,
                      "amount_received": 4999,
                      "status": "%s",
                      "metadata": {
                        "checkout_session_id": "%s",
                        "eventId": "%s"
                      }
                    }
                  }
                }
                """.formatted(stripeEventId, eventType, paymentIntentId, status, stripeSessionId, trackingEventId);
    }

    public static String buildStripeSignatureHeader(String payload, String secret) {
        long timestamp = Instant.now().getEpochSecond();
        String signedPayload = timestamp + "." + payload;
        String signature = hmacSha256(secret, signedPayload);
        return "t=" + timestamp + ",v1=" + signature;
    }

    private static String hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate Stripe test signature", ex);
        }
    }
}
