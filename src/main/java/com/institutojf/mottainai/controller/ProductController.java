package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.ProductControllerApi;
import com.institutojf.mottainai.dto.request.CreateProductRequest;
import com.institutojf.mottainai.dto.request.UpdateProductRequest;
import com.institutojf.mottainai.dto.request.UpdateActiveStatusRequest;
import com.institutojf.mottainai.dto.request.BatchStorePriceRequest;
import com.institutojf.mottainai.dto.request.LinkSupplierToProductRequest;
import com.institutojf.mottainai.dto.request.UpdateStorePriceRequest;
import com.institutojf.mottainai.dto.request.UpdateSupplierProductRequest;
import com.institutojf.mottainai.dto.response.ProductResponse;
import com.institutojf.mottainai.dto.response.ProductHistoryResponse;
import com.institutojf.mottainai.dto.response.ProductPriceHistoryResponse;
import com.institutojf.mottainai.dto.response.StoreProductPriceResponse;
import com.institutojf.mottainai.dto.response.SupplierProductResponse;
import com.institutojf.mottainai.dto.response.SupplierResponse;
import com.institutojf.mottainai.service.ProductService;
import com.institutojf.mottainai.service.ProductCommercialService;
import com.institutojf.mottainai.service.SupplierProductService;
import com.institutojf.mottainai.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController implements ProductControllerApi {

    private final ProductService productService;

    private final ProductCommercialService productCommercialService;

    private final SupplierProductService supplierProductService;

    private final SupplierService supplierService;

    @Override
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request, Authentication authentication) {
        ProductResponse product = productService.create(request, authentication);
        URI location = URI.create("/api/v1/products/" + product.id());
        return ResponseEntity.created(location).body(product);
    }

    @Override
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> findAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(productService.findAll(pageable));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @Override
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ProductResponse> findByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(productService.findByBarcode(barcode));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Integer id, @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @Override
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<ProductResponse> updateStatus(@PathVariable Integer id, @Valid @RequestBody UpdateActiveStatusRequest request) {
        return ResponseEntity.ok(productService.updateStatus(id, request.active()));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication authentication) {
        productService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/{id}/store-prices")
    public ResponseEntity<List<StoreProductPriceResponse>> findStorePrices(@PathVariable Integer id) {
        return ResponseEntity.ok(productCommercialService.findStorePrices(id));
    }

    @Override
    @PutMapping("/{id}/store-prices/{storeId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<StoreProductPriceResponse> updateStorePrice(@PathVariable Integer id, @PathVariable Integer storeId, @Valid @RequestBody UpdateStorePriceRequest request, Authentication authentication) {
        return ResponseEntity
            .ok(productCommercialService.updateStorePrice(id, storeId, request.newPrice(), authentication));
    }

    @Override
    @PutMapping("/prices/batch")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<List<StoreProductPriceResponse>> updatePrices( @Valid @RequestBody BatchStorePriceRequest request, Authentication authentication) {
        return ResponseEntity.ok(productCommercialService.updateBatch(request, authentication));
    }

    @Override
    @GetMapping("/suppliers")
    public ResponseEntity<Page<SupplierResponse>> findSuppliers(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(supplierService.findAll(pageable));
    }

    @Override
    @GetMapping("/{id}/suppliers")
    public ResponseEntity<Page<SupplierProductResponse>> findSuppliersByProduct(@PathVariable Integer id, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(supplierProductService.findByProduct(id, pageable));
    }

    @Override
    @PostMapping("/{id}/suppliers")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<SupplierProductResponse> linkSupplier(@PathVariable Integer id, @Valid @RequestBody LinkSupplierToProductRequest request, Authentication authentication) {
        SupplierProductResponse link = supplierProductService.link(request.supplierId(), id, request.supplierCode(),
                request.purchasePrice(), request.leadTime(), authentication);
        return ResponseEntity.created(URI.create("/api/v1/products/" + id + "/suppliers/" + request.supplierId()))
            .body(link);
    }

    @Override
    @PutMapping("/{id}/suppliers/{supplierId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<SupplierProductResponse> updateSupplierLink(@PathVariable Integer id, @PathVariable Integer supplierId, @Valid @RequestBody UpdateSupplierProductRequest request, Authentication authentication) {
        return ResponseEntity.ok(supplierProductService.update(supplierId, id, request, authentication));
    }

    @Override
    @DeleteMapping("/{id}/suppliers/{supplierId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<Void> deactivateSupplierLink(@PathVariable Integer id, @PathVariable Integer supplierId, Authentication authentication) {
        supplierProductService.deactivate(supplierId, id, authentication);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/{id}/history/master-data")
    public ResponseEntity<List<ProductHistoryResponse>> findMasterHistory(@PathVariable Integer id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(productCommercialService.findMasterHistory(id, from, to));
    }

    @Override
    @GetMapping("/{id}/history/price")
    public ResponseEntity<List<ProductPriceHistoryResponse>> findPriceHistory(@PathVariable Integer id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(productCommercialService.findPriceHistory(id, from, to));
    }

}
