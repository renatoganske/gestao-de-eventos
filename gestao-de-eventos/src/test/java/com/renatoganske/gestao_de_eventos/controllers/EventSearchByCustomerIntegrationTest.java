package com.renatoganske.gestao_de_eventos.controllers;

import com.renatoganske.gestao_de_eventos.configs.security.AppUserDetailsService;
import com.renatoganske.gestao_de_eventos.configs.security.JwtService;
import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventCustomerAssignmentDto;
import com.renatoganske.gestao_de_eventos.services.CustomerService;
import com.renatoganske.gestao_de_eventos.services.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The {@code customerId} parameter of GET /api/events/search, through the real HTTP layer and database. */
@SpringBootTest
@AutoConfigureMockMvc
class EventSearchByCustomerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private EventService eventService;

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
    void search_byCustomerId_returnsOnlyTheCustomersEventsOnce() throws Exception {
        String tag = "http" + UUID.randomUUID().toString().substring(0, 8);
        UUID contracting = customerService.createCustomer(new CreateCustomerDto("Maria " + tag, null, null, null)).id();
        UUID guest = customerService.createCustomer(new CreateCustomerDto("Joao " + tag, null, null, null)).id();
        UUID shared = event(tag + "-1", contracting, guest);
        UUID soloOfGuest = event(tag + "-2", guest);
        event(tag + "-3", contracting);

        mockMvc.perform(get("/api/events/search").param("customerId", guest.toString())
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.id == '" + shared + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id == '" + soloOfGuest + "')]").exists());
    }

    @Test
    void search_byCustomerIdWithoutEvents_returns200WithEmptyPage() throws Exception {
        mockMvc.perform(get("/api/events/search").param("customerId", UUID.randomUUID().toString())
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void search_byMalformedCustomerId_returns400() throws Exception {
        mockMvc.perform(get("/api/events/search").param("customerId", "not-a-uuid")
                        .header("Authorization", authHeader))
                .andExpect(status().isBadRequest());
    }

    /** The first customer listed is the contracting one. */
    private UUID event(String code, UUID... customerIds) {
        List<EventCustomerAssignmentDto> customers = new ArrayList<>();
        for (int i = 0; i < customerIds.length; i++) {
            customers.add(new EventCustomerAssignmentDto(customerIds[i], i == 0));
        }
        return eventService.createEvent(new CreateEventDto(
                code, null, "Evento " + code, null, null, null, null, null, null, null, null, null,
                null, customers, null)).id();
    }
}
