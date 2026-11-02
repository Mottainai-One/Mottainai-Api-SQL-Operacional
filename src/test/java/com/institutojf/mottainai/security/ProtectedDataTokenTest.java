package com.institutojf.mottainai.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

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

    @ParameterizedTest
    @MethodSource("installerTokens")
    void matchesInstallerHashAndPreservesExistingTokens(UnaryOperator<String> tokenize, String raw, String expected) {
        assertEquals(expected, tokenize.apply(raw));
        assertEquals(expected, tokenize.apply(expected));
        int hashStart = expected.indexOf("_SHA256_") + 8;
        assertEquals(expected, tokenize.apply(expected.substring(0, hashStart)
                + expected.substring(hashStart).toLowerCase(java.util.Locale.ROOT)));
    }

    static Stream<Arguments> installerTokens() {
        return Stream.of(
            Arguments.of((UnaryOperator<String>) ProtectedDataToken::cpf, "123.456.789-09",
                "CPF_SHA256_7EC94663084BD506D4F0C3E21042DF233681FD7426E93F397C921B1D3E397BBA"),
            Arguments.of((UnaryOperator<String>) ProtectedDataToken::email, " Ana@Example.com ",
                "EMAIL_SHA256_8E43CA37701228E74983EFDBD0CFF5C16B3B1E5D4E29A7C05626D4D25A018E11"),
            Arguments.of((UnaryOperator<String>) ProtectedDataToken::phone, "(11) 99999-9999",
                "PHONE_SHA256_F526027D759CE0D434924CD17D8AEC03126A35330C916CADCA1306C9DD289D79"),
            Arguments.of((UnaryOperator<String>) ProtectedDataToken::customerName, " Ana ",
                "CUS_SHA256_DEA210F058B407DB5C1B5EA89B2E42A57221C003DBA55E2F1776A75A3254D386"),
            Arguments.of((UnaryOperator<String>) ProtectedDataToken::externalAuthUid, " firebase-uid ",
                "AUTH_SHA256_17AC68F0521E0F87862AB687AC66091D7F5E7CC66CEDF0018E87C0896C4D2FE9")
        );
    }

    @Test
    void matchesInstallerAddressConcatenationIncludingEmptyPartsAndSpaces() {
        assertEquals("ADDR_SHA256_392B373C86B27CAC0B1BFBC81559E75BDB115CAF11C441EC10028654B2D8B9B1",
            ProtectedDataToken.address("01001000", " Rua A ", "1", "", "Centro", "São Paulo", "SP"));
        org.junit.jupiter.api.Assertions.assertNotEquals(
            ProtectedDataToken.address("", "Rua", "1", null, null, null, null),
            ProtectedDataToken.address(null, "Rua", "1", null, null, null, null));
    }
}
