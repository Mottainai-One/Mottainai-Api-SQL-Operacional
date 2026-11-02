package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.AlertSuggestedActionControllerApi;
import com.institutojf.mottainai.dto.request.CreateSuggestedActionRequest;
import com.institutojf.mottainai.dto.response.SuggestedActionResponse;
import com.institutojf.mottainai.service.SuggestedActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts/{alertId}/suggested-actions")
@RequiredArgsConstructor
public class AlertSuggestedActionController implements AlertSuggestedActionControllerApi {

    private final SuggestedActionService suggestedActionService;

    @Override
    @GetMapping
    public List<SuggestedActionResponse> findAll(@PathVariable Integer alertId, Authentication authentication) {
        return suggestedActionService.getActionsByAlert(alertId, authentication);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuggestedActionResponse create(@PathVariable Integer alertId, @Valid @RequestBody CreateSuggestedActionRequest request, Authentication authentication) {
        return suggestedActionService.createForAlert(alertId, request, authentication);
    }

}
