package com.institutojf.mottainai.exception;

/**
 * Usada quando uma regra do sistema não permite a ação
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

}
