package com.institutojf.mottainai.service;

import com.institutojf.mottainai.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffContactDataTest {

    @Test
    void acceptsValidCpfAndNormalizesItsFormattedRepresentation() {
        assertEquals("12345678909", StaffContactData.cpf("12345678909"));
        assertEquals("12345678909", StaffContactData.cpf("123.456.789-09"));
    }

    @Test
    void rejectsCpfWithInvalidCheckDigits() {
        assertFalse(StaffContactData.hasValidCpfChecksum("12345678901"));
        assertThrows(BusinessException.class, () -> StaffContactData.cpf("12345678901"));
    }

    @Test
    void rejectsCpfWithAllDigitsRepeated() {
        assertFalse(StaffContactData.hasValidCpfChecksum("11111111111"));
        assertThrows(BusinessException.class, () -> StaffContactData.cpf("11111111111"));
    }
}
