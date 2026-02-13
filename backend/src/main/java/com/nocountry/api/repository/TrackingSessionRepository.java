package com.nocountry.api.repository;

import com.nocountry.api.entity.TrackingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrackingSessionRepository extends JpaRepository<TrackingSession, UUID> {
}
