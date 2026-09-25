package com.renatoganske.gestao_de_eventos.configs.swagger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GDE-33: the generated OpenAPI spec must declare 204 as the documented response
 * for every DELETE endpoint, matching what the controllers actually return.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDeleteResponseDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deleteEndpoints_documentNoContentResponse() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/customers/{id}']['delete']['responses']['204']").exists())
                .andExpect(jsonPath("$['paths']['/api/events/{id}']['delete']['responses']['204']").exists())
                .andExpect(jsonPath("$['paths']['/api/event-types/{id}']['delete']['responses']['204']").exists())
                .andExpect(jsonPath("$['paths']['/api/event-venues/{id}']['delete']['responses']['204']").exists())
                .andExpect(jsonPath("$['paths']['/api/hds/{id}']['delete']['responses']['204']").exists())
                .andExpect(jsonPath("$['paths']['/api/professionals/{id}']['delete']['responses']['204']").exists());
    }
}
