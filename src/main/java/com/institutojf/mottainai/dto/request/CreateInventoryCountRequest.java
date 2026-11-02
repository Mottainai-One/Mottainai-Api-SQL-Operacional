package com.institutojf.mottainai.dto.request;

import jakarta.validation.constraints.Size;

public record CreateInventoryCountRequest(
        @Size(max = 2000) String observation
) {
}
