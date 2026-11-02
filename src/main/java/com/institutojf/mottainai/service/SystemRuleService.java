package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.UpdateSystemRuleRequest;
import com.institutojf.mottainai.dto.response.SystemRuleResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.SystemRule;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.SystemRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SystemRuleService {

    private final SystemRuleRepository systemRuleRepository;

    private final AppUserRepository appUserRepository;

    private final AuditLogRepository auditLogRepository;

    private final JsonMapper jsonMapper;

    private final StaffUserLookup staffUserLookup;

    @Transactional(readOnly = true)
    public List<SystemRuleResponse> findAll() {
        return systemRuleRepository.findAllByOrderByCategoryAscKeyAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public SystemRuleResponse update(String key, String category, UpdateSystemRuleRequest request, String actorEmail) {
        List<SystemRule> matchingRules = systemRuleRepository.findAllByKey(key)
            .stream()
            .filter(rule -> category == null || rule.getCategory().equalsIgnoreCase(category))
            .toList();
        if (matchingRules.isEmpty()) {
            throw new ResourceNotFoundException("System rule not found");
        }
        if (matchingRules.size() > 1) {
            throw new ConflictException("Rule key exists in multiple categories; provide the category");
        }
        SystemRule rule = matchingRules.getFirst();
        validateValue(rule.getValueType(), request.ruleValue());
        AppUser actor = staffUserLookup.findActive(actorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        String oldValue = rule.getValue();
        rule.setValue(request.ruleValue());
        rule.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        rule = systemRuleRepository.save(rule);
        RuleValueAudit oldData = new RuleValueAudit(rule.getKey(), rule.getCategory(), rule.getValueType(), oldValue);
        RuleValueAudit newData = new RuleValueAudit(rule.getKey(), rule.getCategory(), rule.getValueType(),
                rule.getValue());
        auditLogRepository.record("system_rule", "UPDATE", rule.getId().toString(), actor.getId(), oldData, newData);
        return toResponse(rule);
    }

    private void validateValue(String valueType, String value) {
        try {
            switch (valueType.toUpperCase(Locale.ROOT)) {
                case "NUMBER" -> new BigDecimal(value);
                case "BOOLEAN" -> {
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new BusinessException("Boolean rule values must be true or false");
                    }
                }
                case "JSON" -> jsonMapper.readTree(value);
                case "TEXT" -> {
                }
                default -> throw new BusinessException("Unsupported system rule value type");
            }
        }
        catch (NumberFormatException | JacksonException exception) {
            throw new BusinessException("Rule value does not match its configured value type");
        }
    }

    private SystemRuleResponse toResponse(SystemRule rule) {
        return new SystemRuleResponse(rule.getId(), rule.getCategory(), rule.getKey(), rule.getName(), rule.getValue(),
                rule.getValueType(), rule.getDescription(), rule.getActive(), rule.getCreatedAt(), rule.getUpdatedAt());
    }

    private record RuleValueAudit(String key, String category, String valueType, String ruleValue) {
    }

}
