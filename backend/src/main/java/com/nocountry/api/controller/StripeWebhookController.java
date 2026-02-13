package com.nocountry.api.controller;

import com.nocountry.api.service.RequestMetadata;
import com.nocountry.api.service.RequestMetadataResolver;
import com.nocountry.api.service.StripeWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;
    private final RequestMetadataResolver requestMetadataResolver;

    public StripeWebhookController(StripeWebhookService stripeWebhookService, RequestMetadataResolver requestMetadataResolver) {
        this.stripeWebhookService = stripeWebhookService;
        this.requestMetadataResolver = requestMetadataResolver;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
            HttpServletRequest request
    ) {
        RequestMetadata metadata = requestMetadataResolver.resolve(request);
        stripeWebhookService.process(payload, stripeSignature, metadata);
        return ResponseEntity.ok(Map.of("received", true));
    }
}
