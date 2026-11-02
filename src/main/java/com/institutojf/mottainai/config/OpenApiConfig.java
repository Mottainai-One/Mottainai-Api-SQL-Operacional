package com.institutojf.mottainai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura as informações da API no Swagger e o uso do token JWT
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mottainaiOpenApi() {
        return new OpenAPI()
            .info(new Info().title("Mottainai API")
                .version("v1")
                .description("REST API for predictive retail inventory management")
                .license(new License().name("Academic project")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components().addSecuritySchemes("bearerAuth",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

}
