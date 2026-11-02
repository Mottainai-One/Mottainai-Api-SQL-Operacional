package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.CreateAddressRequest;
import com.institutojf.mottainai.dto.request.UpdateAddressRequest;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.mapper.AddressMapper;
import com.institutojf.mottainai.model.Address;
import com.institutojf.mottainai.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private BrasilApiService brasilApiService;

    @InjectMocks
    private AddressService addressService;

    @BeforeEach
    void stubCepLookup() {
        lenient().when(brasilApiService.completeAddress(any(CreateAddressRequest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(brasilApiService.completeAddress(any(UpdateAddressRequest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should reject duplicate address after normalizing whitespace")
    void shouldRejectDuplicateAddressAfterNormalizingWhitespace() {
        CreateAddressRequest request = createRequest("  Rua Irineu José Bordon  ", " 335 ", "   ");
        when(addressRepository.existsActiveAddress("05120060", "Rua Irineu José Bordon", "335", null, null))
            .thenReturn(true);
        assertThrows(ConflictException.class, () -> addressService.create(request));

        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should normalize address fields before saving")
    void shouldNormalizeAddressFieldsBeforeSaving() {
        CreateAddressRequest request = createRequest("  Rua Irineu José Bordon  ", " 335 ", "  Casa 2  ");
        when(addressRepository.existsActiveAddress("05120060", "Rua Irineu José Bordon", "335", "Casa 2", null))
            .thenReturn(false);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(addressMapper.toResponse(any(Address.class))).thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            return new com.institutojf.mottainai.dto.response.AddressResponse(1, address.getZipCode(), address.getStreet(), address.getNumber(), address.getComplement(), address.getNeighborhood(), address.getCity(), address.getState());
        });

        var response = addressService.create(request);

        assertEquals("Rua Irineu José Bordon", response.street());
        assertEquals("335", response.number());
        assertEquals("Casa 2", response.complement());
        assertEquals("Vila Jaguara", response.neighborhood());
        assertEquals("São Paulo", response.city());
    }

    @Test
    @DisplayName("Should reject duplicate address when updating")
    void shouldRejectDuplicateAddressWhenUpdating() {
        Address address = address(1);
        UpdateAddressRequest request = updateRequest("Rua Irineu José Bordon", "335", null);
        when(addressRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(address));
        when(addressRepository.existsActiveAddress("05120060", "Rua Irineu José Bordon", "335", null, 1))
            .thenReturn(true);
        assertThrows(ConflictException.class, () -> addressService.update(1, request));

        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should exclude the same address when validating update uniqueness")
    void shouldExcludeSameAddressWhenValidatingUpdateUniqueness() {
        Address address = address(1);
        UpdateAddressRequest request = updateRequest("Rua Irineu José Bordon", "335", null);
        when(addressRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(address));
        when(addressRepository.existsActiveAddress("05120060", "Rua Irineu José Bordon", "335", null, 1))
            .thenReturn(false);
        when(addressRepository.save(address)).thenReturn(address);

        addressService.update(1, request);

        verify(addressRepository).save(address);
    }

    private Address address(Integer id) {
        Address address = new Address();
        address.setId(id);
        return address;
    }

    private CreateAddressRequest createRequest(String street, String number, String complement) {
        return new CreateAddressRequest("05120060", street, number, complement, " Vila Jaguara ", " São Paulo ", "SP");
    }

    private UpdateAddressRequest updateRequest(String street, String number, String complement) {
        return new UpdateAddressRequest("05120060", street, number, complement, "Vila Jaguara", "São Paulo", "SP");
    }

}
