package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.DisposalControllerApi;
import com.institutojf.mottainai.dto.request.CreateDisposalRequest;
import com.institutojf.mottainai.dto.response.DisposalResponse;
import com.institutojf.mottainai.service.DisposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/disposals")
@RequiredArgsConstructor
public class DisposalController implements DisposalControllerApi {

    private final DisposalService disposalService;

    @Override
    @GetMapping
    public List<DisposalResponse> findAll(@RequestParam(required = false) Integer storeId, @RequestParam(required = false) String reason, @RequestParam LocalDateTime from, @RequestParam LocalDateTime to, Authentication auth) {
        return disposalService.findAll(storeId, reason, from, to, auth);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DisposalResponse create(@Valid @RequestBody CreateDisposalRequest request, @RequestHeader("Idempotency-Key") String key, Authentication auth) {
        return disposalService.create(request, key, auth);
    }

    @Override
    @GetMapping("/{id}")
    public DisposalResponse findById(@PathVariable Integer id, Authentication auth) {
        return disposalService.findById(id, auth);
    }

}
