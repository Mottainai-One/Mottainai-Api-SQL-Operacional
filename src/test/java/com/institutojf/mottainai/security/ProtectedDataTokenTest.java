package com.institutojf.mottainai.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProtectedDataTokenTest {

    @Test
    void normalizesCpfBeforeGeneratingItsSha256Token() {
        String token = ProtectedDataToken.cpf("123.456.789-09");

        assertEquals(token, ProtectedDataToken.cpf("12345678909"));
        assertTrue(token.matches("CPF_SHA256_[A-F0-9]{64}"));
    }

    @Test
    void normalizesEmailBeforeGeneratingItsSha256Token() {
        assertEquals(ProtectedDataToken.email("ana@example.com"),
                ProtectedDataToken.email(" Ana@Example.com "));
    }

    @Test
    void keepsAnExistingTokenUnchangedAndDoesNotTokenizeBlankValues() {
        String token = ProtectedDataToken.phone("11999999999");

        assertEquals(token, ProtectedDataToken.phone(token));
        assertNull(ProtectedDataToken.email("   "));
    }
}
