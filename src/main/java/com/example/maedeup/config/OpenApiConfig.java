package com.example.maedeup.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI maedeupOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("매듭(Maedeup) API")
                        .description("실시간 이벤트 플랫폼 API 문서")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Maedeup Team")
                                .email("taejin0416@koreatech.ac.kr"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .components(new Components()
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Basic Authentication")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
} 