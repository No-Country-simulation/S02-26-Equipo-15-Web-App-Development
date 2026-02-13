package com.nocountry.api.repository;

import com.nocountry.api.entity.IntegrationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IntegrationLogRepository extends JpaRepository<IntegrationLog, UUID> {
}
