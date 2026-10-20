package com.institutojf.mottainai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ PasswordProperties.class, StaffProperties.class, StaffDataProperties.class,
        CustomerDataProperties.class })
public class AppPropertiesConfig {

}
