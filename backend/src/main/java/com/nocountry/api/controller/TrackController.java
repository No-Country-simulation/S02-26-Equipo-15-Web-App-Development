package com.nocountry.api.controller;

import com.nocountry.api.dto.TrackRequest;
import com.nocountry.api.dto.TrackResponse;
import com.nocountry.api.service.RequestMetadata;
import com.nocountry.api.service.RequestMetadataResolver;
import com.nocountry.api.service.TooManyRequestsException;
import com.nocountry.api.service.TrackRateLimiterService;
import com.nocountry.api.service.TrackingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TrackController {

    private final TrackingService trackingService;
    private final RequestMetadataResolver requestMetadataResolver;
    private final TrackRateLimiterService trackRateLimiterService;

    public TrackController(
            TrackingService trackingService,
            RequestMetadataResolver requestMetadataResolver,
            TrackRateLimiterService trackRateLimiterService
    ) {
        this.trackingService = trackingService;
        this.requestMetadataResolver = requestMetadataResolver;
        this.trackRateLimiterService = trackRateLimiterService;
    }

    @PostMapping("/track")
    public TrackResponse track(@Valid @RequestBody TrackRequest request, HttpServletRequest servletRequest) {
        RequestMetadata metadata = requestMetadataResolver.resolve(servletRequest);
        if (!trackRateLimiterService.allow(metadata.ipHash())) {
            throw new TooManyRequestsException("Rate limit exceeded for this IP");
        }

        UUID eventId = trackingService.track(request, metadata);
        return new TrackResponse(eventId.toString());
    }
}
