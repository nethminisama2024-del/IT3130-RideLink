
package com.ridelink.farepayment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI farePaymentOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("RideLink Fare Payment Service API")
                .version("1.0")
                .description(
                    "Fare estimation, final fare calculation, "
                    + "simulated payments and receipts"
                ))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
            .addSecurityItem(
                new SecurityRequirement().addList("bearerAuth"));
    }
}
