package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.response.CepResponse;
import com.institutojf.mottainai.dto.request.CreateAddressRequest;
import com.institutojf.mottainai.dto.request.UpdateAddressRequest;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.CepLookupUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Consulta CEP na BrasilAPI O resultado fica em cache por 10 minutos pra não ficar
 * chamando a API toda hora
 */
@Service
@RequiredArgsConstructor
public class BrasilApiService {

    private final BrasilApiClient brasilApiClient;

    /**
     * Completes missing address fields from BrasilAPI. User-supplied fields take
     * precedence, and number/complement are always kept as entered. A full manual
     * address remains usable if BrasilAPI is temporarily unavailable.
     */
    public CreateAddressRequest completeAddress(CreateAddressRequest request) {
        if (request == null) return null;
        return completeAddress(request.zipCode(), request.street(), request.number(), request.complement(),
                request.neighborhood(), request.city(), request.state());
    }

    public UpdateAddressRequest completeAddress(UpdateAddressRequest request) {
        if (request == null) return null;
        CreateAddressRequest resolved = completeAddress(request.zipCode(), request.street(), request.number(),
                request.complement(), request.neighborhood(), request.city(), request.state());
        return new UpdateAddressRequest(resolved.zipCode(), resolved.street(), resolved.number(), resolved.complement(),
                resolved.neighborhood(), resolved.city(), resolved.state());
    }

    private CreateAddressRequest completeAddress(String zipCode, String street, String number, String complement,
            String neighborhood, String city, String state) {
        CepResponse cepData;
        String normalizedZipCode = normalizeZipCode(zipCode);
        try {
            cepData = getCep(normalizedZipCode);
        }
        catch (CepLookupUnavailableException exception) {
            if (hasCompleteManualAddress(street, neighborhood, city, state)) {
                return normalizedAddress(normalizedZipCode, street, number, complement, neighborhood, city, state);
            }
            throw exception;
        }

        CreateAddressRequest resolved = normalizedAddress(normalizedZipCode,
                firstNonBlank(street, cepData.street()), number, complement,
                firstNonBlank(neighborhood, cepData.neighborhood()),
                firstNonBlank(city, cepData.city()), firstNonBlank(state, cepData.state()));
        if (!hasCompleteManualAddress(resolved.street(), resolved.neighborhood(), resolved.city(), resolved.state())) {
            throw new BusinessException("The CEP lookup did not return a complete address. Fill in the missing fields manually");
        }
        return resolved;
    }

    private CreateAddressRequest normalizedAddress(String zipCode, String street, String number, String complement,
            String neighborhood, String city, String state) {
        String normalizedState = trim(state);
        return new CreateAddressRequest(trim(zipCode), trim(street), trim(number), normalizeOptional(complement),
                trim(neighborhood), trim(city), normalizedState == null ? null
                        : normalizedState.toUpperCase(java.util.Locale.ROOT));
    }

    private boolean hasCompleteManualAddress(String street, String neighborhood, String city, String state) {
        return hasText(street) && hasText(neighborhood) && hasText(city) && hasText(state);
    }

    private String firstNonBlank(String preferred, String fallback) {
        return hasText(preferred) ? preferred : fallback;
    }

    private String normalizeOptional(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public CepResponse getCep(String cep) {
        return brasilApiClient.getCep(normalizeZipCode(cep));
    }

    private String normalizeZipCode(String zipCode) {
        String supplied = zipCode == null ? "" : zipCode.trim();
        if (!supplied.matches("\\d{8}|\\d{5}-\\d{3}")) {
            throw new BusinessException("CEP must have 8 digits, with an optional hyphen after the fifth digit");
        }
        return supplied.replace("-", "");
    }
}
