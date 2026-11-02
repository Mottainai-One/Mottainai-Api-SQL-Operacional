package com.institutojf.mottainai.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SuggestedActionType {
    PURCHASE_ORDER,
    PROMOTION,
    TRANSFER,
    DONATION,
    DISPOSAL,
    REORDER;

    @JsonCreator
    public static SuggestedActionType fromContract(String value) {
        return switch (value) {
            case "CREATE_PURCHASE_ORDER", "PURCHASE_ORDER" -> PURCHASE_ORDER;
            case "CREATE_PROMOTION", "PROMOTION" -> PROMOTION;
            case "CREATE_TRANSFER", "TRANSFER" -> TRANSFER;
            case "CREATE_DONATION", "DONATION" -> DONATION;
            case "CREATE_DISPOSAL", "DISPOSAL" -> DISPOSAL;
            case "CREATE_REPLENISHMENT", "REORDER" -> REORDER;
            default -> throw new IllegalArgumentException("Unsupported suggested action type: " + value);
        };
    }

    @JsonValue
    public String contractValue() {
        return switch (this) {
            case PURCHASE_ORDER -> "CREATE_PURCHASE_ORDER";
            case PROMOTION -> "CREATE_PROMOTION";
            case TRANSFER -> "CREATE_TRANSFER";
            case DONATION -> "CREATE_DONATION";
            case DISPOSAL -> "CREATE_DISPOSAL";
            case REORDER -> "CREATE_REPLENISHMENT";
        };
    }

}
