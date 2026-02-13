package com.nocountry.api.service;

import com.nocountry.api.dto.EventDto;
import com.nocountry.api.dto.MetricsDto;
import com.nocountry.api.dto.OrderDto;
import com.nocountry.api.dto.SessionDetailDto;
import com.nocountry.api.dto.SessionSummaryDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminQueryService {

    private final JdbcTemplate jdbcTemplate;

    public AdminQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SessionSummaryDto> findSessions(Instant from, Instant to, String utmSource, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT event_id, created_at, last_seen_at, utm_source, utm_medium, utm_campaign,
                       utm_term, utm_content, gclid, fbclid, landing_path, user_agent, ip_hash
                FROM tracking_session
                WHERE 1=1
                """);

        List<Object> args = new ArrayList<>();
        appendTimeRange(sql, args, from, to, "created_at");

        if (utmSource != null && !utmSource.isBlank()) {
            sql.append(" AND utm_source = ?");
            args.add(utmSource);
        }

        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);

        return jdbcTemplate.query(sql.toString(), this::mapSession, args.toArray());
    }

    public SessionDetailDto findSessionDetail(UUID eventId) {
        List<SessionSummaryDto> sessions = jdbcTemplate.query("""
                SELECT event_id, created_at, last_seen_at, utm_source, utm_medium, utm_campaign,
                       utm_term, utm_content, gclid, fbclid, landing_path, user_agent, ip_hash
                FROM tracking_session
                WHERE event_id = ?
                """, this::mapSession, eventId);

        if (sessions.isEmpty()) {
            throw new ResourceNotFoundException("Session not found for eventId=" + eventId);
        }

        List<EventDto> events = jdbcTemplate.query("""
                SELECT id, event_id, event_type, created_at, currency, value, payload_json
                FROM tracking_event
                WHERE event_id = ?
                ORDER BY created_at DESC
                """, this::mapEvent, eventId);

        List<OrderDto> orders = jdbcTemplate.query("""
                SELECT id, event_id, stripe_session_id, payment_intent_id, amount, currency, status, created_at
                FROM orders
                WHERE event_id = ?
                ORDER BY created_at DESC
                """, this::mapOrder, eventId);

        return new SessionDetailDto(sessions.get(0), events, orders);
    }

    public List<EventDto> findEvents(String eventType, Instant from, Instant to, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, event_id, event_type, created_at, currency, value, payload_json
                FROM tracking_event
                WHERE 1=1
                """);

        List<Object> args = new ArrayList<>();
        appendTimeRange(sql, args, from, to, "created_at");

        if (eventType != null && !eventType.isBlank()) {
            sql.append(" AND event_type = ?");
            args.add(eventType);
        }

        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);

        return jdbcTemplate.query(sql.toString(), this::mapEvent, args.toArray());
    }

    public MetricsDto metrics(Instant from, Instant to) {
        StringBuilder sql = new StringBuilder("""
                SELECT event_type, COUNT(*) AS total
                FROM tracking_event
                WHERE 1=1
                """);

        List<Object> args = new ArrayList<>();
        appendTimeRange(sql, args, from, to, "created_at");
        sql.append(" GROUP BY event_type");

        Map<String, Long> counts = new HashMap<>();

        jdbcTemplate.query(sql.toString(), rs -> {
            counts.put(rs.getString("event_type"), rs.getLong("total"));
        }, args.toArray());

        long landingView = counts.getOrDefault("landing_view", 0L);
        long clickCta = counts.getOrDefault("click_cta", 0L);
        long beginCheckout = counts.getOrDefault("begin_checkout", 0L);
        long purchase = counts.getOrDefault("purchase", 0L);

        double conversionRate = landingView == 0 ? 0.0 : ((double) purchase / (double) landingView);

        return new MetricsDto(landingView, clickCta, beginCheckout, purchase, conversionRate);
    }

    private void appendTimeRange(StringBuilder sql, List<Object> args, Instant from, Instant to, String column) {
        if (from != null) {
            sql.append(" AND ").append(column).append(" >= ?");
            args.add(from);
        }
        if (to != null) {
            sql.append(" AND ").append(column).append(" <= ?");
            args.add(to);
        }
    }

    private SessionSummaryDto mapSession(ResultSet rs, int rowNum) throws SQLException {
        return new SessionSummaryDto(
                toUuid(rs.getString("event_id")),
                toInstant(rs, "created_at"),
                toInstant(rs, "last_seen_at"),
                rs.getString("utm_source"),
                rs.getString("utm_medium"),
                rs.getString("utm_campaign"),
                rs.getString("utm_term"),
                rs.getString("utm_content"),
                rs.getString("gclid"),
                rs.getString("fbclid"),
                rs.getString("landing_path"),
                rs.getString("user_agent"),
                rs.getString("ip_hash")
        );
    }

    private EventDto mapEvent(ResultSet rs, int rowNum) throws SQLException {
        return new EventDto(
                toUuid(rs.getString("id")),
                toUuid(rs.getString("event_id")),
                rs.getString("event_type"),
                toInstant(rs, "created_at"),
                rs.getString("currency"),
                rs.getBigDecimal("value"),
                rs.getString("payload_json")
        );
    }

    private OrderDto mapOrder(ResultSet rs, int rowNum) throws SQLException {
        return new OrderDto(
                toUuid(rs.getString("id")),
                toUuid(rs.getString("event_id")),
                rs.getString("stripe_session_id"),
                rs.getString("payment_intent_id"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getString("status"),
                toInstant(rs, "created_at")
        );
    }

    private UUID toUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    private Instant toInstant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }
}
