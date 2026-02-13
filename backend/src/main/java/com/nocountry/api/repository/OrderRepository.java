package com.nocountry.api.repository;

import com.nocountry.api.entity.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderRecord, UUID> {

    boolean existsByStripeSessionId(String stripeSessionId);

    Optional<OrderRecord> findByStripeSessionId(String stripeSessionId);

    Optional<OrderRecord> findByPaymentIntentId(String paymentIntentId);
}
