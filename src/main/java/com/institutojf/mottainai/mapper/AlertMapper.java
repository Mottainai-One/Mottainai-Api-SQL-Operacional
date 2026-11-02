package com.institutojf.mottainai.mapper;

import com.institutojf.mottainai.dto.response.AlertResponse;
import com.institutojf.mottainai.model.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    public AlertResponse toResponse(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getStore().getId(), alert.getTitle(), alert.getDescription(),
                alert.getAlertType(), alert.getPriority(), alert.getStatus(), alert.getGeneratedAt(),
                alert.getResolvedAt(), alert.getCreatedAt());
    }

}
