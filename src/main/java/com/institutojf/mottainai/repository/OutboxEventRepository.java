package com.institutojf.mottainai.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepository {

    private final JdbcTemplate jdbcTemplate;

    private final JsonMapper jsonMapper;

    public void lockIdempotencyKey(String idempotencyKey) {
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                (PreparedStatementCallback<Void>) statement -> {
                    statement.setString(1, idempotencyKey);
                    statement.execute();
                    return null;
                });
    }

    public Optional<Event> findByIdempotencyKey(String idempotencyKey) {
        return jdbcTemplate.query(
                "SELECT event_type, aggregate_type, aggregate_id, event_data::text "
                        + "FROM mottainai.fn_find_outbox_event_by_key(?)",
                (ResultSetExtractor<Optional<Event>>) resultSet -> {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional
                        .of(new Event(resultSet.getString("event_type"), resultSet.getString("aggregate_type"),
                                resultSet.getString("aggregate_id"), resultSet.getString("event_data")));
                }, idempotencyKey);
    }

    public boolean isPurchaseOrderFor(Event event, String requestHash) {
        if (!"PURCHASE_ORDER_CREATED".equals(event.eventType()) || !"purchase_order".equals(event.aggregateType())) {
            return false;
        }
        try {
            return requestHash.equals(jsonMapper.readTree(event.eventData()).path("request_hash").asText());
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Stored outbox event contains invalid JSON", exception);
        }
    }

    public void publishPurchaseOrder(String orderId, Integer companyId, Integer storeId, String requestHash,
            String idempotencyKey) {
        String eventData;
        try {
            eventData = jsonMapper
                .writeValueAsString(Map.of("purchase_order_id", orderId, "request_hash", requestHash));
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize purchase order event", exception);
        }
        jdbcTemplate.queryForObject(
                "SELECT mottainai.fn_publish_integration_event(?, ?, ?, CAST(? AS jsonb), ?, ?, ?, 5)", Object.class,
                "PURCHASE_ORDER_CREATED", "purchase_order", orderId, eventData, companyId, storeId, idempotencyKey);
    }

    public boolean isReceivingFor(Event event, Integer purchaseOrderId, String requestHash) {
        if (!"RECEIVING_CREATED".equals(event.eventType()) || !"receiving".equals(event.aggregateType())) {
            return false;
        }
        try {
            JsonNode data = jsonMapper.readTree(event.eventData());
            return data.path("purchase_order_id").asInt(-1) == purchaseOrderId
                    && requestHash.equals(data.path("request_hash").asText());
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Stored outbox event contains invalid JSON", exception);
        }
    }

    public void publishReceiving(String receivingId, Integer purchaseOrderId, Integer companyId, Integer storeId,
            String requestHash, String idempotencyKey) {
        String eventData;
        try {
            eventData = jsonMapper.writeValueAsString(Map.of("receiving_id", receivingId, "purchase_order_id",
                    purchaseOrderId, "request_hash", requestHash));
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize receiving event", exception);
        }
        jdbcTemplate.queryForObject(
                "SELECT mottainai.fn_publish_integration_event(?, ?, ?, CAST(? AS jsonb), ?, ?, ?, 5)", Object.class,
                "RECEIVING_CREATED", "receiving", receivingId, eventData, companyId, storeId, idempotencyKey);
    }

    public boolean isTransferFor(Event event, String requestHash) {
        if (!"TRANSFER_CREATED".equals(event.eventType()) || !"transfer".equals(event.aggregateType())) {
            return false;
        }
        try {
            return requestHash.equals(jsonMapper.readTree(event.eventData()).path("request_hash").asText());
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Stored outbox event contains invalid JSON", exception);
        }
    }

    public void publishTransfer(String transferId, Integer companyId, Integer storeId, String requestHash,
            String idempotencyKey) {
        String eventData;
        try {
            eventData = jsonMapper.writeValueAsString(Map.of("transfer_id", transferId, "request_hash", requestHash));
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize transfer event", exception);
        }
        jdbcTemplate.queryForObject(
                "SELECT mottainai.fn_publish_integration_event(?, ?, ?, CAST(? AS jsonb), ?, ?, ?, 5)", Object.class,
                "TRANSFER_CREATED", "transfer", transferId, eventData, companyId, storeId, idempotencyKey);
    }

    public boolean isEventFor(Event event, String eventType, String aggregateType) {
        return eventType.equals(event.eventType()) && aggregateType.equals(event.aggregateType());
    }

    public boolean isEventFor(Event event, String eventType, String aggregateType, String requestHash) {
        if (!isEventFor(event, eventType, aggregateType))
            return false;
        try {
            return requestHash.equals(jsonMapper.readTree(event.eventData()).path("request_hash").asText());
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Stored outbox event contains invalid JSON", exception);
        }
    }

    public Optional<SuggestedActionExecution> readSuggestedActionExecution(Event event, Integer suggestedActionId) {
        if (!"SUGGESTED_ACTION_EXECUTED".equals(event.eventType()) || !"suggested_action".equals(event.aggregateType())
                || !suggestedActionId.toString().equals(event.aggregateId())) {
            return Optional.empty();
        }
        try {
            JsonNode data = jsonMapper.readTree(event.eventData());
            if (data.path("suggested_action_id").asInt(Integer.MIN_VALUE) != suggestedActionId) {
                return Optional.empty();
            }
            String entityType = data.path("entity_type").asText(null);
            int entityId = data.path("entity_id").asInt(Integer.MIN_VALUE);
            if (entityType == null || entityId == Integer.MIN_VALUE) {
                throw new IllegalStateException("Stored suggested action execution event is incomplete");
            }
            return Optional.of(new SuggestedActionExecution(entityType, entityId));
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Stored outbox event contains invalid JSON", exception);
        }
    }

    public void publish(String eventType, String aggregateType, String aggregateId, Object data, Integer companyId,
            Integer storeId, String idempotencyKey) {
        String eventData;
        try {
            eventData = jsonMapper.writeValueAsString(data);
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize outbox event", exception);
        }
        jdbcTemplate.queryForObject(
                "SELECT mottainai.fn_publish_integration_event(?, ?, ?, CAST(? AS jsonb), ?, ?, ?, 5)", Object.class,
                eventType, aggregateType, aggregateId, eventData, companyId, storeId, idempotencyKey);
    }

    public record Event(
            String eventType,
            String aggregateType,
            String aggregateId,
            String eventData
    ) {
    }

    public record SuggestedActionExecution(
            String entityType,
            Integer entityId
    ) {
    }

}
