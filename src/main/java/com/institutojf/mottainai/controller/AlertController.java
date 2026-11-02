package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.AlertControllerApi;
import com.institutojf.mottainai.dto.request.CreateAlertRequest;
import com.institutojf.mottainai.dto.request.UpdateAlertStatusRequest;
import com.institutojf.mottainai.dto.response.AlertResponse;
import com.institutojf.mottainai.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController implements AlertControllerApi {

    private final AlertService alertService;

    @Override
    @GetMapping
    public List<AlertResponse> getAlertsByStore(@RequestParam Integer storeId, Authentication authentication) {
        return alertService.getAlertsByStore(storeId, authentication);
    }

    @Override
    @GetMapping("/{id}")
    public AlertResponse getAlertById(@PathVariable Integer id, Authentication authentication) {
        return alertService.getAlertById(id, authentication);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertResponse createAlert(@Valid @RequestBody CreateAlertRequest request, Authentication authentication) {
        return alertService.createAlert(request, authentication);
    }

    @Override
    @PatchMapping("/{id}/status")
    public AlertResponse updateStatus(@PathVariable Integer id, @Valid @RequestBody UpdateAlertStatusRequest request, Authentication authentication) {
        return alertService.updateStatus(id, request.status(), authentication);
    }

}
