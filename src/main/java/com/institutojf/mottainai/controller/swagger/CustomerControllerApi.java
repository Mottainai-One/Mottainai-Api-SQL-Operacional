package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateCustomerRequest;
import com.institutojf.mottainai.dto.request.UpdateCustomerRequest;
import com.institutojf.mottainai.dto.request.UpdateLoyaltyStatusRequest;
import com.institutojf.mottainai.dto.response.CustomerResponse;
import com.institutojf.mottainai.dto.response.LoyaltyAccountResponse;
import com.institutojf.mottainai.dto.response.LoyaltyTransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Customer", description = "API for Customer")
public interface CustomerControllerApi {

    @Operation(summary = "findAll")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<CustomerResponse> findAll();

    @Operation(summary = "create")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successful response")
    })
    CustomerResponse create(CreateCustomerRequest request);

    @Operation(summary = "find")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    CustomerResponse find(Integer id);

    @Operation(summary = "update")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    CustomerResponse update(Integer id, UpdateCustomerRequest request);

    @Operation(summary = "delete")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Successful response")
    })
    void delete(Integer id);

    @Operation(summary = "anonymize")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Successful response")
    })
    void anonymize(Integer id);

    @Operation(summary = "updateLoyaltyStatus")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    LoyaltyAccountResponse updateLoyaltyStatus(Integer id, UpdateLoyaltyStatusRequest request);

    @Operation(summary = "loyalty")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    LoyaltyAccountResponse loyalty(Integer id);

    @Operation(summary = "loyaltyTransactions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    List<LoyaltyTransactionResponse> loyaltyTransactions(Integer id, LocalDateTime from, LocalDateTime to);

}
