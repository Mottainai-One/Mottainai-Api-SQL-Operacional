package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.SuggestedActionDecisionControllerApi;
import com.institutojf.mottainai.dto.request.SuggestedActionDecisionRequest;
import com.institutojf.mottainai.dto.request.ExecuteSuggestedActionRequest;
import com.institutojf.mottainai.dto.response.ExecuteSuggestedActionResponse;
import com.institutojf.mottainai.dto.response.SuggestedActionResponse;
import com.institutojf.mottainai.service.SuggestedActionExecutionService;
import com.institutojf.mottainai.service.SuggestedActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suggested-actions")
@RequiredArgsConstructor
public class SuggestedActionDecisionController implements SuggestedActionDecisionControllerApi {

    private final SuggestedActionService suggestedActionService;

    private final SuggestedActionExecutionService suggestedActionExecutionService;

    @GetMapping
    public java.util.List<SuggestedActionResponse> findByStore(@RequestParam Integer storeId, Authentication authentication) {
        return suggestedActionService.getActionsByStore(storeId, authentication);
    }

    @Override
    @PostMapping("/{id}/decision")
    public SuggestedActionResponse decide(@PathVariable Integer id, @Valid @RequestBody SuggestedActionDecisionRequest request, Authentication authentication) {
        return suggestedActionService.decide(id, request.decision(), authentication);
    }

    @Override
    @PostMapping("/{id}/execute")
    public ExecuteSuggestedActionResponse execute(@PathVariable Integer id, @Valid @RequestBody ExecuteSuggestedActionRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey, Authentication authentication) {
        return suggestedActionExecutionService.execute(id, request, idempotencyKey, authentication);
    }

}
