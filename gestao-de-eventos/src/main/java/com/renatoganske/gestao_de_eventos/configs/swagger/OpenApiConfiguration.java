package com.renatoganske.gestao_de_eventos.configs.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Slf4j
@Configuration
public class OpenApiConfiguration {
    @Value("${info.app.name}")
    private String title;
    @Value("${info.app.description}")
    private String description;
    @Value("${info.app.version}")
    private String appVersion;

    @Value("${server.port:8080}")
    private String serverPort;
    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerUiPath;

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title(title)
                .description(description)
                .version(appVersion))
            .components(new Components().addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSwaggerUiAddress() {
        log.info("Swagger UI disponivel em: http://localhost:{}{}", serverPort, swaggerUiPath);
    }
}
