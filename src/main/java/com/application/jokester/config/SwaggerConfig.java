package com.application.jokester.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configures Swagger UI which is available at http://localhost:8080/swagger-ui.html
// Swagger provides a visual interface where anyone can see all API endpoints,
// their parameters, request bodies, and response formats — and test them directly.
// The JWT security scheme allows testing protected endpoints from Swagger UI
// by entering a token in the Authorize button at the top right.
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Jokester API")
                        .description("REST API for sharing, voting on, and discovering jokes. Register to post jokes and vote. No login needed to read jokes.")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token here. Get it from /api/auth/login or /api/auth/register")));
    }
}