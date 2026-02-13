package com.nocountry.api.integration.stripe;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class StripeSignatureVerifier {

    private static final long DEFAULT_TOLERANCE_SECONDS = 300;

    public boolean verify(String payload, String stripeSignatureHeader, String secret) {
        if (payload == null || stripeSignatureHeader == null || secret == null || secret.isBlank()) {
            return false;
        }

        SignatureParts parts = parseHeader(stripeSignatureHeader);
        if (parts.timestamp == null || parts.v1Signatures.isEmpty()) {
            return false;
        }

        long now = Instant.now().getEpochSecond();
        long age = Math.abs(now - parts.timestamp);
        if (age > DEFAULT_TOLERANCE_SECONDS) {
            return false;
        }

        String signedPayload = parts.timestamp + "." + payload;
        String expected = hmacSha256(secret, signedPayload);

        for (String candidate : parts.v1Signatures) {
            if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }
        return false;
    }

    private SignatureParts parseHeader(String header) {
        Long timestamp = null;
        List<String> signatures = new ArrayList<>();

        for (String part : header.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            String key = kv[0].trim();
            String value = kv[1].trim();
            if ("t".equals(key)) {
                try {
                    timestamp = Long.parseLong(value);
                } catch (NumberFormatException ignored) {
                    timestamp = null;
                }
            }
            if ("v1".equals(key)) {
                signatures.add(value);
            }
        }

        return new SignatureParts(timestamp, signatures);
    }

    private String hmacSha256(String secret, String payload) {
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
            throw new IllegalStateException("Unable to compute Stripe signature", ex);
        }
    }

    private record SignatureParts(Long timestamp, List<String> v1Signatures) {
    }
}
