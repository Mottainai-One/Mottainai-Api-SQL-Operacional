package com.institutojf.mottainai.exception;

/**
 * CEP não existe na base dos Correios
 */
public class CepNotFoundException extends BusinessException {

    public CepNotFoundException(String cep) {
        super("CEP not found: " + cep);
    }

}
