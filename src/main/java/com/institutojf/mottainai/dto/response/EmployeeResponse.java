package com.institutojf.mottainai.dto.response;

import java.time.LocalDateTime;

public record EmployeeResponse(
        Integer employeeId,
        Integer userId,
        String name,
        String cpf,
        String email,
        String phone,
        String role,
        Integer storeId,
        Boolean active,
        LocalDateTime lastLogin
) {
}
