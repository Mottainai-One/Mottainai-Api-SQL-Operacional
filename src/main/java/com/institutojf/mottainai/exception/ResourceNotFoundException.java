package com.institutojf.mottainai.exception;

/**
 * Usada quando o item procurado não existe
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

}
