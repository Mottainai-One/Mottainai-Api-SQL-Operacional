package com.institutojf.mottainai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.staff")
public record StaffProperties(
        String passwordResetUrl
) {
}
