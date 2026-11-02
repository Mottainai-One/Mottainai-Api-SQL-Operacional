package com.institutojf.mottainai.exception;

public class PosApprovalStorageUnavailableException extends RuntimeException {
    public PosApprovalStorageUnavailableException(Throwable cause) {
        super("POS approval database contract is unavailable", cause);
    }
}
