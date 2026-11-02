package com.institutojf.mottainai.dto.response;

public record UserResponse(
        Integer id,
        String name,
        String cpf,
        String email,
        String phone,
        String role,
        Boolean active,
        Integer storeId
) {
}
