package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.response.CepResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;

@Tag(name = "Cep", description = "API for Cep")
public interface CepControllerApi {

    @Operation(summary = "Look up a Brazilian postal code", description = "Accepts CEP with or without a hyphen, for example 01001000 or 01001-000.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    ResponseEntity<CepResponse> getCepByZipCode(@Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP must have 8 digits, optionally with a hyphen") String cep);

}
