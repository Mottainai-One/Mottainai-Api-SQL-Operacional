package com.institutojf.mottainai.dto.response;

public record EmployeeRoleResponse(
        Integer id,
        String name,
        String description,
        Integer permissionLevel,
        Boolean active
) {
}
