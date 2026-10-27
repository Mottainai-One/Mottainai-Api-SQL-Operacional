package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.LoyaltyAccount;

public record LoyaltyAccountResponse(Integer pointsBalance) {
    public static LoyaltyAccountResponse fromEntity(LoyaltyAccount account) {
        return new LoyaltyAccountResponse(account.getPointsBalance());
    }
}