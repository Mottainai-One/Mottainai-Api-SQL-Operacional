package com.institutojf.mottainai.security;

import com.institutojf.mottainai.config.CustomerDataProperties;
import com.institutojf.mottainai.config.StaffDataProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** Uses an independent customer-data key; the serialized payload is authenticated as one envelope. */
@Component
public final class CustomerDataCryptography {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKey key;
    private final short keyVersion;
    private final Map<Short, SecretKey> decryptionKeys;

    public CustomerDataCryptography(CustomerDataProperties properties, StaffDataProperties staffProperties) {
        byte[] decoded = Base64.getDecoder().decode(properties.encryptionKey());
        byte[] staffKey = Base64.getDecoder().decode(staffProperties.encryptionKey());
        if (decoded.length != 32 || properties.keyVersion() < 1 || properties.keyVersion() > Short.MAX_VALUE) {
            throw new IllegalStateException("Customer data encryption settings are invalid");
        }
        if (MessageDigest.isEqual(decoded, staffKey)) {
            throw new IllegalStateException("CUSTOMER_DATA_ENCRYPTION_KEY must differ from STAFF_DATA_ENCRYPTION_KEY");
        }
        key = new SecretKeySpec(decoded, "AES");
        keyVersion = (short) properties.keyVersion();
        Map<Short, SecretKey> keys = new HashMap<>();
        keys.put(keyVersion, key);
        if (properties.decryptionKeys() != null && !properties.decryptionKeys().isBlank()) {
            for (String item : properties.decryptionKeys().split(",")) {
                String[] pair = item.trim().split(":", 2);
                short version = Short.parseShort(pair[0].trim());
                byte[] oldKey = Base64.getDecoder().decode(pair[1].trim());
                if (MessageDigest.isEqual(oldKey, staffKey)) {
                    throw new IllegalStateException("Customer decryption keys must differ from staff encryption key");
                }
                keys.put(version, new SecretKeySpec(oldKey, "AES"));
            }
        }
        decryptionKeys = Map.copyOf(keys);
    }

    public Envelope encrypt(Integer customerId, String plaintext) {
        byte[] nonce = new byte[12];
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(aad(customerId));
            return new Envelope(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)), nonce, keyVersion);
        }
        catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt customer personal data", exception);
        }
    }

    public String decrypt(Integer customerId, Envelope envelope) {
        if (envelope == null || envelope.nonce() == null
                || envelope.nonce().length != 12 || envelope.ciphertext() == null || envelope.ciphertext().length < 17) {
            throw new IllegalStateException("Customer personal data envelope or key version is invalid");
        }
        SecretKey decryptKey = decryptionKeys.get(envelope.keyVersion());
        if (decryptKey == null) {
            throw new IllegalStateException("No customer data decryption key is configured for key_version "
                    + envelope.keyVersion());
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, decryptKey, new GCMParameterSpec(128, envelope.nonce()));
            cipher.updateAAD(aad(customerId));
            return new String(cipher.doFinal(envelope.ciphertext()), StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Customer personal data could not be authenticated or decrypted", exception);
        }
    }

    private static byte[] aad(Integer customerId) {
        if (customerId == null || customerId < 1) {
            throw new IllegalArgumentException("A persisted customer ID is required for encryption context");
        }
        return ("mottainai:operational:customer.personal_data:" + customerId).getBytes(StandardCharsets.UTF_8);
    }

    public record Envelope(byte[] ciphertext, byte[] nonce, short keyVersion) {
        public Envelope {
            ciphertext = ciphertext == null ? null : ciphertext.clone();
            nonce = nonce == null ? null : nonce.clone();
        }
        @Override public byte[] ciphertext() { return ciphertext == null ? null : ciphertext.clone(); }
        @Override public byte[] nonce() { return nonce == null ? null : nonce.clone(); }
    }
}
