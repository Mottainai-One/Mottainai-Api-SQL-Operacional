package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateEmployeeRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
        @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @NotNull @Positive Integer roleId,
        @NotNull @Positive Integer storeId,
        @NotNull LocalDate hireDate,
        Boolean createLogin
) {
    public CreateEmployeeRequest(String name, String cpf, String email, String phone, Integer roleId,
            Integer storeId, LocalDate hireDate) {
        this(name, cpf, email, phone, roleId, storeId, hireDate, true);
    }
}
