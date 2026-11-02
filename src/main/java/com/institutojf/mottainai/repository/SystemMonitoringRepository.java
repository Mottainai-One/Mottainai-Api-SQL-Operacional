package com.institutojf.mottainai.repository;

import com.institutojf.mottainai.dto.response.SystemEventResponse;
import com.institutojf.mottainai.dto.response.SystemJobResponse;
import com.institutojf.mottainai.dto.response.SystemLogResponse;
import com.institutojf.mottainai.model.AppUser;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemMonitoringRepository extends Repository<AppUser, Integer> {

    @Query(value = "SELECT * FROM mottainai.vw_api_system_event WHERE company_id = :companyId AND occurred_at BETWEEN :from AND :to AND (:status IS NULL OR status = :status) ORDER BY occurred_at DESC, event_id DESC", nativeQuery = true)
    List<SystemEventProjection> queryEvents(@Param("companyId") Integer companyId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("status") String status);

    @Query(value = "SELECT * FROM mottainai.vw_api_system_event WHERE event_id = :id AND company_id = :companyId FOR UPDATE", nativeQuery = true)
    Optional<SystemEventProjection> queryEventForUpdate(@Param("id") Long id, @Param("companyId") Integer companyId);

    @Modifying
    @Query(value = "UPDATE mottainai.event_queue SET status = 'PENDING', retry_count = retry_count + 1, error_message = NULL, processed_at = NULL, published_at = NULL WHERE event_id = :id AND status = 'FAILED'", nativeQuery = true)
    int retryEvent(@Param("id") Long id);

    @Query(value = "SELECT * FROM mottainai.vw_api_system_log WHERE company_id = :companyId AND created_at BETWEEN :from AND :to AND (:level IS NULL OR level = :level) ORDER BY created_at DESC, id DESC", nativeQuery = true)
    List<SystemLogProjection> queryLogs(@Param("companyId") Integer companyId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("level") String level);

    @Query(value = "SELECT * FROM mottainai.vw_api_system_job WHERE company_id = :companyId AND start_time BETWEEN :from AND :to AND (:success IS NULL OR success = :success) ORDER BY start_time DESC, job_id DESC", nativeQuery = true)
    List<SystemJobProjection> queryJobs(@Param("companyId") Integer companyId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("success") Boolean success);

    default List<SystemEventResponse> findEvents(Integer companyId, LocalDateTime from, LocalDateTime to, String status) {
        return queryEvents(companyId, from, to, status).stream().map(SystemEventProjection::toResponse).toList();
    }

    default Optional<SystemEventResponse> findEventForUpdate(Long id, Integer companyId) {
        return queryEventForUpdate(id, companyId).map(SystemEventProjection::toResponse);
    }

    default boolean retry(Long id) {
        return retryEvent(id) == 1;
    }

    default List<SystemLogResponse> findLogs(Integer companyId, LocalDateTime from, LocalDateTime to, String level) {
        return queryLogs(companyId, from, to, level).stream().map(SystemLogProjection::toResponse).toList();
    }

    default List<SystemJobResponse> findJobs(Integer companyId, LocalDateTime from, LocalDateTime to, Boolean success) {
        return queryJobs(companyId, from, to, success).stream().map(SystemJobProjection::toResponse).toList();
    }

    interface SystemEventProjection {
        Long getEventId();
        UUID getEventUuid();
        String getEventType();
        String getAggregateType();
        String getAggregateId();
        String getEventData();
        Integer getPriority();
        String getStatus();
        Integer getRetryCount();
        String getErrorMessage();
        LocalDateTime getOccurredAt();
        LocalDateTime getProcessedAt();

        default SystemEventResponse toResponse() {
            return new SystemEventResponse(getEventId(), getEventUuid(), getEventType(), getAggregateType(), getAggregateId(), JsonSupport.parse(getEventData()), getPriority(), getStatus(), getRetryCount(), getErrorMessage(), getOccurredAt(), getProcessedAt());
        }
    }

    interface SystemLogProjection {
        Long getId();
        String getSource();
        String getLevel();
        String getModule();
        String getMessage();
        String getStackTrace();
        Integer getUserId();
        String getIpAddress();
        LocalDateTime getCreatedAt();

        default SystemLogResponse toResponse() {
            return new SystemLogResponse(getId(), getSource(), getLevel(), getModule(), getMessage(), getStackTrace(), getUserId(), getIpAddress(), getCreatedAt());
        }
    }

    interface SystemJobProjection {
        Long getJobId();
        String getJobName();
        String getJobType();
        LocalDateTime getStartTime();
        LocalDateTime getEndTime();
        Integer getDurationSeconds();
        Integer getRecordsProcessed();
        Boolean getSuccess();
        String getDetails();

        default SystemJobResponse toResponse() {
            return new SystemJobResponse(getJobId(), getJobName(), getJobType(), getStartTime(), getEndTime(), getDurationSeconds(), getRecordsProcessed(), getSuccess(), JsonSupport.parse(getDetails()));
        }
    }

    final class JsonSupport {
        private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

        private JsonSupport() {
        }

        static JsonNode parse(String value) {
            if (value == null) {
                return null;
            }
            try {
                return JSON_MAPPER.readTree(value);
            }
            catch (JacksonException exception) {
                throw new IllegalStateException("Database contains invalid JSON", exception);
            }
        }
    }
}
