package com.institutojf.mottainai.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.staff.data")
public record StaffDataProperties(
        @NotBlank(message = "STAFF_DATA_ENCRYPTION_KEY must be configured")
        String encryptionKey,
        @Positive(message = "STAFF_DATA_KEY_VERSION must be a positive SMALLINT")
        @Max(value = Short.MAX_VALUE, message = "STAFF_DATA_KEY_VERSION must be a positive SMALLINT")
        int keyVersion,
        String decryptionKeys,
        @NotBlank(message = "STAFF_DATA_LOOKUP_KEY must be configured")
        String lookupKey
) {

    @AssertTrue(message = "STAFF_DATA_ENCRYPTION_KEY must be Base64 encoding of exactly 32 bytes")
    public boolean isEncryptionKeyValid() {
        return isValidKey(encryptionKey);
    }

    @AssertTrue(message = "STAFF_DATA_LOOKUP_KEY must be Base64 encoding of exactly 32 bytes")
    public boolean isLookupKeyValid() {
        return isValidKey(lookupKey);
    }

    @AssertTrue(message = "STAFF_DATA_LOOKUP_KEY must be different from STAFF_DATA_ENCRYPTION_KEY")
    public boolean isLookupKeyDistinct() {
        if (!isValidKey(encryptionKey) || !isValidKey(lookupKey)) {
            return true;
        }
        return !MessageDigest.isEqual(decodeKey(encryptionKey), decodeKey(lookupKey));
    }

    @AssertTrue(message = "STAFF_DATA_DECRYPTION_KEYS must contain unique version:base64-key entries")
    public boolean isDecryptionKeysValid() {
        if (decryptionKeys == null || decryptionKeys.isBlank()) {
            return true;
        }
        Set<Integer> versions = new HashSet<>();
        versions.add(keyVersion);
        for (String entry : decryptionKeys.split(",")) {
            String[] versionAndKey = entry.trim().split(":", 2);
            if (versionAndKey.length != 2 || versionAndKey[0].isBlank()
                    || !versionAndKey[0].trim().matches("\\d+")) {
                return false;
            }
            try {
                int version = Integer.parseInt(versionAndKey[0].trim());
                if (version < 1 || version > Short.MAX_VALUE || !versions.add(version)
                        || !isValidKey(versionAndKey[1].trim())) {
                    return false;
                }
            }
            catch (NumberFormatException exception) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidKey(String encoded) {
        byte[] decoded = decodeKey(encoded);
        return decoded != null && decoded.length == 32;
    }

    private static byte[] decodeKey(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(encoded.trim());
        }
        catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
