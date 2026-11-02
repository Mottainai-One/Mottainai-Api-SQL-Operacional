package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateDonationRequest;
import com.institutojf.mottainai.dto.request.UpdateDonationStatusRequest;
import com.institutojf.mottainai.dto.response.DonationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;

@Tag(name = "Donation", description = "API for Donation")
public interface DonationControllerApi {

    @Operation(summary = "findAll")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<DonationResponse> findAll(Integer storeId, LocalDateTime from, LocalDateTime to, Authentication auth);

    @Operation(summary = "create")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successful response")
    })
    DonationResponse create(CreateDonationRequest request, String key, Authentication auth);

    @Operation(summary = "findById")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    DonationResponse findById(Integer id, Authentication auth);

    @Operation(summary = "updateStatus")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    DonationResponse updateStatus(Integer id, UpdateDonationStatusRequest request, Authentication auth);

}
