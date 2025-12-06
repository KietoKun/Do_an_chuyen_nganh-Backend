package com.pizzastore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server; // <--- Nhớ Import cái này
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 1. Thiết lập thông tin chung
                .info(new Info()
                        .title("Pizza Store API Documentation")
                        .version("1.0")
                        .description("Tài liệu API cho hệ thống cửa hàng Pizza")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))

                // 2. Cấu hình danh sách Server (QUAN TRỌNG: Cái bạn đang hỏi)
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Server Local"),
                        new Server().url("http://160.191.242.181:8080").description("Server VPS (Production)")
                ))

                // 3. Thiết lập bảo mật (Bearer Token)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}