package com.yuntian.chat_app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    private static final String JWT_SECURITY_SCHEME = "JWT";

    @Bean
    public OpenAPI chatAppOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chat App API")
                        .description("Chat App backend API specification")
                        .version("v1.0.0")
                        .contact(new Contact().name("Yuntian"))
                        .license(new License().name("Apache 2.0")))
                .components(new Components()
                        .addSecuritySchemes(JWT_SECURITY_SCHEME, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SECURITY_SCHEME));
    }
}
