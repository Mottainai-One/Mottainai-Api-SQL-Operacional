package com.institutojf.mottainai.dto.response;

public record InviteStoreUserResponse(
        UserResponse user,
        boolean active,
        boolean passwordSetupRequired,
        boolean emailNotificationSent
) {
}
