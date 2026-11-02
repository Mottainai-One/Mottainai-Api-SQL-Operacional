package com.institutojf.mottainai.mapper;

import com.institutojf.mottainai.dto.response.SubscriptionPlanResponse;
import com.institutojf.mottainai.model.SubscriptionPlan;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPlanMapper {

    public SubscriptionPlanResponse toResponse(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(plan.getId(), plan.getName(), plan.getDescription(), plan.getPrice(),
                plan.getStoreLimit(), plan.getUserLimit(), plan.getActive());
    }

}
