package com.institutojf.mottainai.exception;

/** Raised when the external postal-code provider cannot complete a lookup. */
public class CepLookupUnavailableException extends BusinessException {

    public CepLookupUnavailableException(String message) {
        super(message);
    }
}
