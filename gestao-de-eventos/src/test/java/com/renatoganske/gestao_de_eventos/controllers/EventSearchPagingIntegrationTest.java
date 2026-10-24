package com.renatoganske.gestao_de_eventos.controllers;

import com.jayway.jsonpath.JsonPath;
import com.renatoganske.gestao_de_eventos.configs.security.AppUserDetailsService;
import com.renatoganske.gestao_de_eventos.configs.security.JwtService;
import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateHdDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.EventCustomerAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueAssignmentDto;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.services.CustomerService;
import com.renatoganske.gestao_de_eventos.services.EventService;
import com.renatoganske.gestao_de_eventos.services.EventTypeService;
import com.renatoganske.gestao_de_eventos.services.EventVenueService;
import com.renatoganske.gestao_de_eventos.services.HdService;
import com.renatoganske.gestao_de_eventos.services.ProfessionalService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paging, server-side sorting and the free-text {@code q} of GET /api/events/search (ADR-0029),
 * through the real HTTP layer and database. The local database also holds the imported events, so
 * every request narrows the search to this test's fixtures with {@code q=<tag>}: the tag is in all
 * their codes and in no real data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class EventSearchPagingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EventService eventService;

    @Autowired
    private EventTypeService eventTypeService;

    @Autowired
    private EventVenueService eventVenueService;

    @Autowired
    private HdService hdService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ProfessionalService professionalService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    private final String tag = "pg" + UUID.randomUUID().toString().substring(0, 8);
    private String authHeader;

    @BeforeEach
    void setUp() {
        UserDetails userDetails = User.withUsername("renato").password("hash").authorities("USER").build();
        when(appUserDetailsService.loadUserByUsername("renato")).thenReturn(userDetails);
        authHeader = "Bearer " + jwtService.generateToken("renato");

        UUID wedding = eventTypeService.createEventType(new CreateEventTypeDto(tag + " Casamento", true)).id();
        UUID birthday = eventTypeService.createEventType(new CreateEventTypeDto(tag + " Aniversario", false)).id();
        UUID hdA = hdService.createHd(new CreateHdDto(tag + " HD A", 1000, null, 0, null, null, null, null)).id();
        UUID hdB = hdService.createHd(new CreateHdDto(tag + " HD B", 1000, null, 0, null, null, null, null)).id();
        UUID church = venue("Capela");
        UUID hall = venue("Salao");
        UUID beach = venue("Praia");
        UUID ana = customer("Ana");
        UUID bia = customer("Bia");
        UUID caio = customer("Caio");
        UUID photographer = professionalService.createProfessional(
                new CreateProfessionalDto(tag + " Foto", null, null, null, null)).id();

        // code     date        type        HD    status     venues          customers (contracting first)
        // -001     2026-03-10  Casamento   B     PENDING    Salao           Caio, Ana
        // -002     2026-01-05  Aniversario A     DELIVERED  Praia, Capela   Bia
        // -003     (none)      (none)      none  ARCHIVED   (none)          Ana (not contracting)
        // -004     2026-07-20  Casamento   A     PENDING    Capela          (none), 1 professional
        event("-001", wedding, LocalDate.of(2026, 3, 10), hdB, DeliveryStatus.PENDING,
                List.of(hall), List.of(new EventCustomerAssignmentDto(caio, true), new EventCustomerAssignmentDto(ana, false)),
                List.of());
        event("-002", birthday, LocalDate.of(2026, 1, 5), hdA, DeliveryStatus.DELIVERED,
                List.of(beach, church), List.of(new EventCustomerAssignmentDto(bia, true)), List.of());
        event("-003", null, null, null, DeliveryStatus.ARCHIVED,
                List.of(), List.of(new EventCustomerAssignmentDto(ana, false)), List.of());
        event("-004", wedding, LocalDate.of(2026, 7, 20), hdA, DeliveryStatus.PENDING,
                List.of(church), List.of(), List.of(new EventProfessionalAssignmentDto(photographer, "Fotógrafo")));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void defaultsToNewestFirstWithEventsWithoutDateLast() throws Exception {
        assertThat(codes(search())).containsExactly("-004", "-001", "-002", "-003");
    }

    @Test
    void sortsByEveryColumnOfTheList() throws Exception {
        assertThat(codes(search().param("sort", "code,desc"))).containsExactly("-004", "-003", "-002", "-001");
        assertThat(codes(search().param("sort", "date,asc"))).containsExactly("-002", "-001", "-004", "-003");
        // Aniversario < Casamento; ties by code; no type last
        assertThat(codes(search().param("sort", "type,asc"))).containsExactly("-002", "-001", "-004", "-003");
        assertThat(codes(search().param("sort", "hd,desc"))).containsExactly("-001", "-002", "-004", "-003");
        // ARCHIVED < DELIVERED < PENDING, the same order as Arquivado < Entregue < Pendente
        assertThat(codes(search().param("sort", "status,asc"))).containsExactly("-003", "-002", "-001", "-004");
    }

    @Test
    void sortsCustomersByTheContractingOneAndOtherwiseByTheFirstName() throws Exception {
        // -003 has only a non-contracting Ana; -001 is Caio's even though Ana is also there; -004 has nobody
        assertThat(codes(search().param("sort", "customer,asc"))).containsExactly("-003", "-002", "-001", "-004");
        assertThat(codes(search().param("sort", "customer,desc"))).containsExactly("-001", "-002", "-003", "-004");
    }

    @Test
    void sortsVenuesByTheFirstOneByName() throws Exception {
        // -002 has Praia and Capela: it sorts as Capela, tied with -004 and broken by code
        assertThat(codes(search().param("sort", "venues,asc"))).containsExactly("-002", "-004", "-001", "-003");
    }

    @Test
    void sortingByACollectionNeverRepeatsAnEvent() throws Exception {
        String body = body(search().param("sort", "customer,asc"));
        assertThat((Integer) JsonPath.read(body, "$.totalElements")).isEqualTo(4);
        assertThat(codes(body)).doesNotHaveDuplicates().hasSize(4);
    }

    @Test
    void pagesThroughTheResultWithTotals() throws Exception {
        String first = body(search().param("size", "3").param("sort", "code,asc"));
        String second = body(search().param("size", "3").param("page", "1").param("sort", "code,asc"));

        assertThat(codes(first)).containsExactly("-001", "-002", "-003");
        assertThat(codes(second)).containsExactly("-004");
        assertThat((Integer) JsonPath.read(first, "$.page")).isZero();
        assertThat((Integer) JsonPath.read(second, "$.page")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(first, "$.size")).isEqualTo(3);
        assertThat((Integer) JsonPath.read(first, "$.totalElements")).isEqualTo(4);
        assertThat((Integer) JsonPath.read(first, "$.totalPages")).isEqualTo(2);
    }

    @Test
    void returnsTheListRowShape() throws Exception {
        mockMvc.perform(search().param("sort", "code,asc").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventCode").value(tag + "-001"))
                .andExpect(jsonPath("$.content[0].type.name").value(tag + " Casamento"))
                .andExpect(jsonPath("$.content[0].hdName").value(tag + " HD B"))
                .andExpect(jsonPath("$.content[0].customers[0].customerName").value(tag + " Caio"))
                .andExpect(jsonPath("$.content[0].customers[0].contracting").value(true))
                .andExpect(jsonPath("$.content[0].venues[0].venueName").value(tag + " Salao"))
                .andExpect(jsonPath("$.content[0].professionalCount").value(0))
                // the heavy fields of the full EventDto stay out of the list
                .andExpect(jsonPath("$.content[0].eventProfessionals").doesNotExist())
                .andExpect(jsonPath("$.content[0].description").doesNotExist());
    }

    @Test
    void qMatchesACustomerNameAsWellAsTheCode() throws Exception {
        String body = body(get("/api/events/search").param("q", tag + " ANA").param("sort", "code,asc")
                .header("Authorization", authHeader));

        assertThat(codes(body)).containsExactly("-001", "-003");
    }

    @Test
    void qCombinesWithTheOtherFilters() throws Exception {
        assertThat(codes(search().param("deliveryStatus", "PENDING").param("sort", "code,asc")))
                .containsExactly("-001", "-004");
    }

    @Test
    void theNumberOfQueriesDoesNotGrowWithThePageSize() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        statistics.clear();
        body(search().param("size", "1"));
        long onePerPage = statistics.getPrepareStatementCount();

        statistics.clear();
        body(search().param("size", "4"));
        long fourPerPage = statistics.getPrepareStatementCount();

        assertThat(fourPerPage).isLessThanOrEqualTo(onePerPage);
    }

    @Test
    void rejectsAnUnknownSortOrAnOutOfRangePage() throws Exception {
        mockMvc.perform(search().param("sort", "eventDate,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid sort")));
        mockMvc.perform(search().param("size", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(search().param("size", "501")).andExpect(status().isBadRequest());
        mockMvc.perform(search().param("page", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(search().param("page", "x")).andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder search() {
        return get("/api/events/search").param("q", tag).header("Authorization", authHeader);
    }

    private String body(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private List<String> codes(MockHttpServletRequestBuilder request) throws Exception {
        return codes(body(request));
    }

    /** Codes without the tag, so the expectations read like the table in {@link #setUp()}. */
    private List<String> codes(String body) {
        List<String> codes = JsonPath.read(body, "$.content[*].eventCode");
        return codes.stream().map(code -> code.substring(tag.length())).toList();
    }

    private void event(String suffix, UUID typeId, LocalDate date, UUID hdId, DeliveryStatus status,
                       List<UUID> venueIds, List<EventCustomerAssignmentDto> customers,
                       List<EventProfessionalAssignmentDto> team) {
        eventService.createEvent(new CreateEventDto(
                tag + suffix, typeId, "Evento " + tag + suffix, date, null, null, null, null, null, null,
                status, hdId,
                venueIds.stream().map(venueId -> new EventVenueAssignmentDto(venueId, null)).toList(),
                customers, team));
    }

    private UUID venue(String name) {
        return eventVenueService.createEventVenue(new CreateEventVenueDto(tag + " " + name, null, null, null, null)).id();
    }

    private UUID customer(String name) {
        return customerService.createCustomer(new CreateCustomerDto(tag + " " + name, null, null, null)).id();
    }
}
