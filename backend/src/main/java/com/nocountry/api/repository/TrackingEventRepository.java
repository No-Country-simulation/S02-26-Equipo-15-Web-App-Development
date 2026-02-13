package com.nocountry.api.repository;

import com.nocountry.api.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, UUID> {
}
