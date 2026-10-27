package com.institutojf.mottainai.controller.swagger;

import com.institutojf.mottainai.dto.request.CreateProductCategoryRequest;
import com.institutojf.mottainai.dto.request.UpdateProductCategoryRequest;
import com.institutojf.mottainai.dto.response.ProductCategoryResponse;
import com.institutojf.mottainai.handler.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "Product Categories", description = "API for managing product categories")
public interface ProductCategoryControllerApi {

    @Operation(summary = "Create a product category")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product category created", content = @Content(schema = @Schema(implementation = ProductCategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Category name already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<ProductCategoryResponse> create(CreateProductCategoryRequest request);

    @Operation(summary = "Find all product categories")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product category list found", content = @Content(schema = @Schema(implementation = ProductCategoryResponse.class)))
    })
    ResponseEntity<Page<ProductCategoryResponse>> findAll(@ParameterObject Pageable pageable);

    @Operation(summary = "Find a product category by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product category found", content = @Content(schema = @Schema(implementation = ProductCategoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product category not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<ProductCategoryResponse> findById(Integer id);

    @Operation(summary = "Update a product category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product category updated", content = @Content(schema = @Schema(implementation = ProductCategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Product category not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Resource already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<ProductCategoryResponse> update(Integer id, UpdateProductCategoryRequest request);

    @Operation(summary = "Logically delete a product category")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product category logically deleted"),
            @ApiResponse(responseCode = "400", description = "Product category cannot be logically deleted", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Product category not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<Void> delete(Integer id);

}
