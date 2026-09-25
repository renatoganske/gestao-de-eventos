package com.renatoganske.gestao_de_eventos.configs.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GDE-31: the "prod" profile must NOT carry the "!prod" Swagger exemption from
 * SecurityConfig -- unlike SecurityIntegrationTest (which runs on "dev" and proves
 * the opposite, that Swagger stays open there), this activates "prod" for real to
 * catch a regression where that exemption accidentally widens to cover it too.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "DATASOURCE_URL=jdbc:postgresql://localhost:5432/gestaodeeventosdb",
        "DATASOURCE_USERNAME=postgres",
        "DATASOURCE_PASSWORD=1234",
        "JWT_SECRET=prod-profile-test-secret-needs-32-bytes-min"
})
class SwaggerProdLockdownIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void swaggerUi_withoutToken_isBlockedInProd() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiDocs_withoutToken_isBlockedInProd() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isUnauthorized());
    }
}
