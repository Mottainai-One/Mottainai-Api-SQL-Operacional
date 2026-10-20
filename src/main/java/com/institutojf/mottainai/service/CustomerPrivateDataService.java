package com.institutojf.mottainai.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.institutojf.mottainai.dto.request.CreateAddressRequest;
import com.institutojf.mottainai.dto.response.CustomerPrivateDataResponse;
import com.institutojf.mottainai.model.Customer;
import com.institutojf.mottainai.model.CustomerPrivateData;
import com.institutojf.mottainai.repository.CustomerPrivateDataRepository;
import com.institutojf.mottainai.security.CustomerDataCryptography;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists customer PII as one authenticated AES-GCM payload in a separate DB sidecar. */
@Service
public class CustomerPrivateDataService {
    private final CustomerPrivateDataRepository repository;
    private final CustomerDataCryptography cryptography;
    private final JsonMapper objectMapper;

    public CustomerPrivateDataService(CustomerPrivateDataRepository repository,
            CustomerDataCryptography cryptography, JsonMapper objectMapper) {
        this.repository = repository;
        this.cryptography = cryptography;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void save(Customer customer, String fullName, String cpf, String email, String phone,
            LocalDate birthDate, CreateAddressRequest address) {
        var addressData = address == null ? null : new CustomerPrivateDataResponse.CustomerAddressData(
                address.zipCode(), address.street(), address.number(), address.complement(),
                address.neighborhood(), address.city(), address.state());
        savePayload(customer, new CustomerPrivateDataResponse(fullName, cpf, email, phone, birthDate, addressData));
    }

    @Transactional
    public void updateFirebaseProfile(Customer customer, String fullName, String phone) {
        CustomerPrivateDataResponse current = read(customer);
        savePayload(customer, new CustomerPrivateDataResponse(fullName, current.cpf(), current.email(), phone,
                current.birthDate(), current.address()));
    }

    @Transactional
    public void updateProfile(Customer customer, String fullName, String email, String phone,
            LocalDate birthDate, CreateAddressRequest address) {
        CustomerPrivateDataResponse current = read(customer);
        var addressData = address == null ? null : new CustomerPrivateDataResponse.CustomerAddressData(
                address.zipCode(), address.street(), address.number(), address.complement(),
                address.neighborhood(), address.city(), address.state());
        // The update request has no CPF field, so retain the encrypted current CPF unchanged.
        savePayload(customer, new CustomerPrivateDataResponse(fullName, current.cpf(), email, phone,
                birthDate, addressData));
    }

    @Transactional(readOnly = true)
    public CustomerPrivateDataResponse read(Customer customer) {
        CustomerPrivateData data = repository.findById(customer.getId())
                .orElseThrow(() -> new IllegalStateException("Customer encrypted personal data is missing"));
        return deserialize(cryptography.decrypt(customer.getId(), new CustomerDataCryptography.Envelope(data.getPayloadCiphertext(),
                data.getPayloadNonce(), data.getKeyVersion())));
    }

    @Transactional
    public void erase(Customer customer) {
        repository.deleteById(customer.getId());
    }

    private void savePayload(Customer customer, CustomerPrivateDataResponse payload) {
        CustomerDataCryptography.Envelope envelope = cryptography.encrypt(customer.getId(), serialize(payload));
        CustomerPrivateData data = repository.findById(customer.getId()).orElseGet(CustomerPrivateData::new);
        LocalDateTime now = LocalDateTime.now();
        if (data.getCreatedAt() == null) data.setCreatedAt(now);
        data.setCustomer(customer);
        data.setPayloadCiphertext(envelope.ciphertext());
        data.setPayloadNonce(envelope.nonce());
        data.setKeyVersion(envelope.keyVersion());
        data.setUpdatedAt(now);
        repository.save(data);
    }

    private String serialize(CustomerPrivateDataResponse payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize customer personal data", exception);
        }
    }

    private CustomerPrivateDataResponse deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, CustomerPrivateDataResponse.class);
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Could not read customer personal data payload", exception);
        }
    }
}
