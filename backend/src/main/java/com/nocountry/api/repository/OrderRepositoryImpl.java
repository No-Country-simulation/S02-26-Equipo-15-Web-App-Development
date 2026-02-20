package com.nocountry.api.repository;

import com.nocountry.api.entity.OrderRecord;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private static final String INSERT_SQL = """
            INSERT INTO orders (id, event_id, stripe_session_id, payment_intent_id, amount, currency, status, business_status, created_at)
            VALUES (:id, :eventId, :stripeSessionId, :paymentIntentId, :amount, :currency, :status, :businessStatus, :createdAt)
            """;

    private static final String UPDATE_BY_PAYMENT_INTENT_SQL = """
            UPDATE orders
            SET event_id = COALESCE(:eventId, orders.event_id),
                stripe_session_id = CASE
                    WHEN orders.stripe_session_id = :paymentIntentId
                      OR orders.stripe_session_id IS NULL
                      OR orders.stripe_session_id = ''
                    THEN :stripeSessionId
                    ELSE orders.stripe_session_id
                END,
                payment_intent_id = COALESCE(orders.payment_intent_id, :paymentIntentId),
                amount = :amount,
                currency = :currency,
                status = :status,
                business_status = :businessStatus
            WHERE payment_intent_id = :paymentIntentId
            """;

    private static final String UPDATE_BY_STRIPE_SESSION_SQL = """
            UPDATE orders
            SET event_id = COALESCE(:eventId, orders.event_id),
                payment_intent_id = COALESCE(orders.payment_intent_id, :paymentIntentId),
                amount = :amount,
                currency = :currency,
                status = :status,
                business_status = :businessStatus
            WHERE stripe_session_id = :stripeSessionId
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, event_id, stripe_session_id, payment_intent_id, amount, currency, status, business_status, created_at
            FROM orders
            WHERE id = :id
            LIMIT 1
            """;

    private static final String FIND_BY_PAYMENT_INTENT_SQL = """
            SELECT id, event_id, stripe_session_id, payment_intent_id, amount, currency, status, business_status, created_at
            FROM orders
            WHERE payment_intent_id = :paymentIntentId
            LIMIT 1
            """;

    private static final String FIND_BY_STRIPE_SESSION_SQL = """
            SELECT id, event_id, stripe_session_id, payment_intent_id, amount, currency, status, business_status, created_at
            FROM orders
            WHERE stripe_session_id = :stripeSessionId
            LIMIT 1
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<OrderRecord> rowMapper = this::mapOrder;

    public OrderRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OrderRecord upsert(OrderRecord candidate) {
        MapSqlParameterSource params = toParams(candidate);
        DataIntegrityViolationException insertException = null;

        if (hasText(candidate.getPaymentIntentId())) {
            int updatedByPaymentIntent = jdbcTemplate.update(UPDATE_BY_PAYMENT_INTENT_SQL, params);
            if (updatedByPaymentIntent > 0) {
                OrderRecord byPaymentIntent = findByPaymentIntent(candidate.getPaymentIntentId());
                if (byPaymentIntent != null) {
                    return byPaymentIntent;
                }
            }
        }

        int updatedBySession = jdbcTemplate.update(UPDATE_BY_STRIPE_SESSION_SQL, params);
        if (updatedBySession > 0) {
            OrderRecord bySession = findByStripeSession(candidate.getStripeSessionId());
            if (bySession != null) {
                return bySession;
            }
        }

        try {
            int inserted = jdbcTemplate.update(INSERT_SQL, params);
            if (inserted > 0) {
                OrderRecord insertedRow = findById(candidate.getId());
                if (insertedRow != null) {
                    return insertedRow;
                }
            }
        } catch (DataIntegrityViolationException ex) {
            // Parallel webhook may insert first; fallback lookup below handles it.
            // Preserve exception so callers can handle FK retries when fallback misses.
            insertException = ex;
        }

        OrderRecord fallback = findExisting(candidate);
        if (fallback != null) {
            return fallback;
        }

        if (insertException != null) {
            throw insertException;
        }

        throw new IllegalStateException("Unable to upsert order for stripeSessionId=" + candidate.getStripeSessionId());
    }

    private OrderRecord findExisting(OrderRecord candidate) {
        OrderRecord byId = findById(candidate.getId());
        if (byId != null) {
            return byId;
        }

        if (hasText(candidate.getPaymentIntentId())) {
            OrderRecord byPaymentIntent = findByPaymentIntent(candidate.getPaymentIntentId());
            if (byPaymentIntent != null) {
                return byPaymentIntent;
            }
        }

        return findByStripeSession(candidate.getStripeSessionId());
    }

    private OrderRecord findById(UUID id) {
        if (id == null) {
            return null;
        }
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        List<OrderRecord> rows = jdbcTemplate.query(FIND_BY_ID_SQL, params, rowMapper);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private OrderRecord findByPaymentIntent(String paymentIntentId) {
        if (!hasText(paymentIntentId)) {
            return null;
        }
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("paymentIntentId", paymentIntentId);
        List<OrderRecord> rows = jdbcTemplate.query(FIND_BY_PAYMENT_INTENT_SQL, params, rowMapper);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private OrderRecord findByStripeSession(String stripeSessionId) {
        if (!hasText(stripeSessionId)) {
            return null;
        }
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("stripeSessionId", stripeSessionId);
        List<OrderRecord> rows = jdbcTemplate.query(FIND_BY_STRIPE_SESSION_SQL, params, rowMapper);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private MapSqlParameterSource toParams(OrderRecord candidate) {
        Timestamp createdAt = candidate.getCreatedAt() == null ? null : Timestamp.from(candidate.getCreatedAt());
        return new MapSqlParameterSource()
                .addValue("id", candidate.getId())
                .addValue("eventId", candidate.getEventId())
                .addValue("stripeSessionId", candidate.getStripeSessionId())
                .addValue("paymentIntentId", candidate.getPaymentIntentId())
                .addValue("amount", candidate.getAmount())
                .addValue("currency", candidate.getCurrency())
                .addValue("status", candidate.getStatus())
                .addValue("businessStatus", candidate.getBusinessStatus())
                .addValue("createdAt", createdAt);
    }

    private OrderRecord mapOrder(ResultSet rs, int rowNum) throws SQLException {
        OrderRecord order = new OrderRecord();
        order.setId(toUuid(rs.getString("id")));
        order.setEventId(toUuid(rs.getString("event_id")));
        order.setStripeSessionId(rs.getString("stripe_session_id"));
        order.setPaymentIntentId(rs.getString("payment_intent_id"));
        order.setAmount(rs.getBigDecimal("amount"));
        order.setCurrency(rs.getString("currency"));
        order.setStatus(rs.getString("status"));
        order.setBusinessStatus(rs.getString("business_status"));
        order.setCreatedAt(toInstant(rs, "created_at"));
        return order;
    }

    private UUID toUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    private Instant toInstant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
