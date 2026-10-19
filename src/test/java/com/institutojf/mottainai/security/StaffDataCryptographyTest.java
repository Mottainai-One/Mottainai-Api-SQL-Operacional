package com.institutojf.mottainai.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class StaffDataCryptographyTest {

    private static final String ENCRYPTION_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final StaffDataCryptography cryptography = new StaffDataCryptography(ENCRYPTION_KEY, 1);

    @Test
    void encryptsToBinaryCiphertextAndDecryptsWithRandomNonce() {
        var first = cryptography.encrypt("employee.cpf", "12345678909");
        var second = cryptography.encrypt("employee.cpf", "12345678909");

        assertEquals(12, first.nonce().length);
        assertEquals(12, second.nonce().length);
        assertNotEquals(java.util.Arrays.toString(first.nonce()), java.util.Arrays.toString(second.nonce()));
        assertEquals("12345678909", cryptography.decrypt("employee.cpf", cryptography.keyVersion(), first.ciphertext(), first.nonce()));
        assertEquals("12345678909", cryptography.decrypt("employee.cpf", cryptography.keyVersion(), second.ciphertext(), second.nonce()));
    }

    @Test
    void rejectsTamperingAndFieldSwapping() {
        var encrypted = cryptography.encrypt("employee.name", "private value");
        byte[] tampered = encrypted.ciphertext();
        tampered[0] ^= 1;

        assertThrows(IllegalArgumentException.class,
                () -> cryptography.decrypt("employee.name", cryptography.keyVersion(), tampered, encrypted.nonce()));
        assertThrows(IllegalArgumentException.class,
                () -> cryptography.decrypt("employee.email", cryptography.keyVersion(), encrypted.ciphertext(), encrypted.nonce()));
    }

    @Test
    void returnsNullOnlyWhenBothCiphertextAndNonceAreNull() {
        assertEquals(null, cryptography.decrypt("employee.email", cryptography.keyVersion(), null, null));
        assertThrows(IllegalArgumentException.class,
                () -> cryptography.decrypt("employee.email", cryptography.keyVersion(), new byte[18], null));
    }

    @Test
    void rejectsEmptyPlaintextBecauseTheDatabaseRequiresAtLeastOneByteBeforeTheGcmTag() {
        assertThrows(IllegalArgumentException.class, () -> cryptography.encrypt("employee.email", ""));
        var minimumEnvelope = cryptography.encrypt("employee.name", "a");
        assertEquals(17, minimumEnvelope.ciphertext().length);
        assertEquals("a", cryptography.decrypt("employee.name", cryptography.keyVersion(),
                minimumEnvelope.ciphertext(), minimumEnvelope.nonce()));
        assertThrows(IllegalArgumentException.class,
                () -> cryptography.decrypt("employee.name", cryptography.keyVersion(), new byte[16], minimumEnvelope.nonce()));
    }

    @Test
    void requiresAValidBase64KeyOfExactly32Bytes() {
        assertThrows(IllegalStateException.class, () -> new StaffDataCryptography("invalid", 1));
        assertThrows(IllegalStateException.class,
                () -> new StaffDataCryptography(Base64.getEncoder().encodeToString(new byte[16]), 1));
        assertThrows(IllegalStateException.class, () -> new StaffDataCryptography(ENCRYPTION_KEY, 0));
    }

    @Test
    void decryptsUsingPersistedKeyVersionAndFailsExplicitlyWhenHistoricalKeyIsMissing() {
        String historicalKey = Base64.getEncoder().encodeToString(new byte[32]);
        byte[] activeKeyBytes = new byte[32];
        activeKeyBytes[0] = 1;
        String activeKey = Base64.getEncoder().encodeToString(activeKeyBytes);
        StaffDataCryptography oldEncryptor = new StaffDataCryptography(historicalKey, 1);
        var oldEnvelope = oldEncryptor.encrypt("employee.email", "old@example.com");
        StaffDataCryptography rotated = new StaffDataCryptography(activeKey, 2, "1:" + historicalKey);

        assertEquals("old@example.com", rotated.decrypt("employee.email", (short) 1,
                oldEnvelope.ciphertext(), oldEnvelope.nonce()));
        assertThrows(IllegalStateException.class, () -> new StaffDataCryptography(activeKey, 2)
                .decrypt("employee.email", (short) 1, oldEnvelope.ciphertext(), oldEnvelope.nonce()));
        assertThrows(IllegalStateException.class,
                () -> new StaffDataCryptography(activeKey, 2, "bad-entry"));
    }
}
