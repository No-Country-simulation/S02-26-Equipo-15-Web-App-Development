package com.nocountry.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.api.entity.IntegrationLog;
import com.nocountry.api.repository.IntegrationLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class IntegrationLogService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationLogService.class);

    private final IntegrationLogRepository integrationLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IntegrationLogService(
            IntegrationLogRepository integrationLogRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.integrationLogRepository = integrationLogRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void log(
            String integration,
            UUID eventId,
            String status,
            Integer httpStatus,
            Integer latencyMs,
            Object requestPayload,
            Object responsePayload,
            String errorMessage
    ) {
        logWithReference(
                integration,
                eventId == null ? null : eventId.toString(),
                status,
                httpStatus,
                latencyMs,
                requestPayload,
                responsePayload,
                errorMessage
        );
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void logWithReference(
            String integration,
            String referenceId,
            String status,
            Integer httpStatus,
            Integer latencyMs,
            Object requestPayload,
            Object responsePayload,
            String errorMessage
    ) {
        try {
            IntegrationLog row = new IntegrationLog();
            row.setId(UUID.randomUUID());
            row.setIntegration(integration);
            row.setReferenceId(trimReference(referenceId));
            row.setStatus(status);
            row.setHttpStatus(httpStatus);
            row.setLatencyMs(latencyMs);
            row.setRequestPayload(serializeSafe(requestPayload));
            row.setResponsePayload(serializeSafe(responsePayload));
            row.setErrorMessage(trim(errorMessage));
            row.setCreatedAt(Instant.now(clock));
            integrationLogRepository.save(row);
        } catch (Exception ex) {
            log.warn("integration_log status=failed_to_persist integration={} referenceId={} error={}",
                    integration, referenceId, ex.getMessage());
        }
    }

    private String serializeSafe(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            try {
                return objectMapper.writeValueAsString(String.valueOf(payload));
            } catch (JsonProcessingException ignored) {
                return "\"serialization_failed\"";
            }
        }
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 2000 ? value.substring(0, 2000) : value;
    }

    private String trimReference(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() > 255 ? value.substring(0, 255) : value;
    }
}
