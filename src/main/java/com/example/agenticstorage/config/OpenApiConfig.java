package com.example.agenticstorage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 *
 * Springdoc auto-generates the API description from the controllers; this
 * bean only supplies the top-level metadata (title, description, version)
 * shown at the top of the Swagger UI page.
 *
 * Once the app is running:
 *   - Swagger UI:  http://localhost:8080/swagger-ui.html
 *   - OpenAPI doc: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI agenticStorageOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Agentic Storage Demo API")
                        .description("MCP-style storage API demonstrating three safety layers: "
                                + "immutable versioning, sandboxing, and intent validation.")
                        .version("1.0.0")
                        .contact(new Contact().name("Agentic Storage Demo"))
                        .license(new License().name("Apache 2.0")));
    }
}
