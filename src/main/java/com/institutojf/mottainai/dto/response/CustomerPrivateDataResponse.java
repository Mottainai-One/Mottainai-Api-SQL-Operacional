package com.institutojf.mottainai.dto.response;

import java.time.LocalDate;

public record CustomerPrivateDataResponse(String fullName, String cpf, String email, String phone,
        LocalDate birthDate, CustomerAddressData address) {
    public record CustomerAddressData(String zipCode, String street, String number, String complement,
            String neighborhood, String city, String state) { }
}
