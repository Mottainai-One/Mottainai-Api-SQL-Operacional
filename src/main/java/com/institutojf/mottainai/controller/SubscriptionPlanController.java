package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.SubscriptionPlanControllerApi;
import com.institutojf.mottainai.dto.request.CreateSubscriptionPlanRequest;
import com.institutojf.mottainai.dto.request.UpdateSubscriptionPlanRequest;
import com.institutojf.mottainai.dto.response.SubscriptionPlanResponse;
import com.institutojf.mottainai.service.SubscriptionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@PreAuthorize("hasRole('ADMINISTRATOR')")
@RequestMapping("/api/v1/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController implements SubscriptionPlanControllerApi {

    private final SubscriptionPlanService subscriptionPlanService;

    @Override
    @PostMapping
    public ResponseEntity<SubscriptionPlanResponse> create(@Valid @RequestBody CreateSubscriptionPlanRequest request) {
        SubscriptionPlanResponse plan = subscriptionPlanService.create(request);
        URI location = URI.create("/api/v1/subscription-plans/" + plan.id());
        return ResponseEntity.created(location).body(plan);
    }

    @Override
    @GetMapping
    public ResponseEntity<Page<SubscriptionPlanResponse>> findAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(subscriptionPlanService.findAll(pageable));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlanResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(subscriptionPlanService.findById(id));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionPlanResponse> update(@PathVariable Integer id, @Valid @RequestBody UpdateSubscriptionPlanRequest request) {
        return ResponseEntity.ok(subscriptionPlanService.update(id, request));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        subscriptionPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
