package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.*;
import com.institutojf.mottainai.dto.response.*;
import com.institutojf.mottainai.exception.*;
import com.institutojf.mottainai.model.*;
import com.institutojf.mottainai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    private final LoyaltyAccountRepository loyaltyAccountRepository;

    private final AddressRepository addressRepository;

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        ensureCustomerIsAvailable(request.cpf(), request.email());
        LocalDateTime now = LocalDateTime.now();
        Customer customer = createProfile(request.fullName(), request.cpf(), request.email(), request.phone(),
                request.birthDate(), request.marketingConsent(), request.address(), now);
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findByDeletedAtIsNullOrderByFullName()
            .stream()
            .map(CustomerResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse find(Integer id) {
        return CustomerResponse.fromEntity(customer(id));
    }

    @Transactional
    public CustomerResponse update(Integer id, UpdateCustomerRequest request) {
        Customer customer = customer(id);
        if (!customer.getEmail().equalsIgnoreCase(request.email()) && customerRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(request.email())) {
            throw new ConflictException("Email is already registered");
        }
        Map<String, Object> oldData = Map.of("email", customer.getEmail(), "marketing_consent", customer.getMarketingConsent());
        customer.setFullName(request.fullName().trim());
        customer.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        customer.setPhone(normalize(request.phone()));
        customer.setBirthDate(request.birthDate());
        customer.setMarketingConsent(request.marketingConsent());
        customer.setUpdatedAt(LocalDateTime.now());

        if (request.address() != null) {
            customer.setAddress(addressRepository.save(toAddress(request.address())));
        }

        customerRepository.save(customer);
        auditLogRepository.record("customer", "UPDATE", id.toString(), null, oldData,
                Map.of("email", customer.getEmail(), "marketing_consent", customer.getMarketingConsent()));
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional
    public void delete(Integer id) {
        Customer customer = customer(id);
        customer.setActive(false);
        customer.setDeletedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        loyaltyAccountRepository.findByCustomer_Id(id).ifPresent(account -> {
            account.setActive(false);
            loyaltyAccountRepository.save(account);
        });
        customerRepository.save(customer);
        auditLogRepository.record("customer", "DELETE", id.toString(), null, Map.of("active", true), Map.of("active", false));
    }

    @Transactional
    public void anonymize(Integer id) {
        Customer customer = customer(id);
        String marker = UUID.randomUUID().toString();
        customer.setFullName("ANONYMIZED-" + marker);
        customer.setCpf(null);
        customer.setEmail(null);
        customer.setPhone(null);
        customer.setBirthDate(null);
        customer.setAddress(null);
        customer.setMarketingConsent(false);
        customer.setActive(false);
        customer.setDeletedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        loyaltyAccountRepository.findByCustomer_Id(id).ifPresent(account -> {
            account.setActive(false);
            loyaltyAccountRepository.save(account);
        });
        customerRepository.save(customer);
        auditLogRepository.record("customer", "UPDATE", id.toString(), null, null, Map.of("anonymized", true));
    }

    @Transactional
    public LoyaltyAccountResponse updateLoyaltyStatus(Integer customerId, UpdateLoyaltyStatusRequest request) {
        customer(customerId);
        LoyaltyAccount account = loyaltyAccountRepository.findByCustomer_Id(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found"));
        account.setActive(request.active());
        account.setUpdatedAt(LocalDateTime.now());
        loyaltyAccountRepository.save(account);
        auditLogRepository.record("loyalty_account", "UPDATE", account.getId().toString(), null, null,
                Map.of("active", request.active()));
        return LoyaltyAccountResponse.fromEntity(account);
    }

    private Customer customer(Integer id) {
        return customerRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private Address toAddress(CreateAddressRequest request) {
        Address address = new Address();
        address.setZipCode(request.zipCode().trim());
        address.setStreet(request.street().trim());
        address.setNumber(request.number().trim());
        address.setComplement(normalize(request.complement()));
        address.setNeighborhood(request.neighborhood().trim());
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        return address;
    }

    private void ensureCustomerIsAvailable(String cpf, String email) {
        if (customerRepository.existsByCpfAndDeletedAtIsNull(cpf)) {
            throw new ConflictException("CPF is already registered");
        }
        if (customerRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            throw new ConflictException("Email is already registered");
        }
    }

    private Customer createProfile(String fullName, String cpf, String email, String phone, LocalDate birthDate,
            Boolean marketingConsent, CreateAddressRequest addressRequest, LocalDateTime now) {
        Customer customer = new Customer();
        customer.setFullName(fullName.trim());
        customer.setCpf(cpf);
        customer.setEmail(email.trim().toLowerCase(Locale.ROOT));
        customer.setPhone(normalize(phone));
        customer.setBirthDate(birthDate);
        customer.setMarketingConsent(Boolean.TRUE.equals(marketingConsent));
        customer.setActive(true);
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        if (addressRequest != null) {
            customer.setAddress(addressRepository.save(toAddress(addressRequest)));
        }
        customerRepository.save(customer);

        LoyaltyAccount account = new LoyaltyAccount();
        account.setCustomer(customer);
        account.setPointsBalance(0);
        account.setJoinedAt(now);
        account.setActive(true);
        account.setUpdatedAt(now);
        loyaltyAccountRepository.save(account);
        auditLogRepository.record("customer", "INSERT", customer.getId().toString(), null, null,
                Map.of("active", true));
        return customer;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
