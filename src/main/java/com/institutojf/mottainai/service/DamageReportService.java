package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateDamageReportRequest;
import com.institutojf.mottainai.dto.response.DamageReportResponse;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.DamageReport;
import com.institutojf.mottainai.model.Product;
import com.institutojf.mottainai.repository.DamageReportRepository;
import com.institutojf.mottainai.repository.ProductRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class DamageReportService {
    private final DamageReportRepository damageReports;
    private final ProductRepository products;
    private final InventoryAccess inventoryAccess;

    @Transactional
    public DamageReportResponse create(CreateDamageReportRequest request, Authentication authentication) {
        AppUser actor = inventoryAccess.currentUser(authentication);
        Product product = products.findByIdAndDeletedAtIsNull(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (!product.getCompany().getId().equals(actor.getEmployee().getStore().getCompany().getId())) {
            throw new ResourceNotFoundException("Product not found");
        }
        DamageReport report = new DamageReport();
        report.setStore(actor.getEmployee().getStore());
        report.setEmployee(actor.getEmployee());
        report.setProduct(product);
        report.setReason(request.reason().trim());
        report.setQuantity(request.quantity());
        report.setNote(request.note());
        report.setPhotoUrl(request.photoUrl());
        return toResponse(damageReports.save(report));
    }

    @Transactional(readOnly = true)
    public List<DamageReportResponse> findAll(Authentication authentication) {
        AppUser actor = inventoryAccess.currentUser(authentication);
        return damageReports.findAllByStore_IdOrderByCreatedAtDesc(actor.getEmployee().getStore().getId())
                .stream().map(this::toResponse).toList();
    }

    private DamageReportResponse toResponse(DamageReport report) {
        return new DamageReportResponse(report.getId(), report.getStore().getId(), report.getEmployee().getId(),
                report.getProduct().getId(), report.getReason(), report.getQuantity(), report.getNote(),
                report.getPhotoUrl(), report.getCreatedAt());
    }
}
