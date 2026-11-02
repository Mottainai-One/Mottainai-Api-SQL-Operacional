package com.institutojf.mottainai.dto.response;

public record CustomerTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
