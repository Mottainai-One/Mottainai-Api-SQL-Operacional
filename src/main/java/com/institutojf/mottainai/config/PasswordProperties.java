package com.institutojf.mottainai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.password")
public record PasswordProperties(
        String predictableTerms
) {
}
