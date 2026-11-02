package com.institutojf.mottainai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchStorePriceRequest(
        @NotEmpty List<@Valid BatchStorePriceItemRequest> prices
) {
}
