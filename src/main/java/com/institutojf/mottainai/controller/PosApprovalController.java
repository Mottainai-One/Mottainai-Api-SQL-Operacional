package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.PosApprovalControllerApi;
import com.institutojf.mottainai.dto.request.DecidePosApprovalRequest;
import com.institutojf.mottainai.dto.response.PosApprovalResponse;
import com.institutojf.mottainai.exception.PosApprovalStorageUnavailableException;
import com.institutojf.mottainai.service.PosApprovalService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos-approvals")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
@RequiredArgsConstructor
public class PosApprovalController implements PosApprovalControllerApi {
    private final PosApprovalService approvals;

    @Override @GetMapping
    public ResponseEntity<List<PosApprovalResponse>> list(@RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "50") int limit, Authentication authentication) {
        return ResponseEntity.ok(approvals.list(status, limit, authentication));
    }

    @Override @PostMapping("/{id}/decision")
    public ResponseEntity<PosApprovalResponse> decide(@PathVariable UUID id,
            @Valid @RequestBody DecidePosApprovalRequest request, Authentication authentication) {
        return ResponseEntity.ok(approvals.decide(id, request, authentication));
    }

    @ExceptionHandler(PosApprovalStorageUnavailableException.class)
    public ResponseEntity<Map<String, String>> unavailable(PosApprovalStorageUnavailableException error) {
        return ResponseEntity.status(503).body(Map.of("message", "POS approval database contract is unavailable"));
    }
}
