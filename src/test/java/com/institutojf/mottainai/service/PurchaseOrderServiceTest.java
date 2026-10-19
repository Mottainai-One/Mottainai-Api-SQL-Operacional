package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreatePurchaseOrderItemRequest;
import com.institutojf.mottainai.dto.request.CreatePurchaseOrderRequest;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.Company;
import com.institutojf.mottainai.model.Employee;
import com.institutojf.mottainai.model.EmployeeRole;
import com.institutojf.mottainai.model.Product;
import com.institutojf.mottainai.model.RetailStore;
import com.institutojf.mottainai.model.Supplier;
import com.institutojf.mottainai.model.enums.PurchaseOrderStatus;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.OutboxEventRepository;
import com.institutojf.mottainai.repository.ProductRepository;
import com.institutojf.mottainai.repository.PurchaseOrderRepository;
import com.institutojf.mottainai.repository.SupplierRepository;
import com.institutojf.mottainai.security.InventoryAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryAccess inventoryAccess;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PurchaseOrderService service;

    @Test
    @DisplayName("Should use authenticated employee and store when creating")
    void shouldUseAuthenticatedEmployeeAndStoreWhenCreating() {
        AppUser actor = actor();
        var item = new CreatePurchaseOrderItemRequest(12, BigDecimal.TEN, new BigDecimal("8.50"));
        var request = new CreatePurchaseOrderRequest(8, LocalDate.of(2026, 10, 10), "Morning", List.of(item));
        var created = new PurchaseOrderRepository.PurchaseOrderData(5, 3, 8, 4, LocalDateTime.now(), request.expectedDeliveryDate(), PurchaseOrderStatus.PENDING, request.observation(),
                new BigDecimal("85.00"), 1, List.of());
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))).when(authentication).getAuthorities();
        when(inventoryAccess.currentUser(authentication)).thenReturn(actor);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))).when(authentication).getAuthorities();
        when(outboxEventRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(supplierRepository.findByIdAndActiveTrueAndDeletedAtIsNull(8)).thenReturn(Optional.of(new Supplier()));
        when(productRepository.findByIdAndActiveTrueAndDeletedAtIsNull(12)).thenReturn(Optional.of(new Product()));
        when(purchaseOrderRepository.insert(3, 8, 4, request.expectedDeliveryDate(), "Morning", request.items()))
            .thenReturn(created);
        assertEquals(5, service.create(request, "key-1", authentication).id());
        verify(purchaseOrderRepository).insert(3, 8, 4, request.expectedDeliveryDate(), "Morning", request.items());
        verify(outboxEventRepository).publishPurchaseOrder(eq("5"), eq(2), eq(3), any(), eq("key-1"));
    }

    private AppUser actor() {
        Company company = new Company();
        company.setId(2);
        RetailStore store = new RetailStore();
        store.setId(3);
        store.setCompany(company);
        EmployeeRole role = new EmployeeRole();
        role.setName("MANAGER");
        Employee employee = new Employee();
        employee.setId(4);
        employee.setStore(store);
        employee.setRole(role);
        AppUser user = new AppUser();
        user.setId(6);
        user.setEmployee(employee);
        return user;
    }

}
