package com.renatoganske.gestao_de_eventos.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renatoganske.gestao_de_eventos.configs.security.AppUserDetailsService;
import com.renatoganske.gestao_de_eventos.configs.security.JwtService;
import com.renatoganske.gestao_de_eventos.services.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end proof, through the real HTTP layer, that @Valid on the controllers actually
 * rejects a malformed request now. Before the fix, POST /api/customers without "name" would
 * pass @Valid untouched (zero bean-validation constraints on CreateCustomerDto) and only fail
 * downstream with a raw DataIntegrityViolationException from the "name not null" DB column
 * (an unhandled 500), never reaching DomainExceptionHandler's already-tested 400 path.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CreateCustomerValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    private String authHeader;

    @BeforeEach
    void setUpAuthenticatedUser() {
        UserDetails userDetails = User.withUsername("renato").password("hash").authorities("USER").build();
        when(appUserDetailsService.loadUserByUsername("renato")).thenReturn(userDetails);
        authHeader = "Bearer " + jwtService.generateToken("renato");
    }

    @Test
    void createCustomer_withoutName_returns400WithApiErrorBody_notA500() throws Exception {
        String bodyWithoutName = objectMapper.writeValueAsString(new NoNameCustomerRequest("(11) 99999-0000", null, null));

        mockMvc.perform(post("/api/customers")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithoutName))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verifyNoInteractions(customerService);
    }

    private record NoNameCustomerRequest(String contact, String address, String notes) {
    }
}
