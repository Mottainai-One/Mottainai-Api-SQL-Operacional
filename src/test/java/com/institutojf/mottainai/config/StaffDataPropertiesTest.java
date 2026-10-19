package com.institutojf.mottainai.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class StaffDataPropertiesTest {

    private static final String VALID_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private static final String VALID_LOOKUP_KEY = Base64.getEncoder().encodeToString(new byte[] {
            1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidCurrentKeysAndOptionalHistoricalKeys() {
        StaffDataProperties properties = new StaffDataProperties(VALID_KEY, 2, "1:" + VALID_KEY, VALID_LOOKUP_KEY);

        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void rejectsMissingOrMalformedCurrentKeysAndInvalidVersion() {
        StaffDataProperties properties = new StaffDataProperties("not-base64", 0, "", "");

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void rejectsReusingTheEncryptionKeyForCpfLookup() {
        StaffDataProperties properties = new StaffDataProperties(VALID_KEY, 1, "", VALID_KEY);

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void rejectsMalformedHistoricalKeyConfiguration() {
        StaffDataProperties properties = new StaffDataProperties(VALID_KEY, 1, "1:bad-key", VALID_LOOKUP_KEY);

        assertFalse(validator.validate(properties).isEmpty());
    }
}
