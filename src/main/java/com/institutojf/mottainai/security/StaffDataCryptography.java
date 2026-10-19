package com.institutojf.mottainai.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Encrypts operational employee PII into the database's BYTEA AES-GCM columns. */
@Component
public final class StaffDataCryptography {

    private static final int NONCE_LENGTH_BYTES = 12;

    private static final int TAG_LENGTH_BITS = 128;

    private static final int MIN_CIPHERTEXT_LENGTH_BYTES = TAG_LENGTH_BITS / Byte.SIZE + 1;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey encryptionKey;

    private final short keyVersion;

    private final Map<Short, SecretKey> decryptionKeys;

    public StaffDataCryptography(String encryptionKeyBase64, int keyVersion) {
        this(encryptionKeyBase64, keyVersion, "");
    }

    @Autowired
    public StaffDataCryptography(@Value("${security.staff.data.encryption-key:}") String encryptionKeyBase64,
            @Value("${security.staff.data.key-version:1}") int keyVersion,
            @Value("${security.staff.data.decryption-keys:}") String historicalKeyConfiguration) {
        this.encryptionKey = decodeKey(encryptionKeyBase64);
        if (keyVersion < 1 || keyVersion > Short.MAX_VALUE) {
            throw new IllegalStateException("STAFF_DATA_KEY_VERSION must be between 1 and 32767");
        }
        this.keyVersion = (short) keyVersion;
        this.decryptionKeys = loadDecryptionKeys(this.keyVersion, this.encryptionKey, historicalKeyConfiguration);
    }

    public EncryptedValue encrypt(String field, String plaintext) {
        if (plaintext == null) {
            return null;
        }
        if (plaintext.isEmpty()) {
            throw new IllegalArgumentException("Staff data must not be empty when encrypted");
        }
        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(aad(field));
            return new EncryptedValue(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)), nonce);
        }
        catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt staff data", exception);
        }
    }

    public String decrypt(String field, short storedKeyVersion, byte[] ciphertext, byte[] nonce) {
        if (ciphertext == null && nonce == null) {
            return null;
        }
        if (ciphertext == null || nonce == null || nonce.length != NONCE_LENGTH_BYTES
                || ciphertext.length < MIN_CIPHERTEXT_LENGTH_BYTES) {
            throw new IllegalArgumentException("Staff data has an invalid AES-256-GCM envelope");
        }
        SecretKey decryptionKey = decryptionKeys.get(storedKeyVersion);
        if (decryptionKey == null) {
            throw new IllegalStateException("No staff data decryption key is configured for key_version "
                    + storedKeyVersion + "; configure STAFF_DATA_DECRYPTION_KEYS for historical versions");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, decryptionKey, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(aad(field));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Staff data could not be authenticated or decrypted", exception);
        }
    }

    public short keyVersion() {
        return keyVersion;
    }

    private static SecretKey decodeKey(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("STAFF_DATA_ENCRYPTION_KEY must be configured as a Base64 256-bit key");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(encoded);
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalStateException("STAFF_DATA_ENCRYPTION_KEY must be valid Base64", exception);
        }
        if (key.length != 32) {
            Arrays.fill(key, (byte) 0);
            throw new IllegalStateException("STAFF_DATA_ENCRYPTION_KEY must decode to exactly 32 bytes");
        }
        return new SecretKeySpec(key, "AES");
    }

    private static Map<Short, SecretKey> loadDecryptionKeys(short activeVersion, SecretKey activeKey,
            String historicalKeyConfiguration) {
        Map<Short, SecretKey> keys = new HashMap<>();
        keys.put(activeVersion, activeKey);
        if (historicalKeyConfiguration == null || historicalKeyConfiguration.isBlank()) {
            return Map.copyOf(keys);
        }
        for (String entry : historicalKeyConfiguration.split(",")) {
            String[] versionAndKey = entry.trim().split(":", 2);
            if (versionAndKey.length != 2 || versionAndKey[0].isBlank() || versionAndKey[1].isBlank()) {
                throw new IllegalStateException("STAFF_DATA_DECRYPTION_KEYS entries must use version:base64-key format");
            }
            short version;
            try {
                int parsedVersion = Integer.parseInt(versionAndKey[0].trim());
                if (parsedVersion < 1 || parsedVersion > Short.MAX_VALUE) {
                    throw new NumberFormatException("version out of range");
                }
                version = (short) parsedVersion;
            }
            catch (NumberFormatException exception) {
                throw new IllegalStateException("STAFF_DATA_DECRYPTION_KEYS versions must be positive SMALLINT values",
                        exception);
            }
            if (keys.putIfAbsent(version, decodeKey(versionAndKey[1].trim())) != null) {
                throw new IllegalStateException("Duplicate staff data key version configured: " + version);
            }
        }
        return Map.copyOf(keys);
    }

    private static byte[] aad(String field) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("Staff data field context is required");
        }
        return ("mottainai:operational:" + field).getBytes(StandardCharsets.UTF_8);
    }

    public record EncryptedValue(byte[] ciphertext, byte[] nonce) {
        public EncryptedValue {
            ciphertext = ciphertext == null ? null : ciphertext.clone();
            nonce = nonce == null ? null : nonce.clone();
        }

        @Override
        public byte[] ciphertext() {
            return ciphertext == null ? null : ciphertext.clone();
        }

        @Override
        public byte[] nonce() {
            return nonce == null ? null : nonce.clone();
        }
    }
}
