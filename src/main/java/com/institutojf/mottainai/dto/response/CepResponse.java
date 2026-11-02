package com.institutojf.mottainai.dto.response;

public record CepResponse(
        String zipCode,
        String street,
        String neighborhood,
        String city,
        String state,
        String service
) {
}
