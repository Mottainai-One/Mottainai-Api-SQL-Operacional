package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateAddressRequest;
import com.institutojf.mottainai.dto.request.UpdateAddressRequest;
import com.institutojf.mottainai.dto.response.AddressResponse;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.mapper.AddressMapper;
import com.institutojf.mottainai.model.Address;
import com.institutojf.mottainai.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    private final AddressMapper addressMapper;

    private final BrasilApiService brasilApiService;

    @Transactional
    public AddressResponse create(CreateAddressRequest request) {
        return addressMapper.toResponse(createAddress(request));
    }

    public Address createAddress(CreateAddressRequest request) {
        request = brasilApiService.completeAddress(request);
        validateAddressUniqueness(
                request.zipCode(), request.street(), request.number(), request.complement(), null
        );

        Address address = new Address();
        applyCreateRequest(request, address);
        return addressRepository.save(address);
    }

    @Transactional(readOnly = true)
    public Page<AddressResponse> findAll(Pageable pageable) {
        return addressRepository.findAllByDeletedAtIsNull(pageable).map(addressMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AddressResponse findById(Integer id) {
        return addressMapper.toResponse(findAddressById(id));
    }

    @Transactional
    public AddressResponse update(Integer id, UpdateAddressRequest request) {
        return addressMapper.toResponse(updateAddress(id, request));
    }

    public Address updateAddress(Integer id, UpdateAddressRequest request) {
        Address address = findAddressById(id);
        request = brasilApiService.completeAddress(request);
        validateAddressUniqueness(request.zipCode(), request.street(), request.number(), request.complement(), id);
        applyUpdateRequest(request, address);
        return addressRepository.save(address);
    }

    private Address findAddressById(Integer id) {
        return addressRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }

    private void validateAddressUniqueness(String zipCode, String street, String number, String complement, Integer excludedId) {
        if (addressRepository.existsActiveAddress(zipCode.trim(), street.trim(), number.trim(), normalizeOptional(complement), excludedId)) {
            throw new ConflictException("Address already exists");
        }
    }

    private void applyCreateRequest(CreateAddressRequest request, Address address) {
        applyAddressFields(request.zipCode(), request.street(), request.number(), request.complement(),
                request.neighborhood(), request.city(), request.state(), address);
    }

    private void applyUpdateRequest(UpdateAddressRequest request, Address address) {
        applyAddressFields(request.zipCode(), request.street(), request.number(), request.complement(),
                request.neighborhood(), request.city(), request.state(), address);
    }

    private void applyAddressFields(String zipCode, String street, String number, String complement, String neighborhood, String city, String state, Address address) {
        address.setZipCode(zipCode.trim());
        address.setStreet(street.trim());
        address.setNumber(number.trim());
        address.setComplement(normalizeOptional(complement));
        address.setNeighborhood(neighborhood.trim());
        address.setCity(city.trim());
        address.setState(state.trim());
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
