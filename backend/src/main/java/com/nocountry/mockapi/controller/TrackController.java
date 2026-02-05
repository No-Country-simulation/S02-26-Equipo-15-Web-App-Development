package com.nocountry.mockapi.controller;

import com.nocountry.mockapi.dto.TrackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TrackController {

    private static final Logger log = LoggerFactory.getLogger(TrackController.class);

    @PostMapping("/track")
    public Map<String, Object> track(@RequestBody TrackRequest request) {
        String eventId = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();

        log.info("Track event: eventId={} eventType={} utm_source={} utm_medium={} utm_campaign={} utm_term={} utm_content={} gclid={} fbclid={} landing_path={}",
                eventId,
                request.getEventType(),
                request.getUtm_source(),
                request.getUtm_medium(),
                request.getUtm_campaign(),
                request.getUtm_term(),
                request.getUtm_content(),
                request.getGclid(),
                request.getFbclid(),
                request.getLanding_path());

        return Map.of(
                "ok", true,
                "eventId", eventId,
                "firstTouchAt", timestamp,
                "lastUpdatedAt", timestamp
        );
    }
}
