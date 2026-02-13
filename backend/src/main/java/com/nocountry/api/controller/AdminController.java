package com.nocountry.api.controller;

import com.nocountry.api.dto.EventDto;
import com.nocountry.api.dto.MetricsDto;
import com.nocountry.api.dto.PagedResponse;
import com.nocountry.api.dto.SessionDetailDto;
import com.nocountry.api.dto.SessionSummaryDto;
import com.nocountry.api.service.AdminQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminQueryService adminQueryService;

    public AdminController(AdminQueryService adminQueryService) {
        this.adminQueryService = adminQueryService;
    }

    @GetMapping("/sessions")
    public PagedResponse<SessionSummaryDto> sessions(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(value = "utm_source", required = false) String utmSource,
            @RequestParam(value = "limit", defaultValue = "50") @Min(1) @Max(500) int limit,
            @RequestParam(value = "offset", defaultValue = "0") @Min(0) int offset
    ) {
        List<SessionSummaryDto> items = adminQueryService.findSessions(from, to, utmSource, limit, offset);
        return new PagedResponse<>(items, limit, offset);
    }

    @GetMapping("/sessions/{eventId}")
    public SessionDetailDto sessionDetail(@PathVariable("eventId") UUID eventId) {
        return adminQueryService.findSessionDetail(eventId);
    }

    @GetMapping("/events")
    public PagedResponse<EventDto> events(
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(value = "limit", defaultValue = "50") @Min(1) @Max(500) int limit,
            @RequestParam(value = "offset", defaultValue = "0") @Min(0) int offset
    ) {
        List<EventDto> items = adminQueryService.findEvents(eventType, from, to, limit, offset);
        return new PagedResponse<>(items, limit, offset);
    }

    @GetMapping("/metrics")
    public MetricsDto metrics(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return adminQueryService.metrics(from, to);
    }
}
