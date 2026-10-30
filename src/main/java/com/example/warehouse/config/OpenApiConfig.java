package com.example.warehouse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI warehouseOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Warehouse API")
                .version("v1")
                .description("Items, variants, pricing, stock tracking and sales with out-of-stock protection."));
    }
}
