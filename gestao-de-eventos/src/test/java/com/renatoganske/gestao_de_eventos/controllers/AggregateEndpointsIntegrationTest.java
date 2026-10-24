package com.renatoganske.gestao_de_eventos.controllers;

import com.jayway.jsonpath.JsonPath;
import com.renatoganske.gestao_de_eventos.configs.security.AppUserDetailsService;
import com.renatoganske.gestao_de_eventos.configs.security.JwtService;
import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateHdDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.EventCustomerAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueAssignmentDto;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.services.CustomerService;
import com.renatoganske.gestao_de_eventos.services.EventService;
import com.renatoganske.gestao_de_eventos.services.EventVenueService;
import com.renatoganske.gestao_de_eventos.services.HdService;
import com.renatoganske.gestao_de_eventos.services.ProfessionalService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The aggregate endpoints of ADR-0029 -- dashboard summary, upcoming events, {@code /options} -- and
 * the reference lists without embedded events, through the real HTTP layer and database.
 *
 * The local database also holds the imported events, so the dashboard numbers are checked as the
 * difference this test's fixtures make, and "today" is fixed by a mocked clock far in the future
 * (2031-05-15 in Brazil), past every real event.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AggregateEndpointsIntegrationTest {

    private static final ZoneId BRAZIL = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2031, 5, 15);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EventService eventService;

    @Autowired
    private HdService hdService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private EventVenueService eventVenueService;

    @Autowired
    private ProfessionalService professionalService;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @MockBean
    private Clock clock;

    private final String tag = "ag" + UUID.randomUUID().toString().substring(0, 8);
    private String authHeader;

    @BeforeEach
    void setUp() {
        UserDetails userDetails = User.withUsername("renato").password("hash").authorities("USER").build();
        when(appUserDetailsService.loadUserByUsername("renato")).thenReturn(userDetails);
        authHeader = "Bearer " + jwtService.generateToken("renato");

        // 22:30 in Brazil is already the next day in UTC: "today" must still be the 15th
        ZonedDateTime lateEvening = TODAY.atTime(22, 30).atZone(BRAZIL);
        when(clock.instant()).thenReturn(lateEvening.toInstant());
        when(clock.getZone()).thenReturn(BRAZIL);
    }

    @Test
    void summary_countsTheMonthThePendingDeliveriesAndTheHdsNearCapacity() throws Exception {
        Map<String, Integer> before = summary();

        event("-1", TODAY.withDayOfMonth(1), DeliveryStatus.PENDING);           // this month, pending
        event("-2", TODAY.withDayOfMonth(31), DeliveryStatus.DELIVERED);        // this month (last day)
        event("-3", TODAY.minusMonths(1).withDayOfMonth(30), DeliveryStatus.PENDING); // last month, pending
        event("-4", TODAY.plusMonths(1).withDayOfMonth(1), DeliveryStatus.ARCHIVED);  // next month
        event("-5", null, DeliveryStatus.PENDING);                               // no date, pending
        hdService.createHd(new CreateHdDto(tag + " cheio", 1000, null, 950, null, null, null, null));
        hdService.createHd(new CreateHdDto(tag + " folgado", 1000, null, 100, null, null, null, null));
        entityManager.flush();

        Map<String, Integer> after = summary();

        assertThat(after.get("eventsThisMonth") - before.get("eventsThisMonth")).isEqualTo(2);
        assertThat(after.get("pendingDeliveries") - before.get("pendingDeliveries")).isEqualTo(3);
        assertThat(after.get("hdsNearCapacity") - before.get("hdsNearCapacity")).isEqualTo(1);
    }

    @Test
    void upcoming_bringsTheNextEventsFromTodaySoonestFirstInTheListShape() throws Exception {
        event("-past", TODAY.minusDays(1), DeliveryStatus.PENDING);
        event("-later", TODAY.plusDays(10), DeliveryStatus.PENDING);
        event("-today", TODAY, DeliveryStatus.DELIVERED);
        event("-undated", null, DeliveryStatus.PENDING);
        entityManager.flush();
        entityManager.clear();

        String body = mockMvc.perform(get("/api/events/upcoming").param("limit", "50").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventCode").exists())
                .andExpect(jsonPath("$[0].professionalCount").exists())
                .andExpect(jsonPath("$[0].eventProfessionals").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        List<String> codes = JsonPath.read(body, "$[*].eventCode");
        assertThat(codes).containsSubsequence(tag + "-today", tag + "-later")
                .doesNotContain(tag + "-past", tag + "-undated");
    }

    @Test
    void upcoming_defaultsToFiveAndRespectsTheLimit() throws Exception {
        for (int day = 0; day < 7; day++) {
            event("-d" + day, TODAY.plusDays(day), DeliveryStatus.PENDING);
        }
        entityManager.flush();

        mockMvc.perform(get("/api/events/upcoming").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].eventCode").value(tag + "-d0"));
        mockMvc.perform(get("/api/events/upcoming").param("limit", "2").header("Authorization", authHeader))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void upcoming_rejectsALimitOutOfRange() throws Exception {
        mockMvc.perform(get("/api/events/upcoming").param("limit", "0").header("Authorization", authHeader))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/events/upcoming").param("limit", "51").header("Authorization", authHeader))
                .andExpect(status().isBadRequest());
    }

    @Test
    void options_bringOnlyIdAndNameSortedByNameIgnoringCase() throws Exception {
        UUID hdB = hdService.createHd(new CreateHdDto(tag + " b", 1000, null, 0, null, null, null, null)).id();
        UUID hdA = hdService.createHd(new CreateHdDto(tag + " A", 1000, null, 0, null, null, null, null)).id();
        UUID customer = customerService.createCustomer(new CreateCustomerDto(tag + " Cliente", "contato", null, null)).id();
        UUID venue = eventVenueService.createEventVenue(new CreateEventVenueDto(tag + " Local", "Rua", null, null, null)).id();
        UUID professional = professionalService.createProfessional(
                new CreateProfessionalDto(tag + " Profissional", null, "contato", null, null)).id();
        entityManager.flush();

        List<String> hdIds = optionIds("/api/hds/options");
        assertThat(hdIds).containsSubsequence(hdA.toString(), hdB.toString());

        for (String path : List.of("/api/hds/options", "/api/customers/options",
                "/api/event-venues/options", "/api/professionals/options")) {
            String body = mockMvc.perform(get(path).header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<Map<String, Object>> options = JsonPath.read(body, "$");
            assertThat(options).allSatisfy(option -> assertThat(option).containsOnlyKeys("id", "name"));
        }
        assertThat(optionIds("/api/customers/options")).contains(customer.toString());
        assertThat(optionIds("/api/event-venues/options")).contains(venue.toString());
        assertThat(optionIds("/api/professionals/options")).contains(professional.toString());
    }

    @Test
    void referenceLists_reportHowManyEventsButNeverTheEventsThemselves() throws Exception {
        UUID hd = hdService.createHd(new CreateHdDto(tag + " HD", 1000, null, 0, null, null, null, null)).id();
        UUID customer = customerService.createCustomer(new CreateCustomerDto(tag + " Cliente", null, null, null)).id();
        UUID venue = eventVenueService.createEventVenue(new CreateEventVenueDto(tag + " Local", null, null, null, null)).id();
        UUID professional = professionalService.createProfessional(
                new CreateProfessionalDto(tag + " Profissional", null, null, null, null)).id();
        eventService.createEvent(new CreateEventDto(
                tag + "-x", null, "Evento " + tag, TODAY, null, null, null, null, null, null, null, hd,
                List.of(new EventVenueAssignmentDto(venue, null)),
                List.of(new EventCustomerAssignmentDto(customer, true)),
                List.of(new EventProfessionalAssignmentDto(professional, "Foto"))));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/hds/" + hd).header("Authorization", authHeader))
                .andExpect(jsonPath("$.eventCount").value(1)).andExpect(jsonPath("$.events").doesNotExist());
        mockMvc.perform(get("/api/customers/" + customer).header("Authorization", authHeader))
                .andExpect(jsonPath("$.eventCount").value(1)).andExpect(jsonPath("$.events").doesNotExist());
        mockMvc.perform(get("/api/event-venues/" + venue).header("Authorization", authHeader))
                .andExpect(jsonPath("$.eventCount").value(1)).andExpect(jsonPath("$.events").doesNotExist());
        mockMvc.perform(get("/api/professionals/" + professional).header("Authorization", authHeader))
                .andExpect(jsonPath("$.eventCount").value(1)).andExpect(jsonPath("$.eventProfessionals").doesNotExist());
        mockMvc.perform(get("/api/hds").header("Authorization", authHeader))
                .andExpect(jsonPath("$[0].events").doesNotExist());
    }

    private Map<String, Integer> summary() throws Exception {
        String body = mockMvc.perform(get("/api/dashboard/summary").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Map.of(
                "eventsThisMonth", JsonPath.read(body, "$.eventsThisMonth"),
                "pendingDeliveries", JsonPath.read(body, "$.pendingDeliveries"),
                "hdsNearCapacity", JsonPath.read(body, "$.hdsNearCapacity"));
    }

    private List<String> optionIds(String path) throws Exception {
        String body = mockMvc.perform(get(path).header("Authorization", authHeader))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].id");
    }

    private void event(String suffix, LocalDate date, DeliveryStatus status) {
        eventService.createEvent(new CreateEventDto(
                tag + suffix, null, "Evento " + tag + suffix, date, null, null, null, null, null, null,
                status, null, null, null, null));
    }
}
