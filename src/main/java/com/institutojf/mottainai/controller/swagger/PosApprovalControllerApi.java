package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.DecidePosApprovalRequest;
import com.institutojf.mottainai.dto.response.PosApprovalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "POS approvals", description = "Store-scoped manager decisions for the operational POS")
public interface PosApprovalControllerApi {
    @Operation(summary = "List approvals from the authenticated manager's store")
    ResponseEntity<List<PosApprovalResponse>> list(String status, int limit, Authentication authentication);
    @Operation(summary = "Decide a pending approval, with cancellation and audit in one transaction")
    ResponseEntity<PosApprovalResponse> decide(UUID id, DecidePosApprovalRequest request, Authentication authentication);
}
