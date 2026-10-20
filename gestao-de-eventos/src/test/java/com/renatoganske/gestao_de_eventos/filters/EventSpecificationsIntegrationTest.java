package com.renatoganske.gestao_de_eventos.filters;

import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateHdDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.EventCustomerAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueAssignmentDto;
import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.repositories.EventRepository;
import com.renatoganske.gestao_de_eventos.services.CustomerService;
import com.renatoganske.gestao_de_eventos.services.EventService;
import com.renatoganske.gestao_de_eventos.services.EventTypeService;
import com.renatoganske.gestao_de_eventos.services.EventVenueService;
import com.renatoganske.gestao_de_eventos.services.HdService;
import com.renatoganske.gestao_de_eventos.services.ProfessionalService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the event search criteria against the real Postgres instance (ADR-0027): the filters are SQL
 * now, so only the database can prove they keep the behaviour of the in-memory EventFilter they
 * replaced -- nulls, case, "any of the event's venues/customers", no duplicated rows.
 *
 * The local dev database also holds the imported events, so the assertions never expect an exact
 * result list: they check that each fixture is in or out of it. The fixtures' names and codes carry
 * a random tag so text searches cannot be satisfied by real data.
 */
@SpringBootTest
@Transactional
@Rollback
class EventSpecificationsIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

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

    private final String tag = "spec" + UUID.randomUUID().toString().substring(0, 8);

    private UUID weddingTypeId;
    private UUID birthdayTypeId;
    private UUID churchId;
    private UUID hallId;
    private UUID hdId;
    private UUID professionalId;

    /** Wedding on 2026-06-15: church + hall, two customers, one professional, daytime indoor, PENDING. */
    private UUID wedding;
    /** Birthday on 2026-01-01: another venue, HD and customer, no team, DELIVERED. */
    private UUID birthday;
    /** No date, type, HD, venue, customer or team; a night outdoor wedding by its flags. */
    private UUID bare;

    @BeforeEach
    void setUp() {
        weddingTypeId = eventTypeService.createEventType(new CreateEventTypeDto(tag + " WEDDING", true)).id();
        birthdayTypeId = eventTypeService.createEventType(new CreateEventTypeDto(tag + " BIRTHDAY", false)).id();
        churchId = venue("Church");
        hallId = venue("Hall");
        UUID otherVenueId = venue("Buffet");
        hdId = hd("HD 1");
        UUID otherHdId = hd("HD 2");
        professionalId = professionalService.createProfessional(
                new CreateProfessionalDto(tag + " Photographer", null, null, null, null)).id();

        wedding = eventService.createEvent(new CreateEventDto(
                tag + "-001", weddingTypeId, "Casamento " + tag, LocalDate.of(2026, 6, 15),
                true, false, null, null, null, null, DeliveryStatus.PENDING, hdId,
                List.of(new EventVenueAssignmentDto(churchId, "Cerimônia"), new EventVenueAssignmentDto(hallId, "Recepção")),
                List.of(new EventCustomerAssignmentDto(customer("Maria " + tag + "silva"), true),
                        new EventCustomerAssignmentDto(customer("Joao " + tag + "pereira"), false)),
                List.of(new EventProfessionalAssignmentDto(professionalId, "Fotógrafo")))).id();
        birthday = eventService.createEvent(new CreateEventDto(
                tag + "-002", birthdayTypeId, "Aniversario " + tag, LocalDate.of(2026, 1, 1),
                null, null, null, null, null, null, DeliveryStatus.DELIVERED, otherHdId,
                List.of(new EventVenueAssignmentDto(otherVenueId, null)),
                List.of(new EventCustomerAssignmentDto(customer("Ana " + tag + "souza"), true)),
                List.of())).id();
        bare = eventService.createEvent(new CreateEventDto(
                tag + "-003", null, "Sem dados " + tag, null,
                false, true, null, null, null, null, null, null, null, null, null)).id();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void byType_matchesOnlyGivenType() {
        assertThat(search(EventSpecifications.byType(weddingTypeId))).contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byVenue_matchesAnyOfTheEventsVenues() {
        assertThat(search(EventSpecifications.byVenue(churchId))).contains(wedding).doesNotContain(birthday, bare);
        // the second venue of the event matches too, not only the first one
        assertThat(search(EventSpecifications.byVenue(hallId))).contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byProfessional_matchesOnlyEventsWithGivenProfessional() {
        assertThat(search(EventSpecifications.byProfessional(professionalId)))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byPeriod_matchesWithinRange() {
        assertThat(search(EventSpecifications.byPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30))))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byPeriod_onlyFromBoundsLowerLimit() {
        assertThat(search(EventSpecifications.byPeriod(LocalDate.of(2026, 3, 1), null)))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byPeriod_onlyToBoundsUpperLimit() {
        assertThat(search(EventSpecifications.byPeriod(null, LocalDate.of(2026, 3, 1))))
                .contains(birthday).doesNotContain(wedding, bare);
    }

    @Test
    void byHd_matchesOnlyGivenHd() {
        assertThat(search(EventSpecifications.byHd(hdId))).contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byDeliveryStatus_matchesOnlyGivenStatus() {
        assertThat(search(EventSpecifications.byDeliveryStatus(DeliveryStatus.PENDING)))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byCustomerName_matchesPartialCaseInsensitive() {
        assertThat(search(EventSpecifications.byCustomerName(tag.toUpperCase() + "SILVA")))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byCustomerName_matchesAnyOfTheEventsCustomers() {
        assertThat(search(EventSpecifications.byCustomerName(tag + "pereira")))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byCustomerName_returnsAnEventOnceEvenWhenSeveralOfItsCustomersMatch() {
        List<UUID> result = search(EventSpecifications.byCustomerName(tag));

        assertThat(result).filteredOn(wedding::equals).hasSize(1);
        assertThat(result).contains(birthday).doesNotContain(bare);
    }

    @Test
    void byEventCode_matchesPartialCaseInsensitive() {
        assertThat(search(EventSpecifications.byEventCode(tag.toUpperCase() + "-001")))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void textFilters_matchLikeWildcardsLiterally() {
        // unescaped, "_" would match the "-" of the code and "%" any text in between
        assertThat(search(EventSpecifications.byEventCode(tag + "_001"))).doesNotContain(wedding);
        assertThat(search(EventSpecifications.byEventCode(tag + "%001"))).doesNotContain(wedding);
        assertThat(search(EventSpecifications.byCustomerName("maria%" + tag))).doesNotContain(wedding);
    }

    @Test
    void byOutdoorWedding_matchesOnlyExplicitValue() {
        assertThat(search(EventSpecifications.byOutdoorWedding(true))).contains(bare).doesNotContain(wedding, birthday);
        assertThat(search(EventSpecifications.byOutdoorWedding(false))).contains(wedding).doesNotContain(bare, birthday);
    }

    @Test
    void byDaytimeWedding_matchesOnlyExplicitValue() {
        assertThat(search(EventSpecifications.byDaytimeWedding(true))).contains(wedding).doesNotContain(bare, birthday);
        assertThat(search(EventSpecifications.byDaytimeWedding(false))).contains(bare).doesNotContain(wedding, birthday);
    }

    @Test
    void emptyCriteriaDoNotRestrict() {
        assertThat(EventSpecifications.byType(null)).isNull();
        assertThat(EventSpecifications.byVenue(null)).isNull();
        assertThat(EventSpecifications.byProfessional(null)).isNull();
        assertThat(EventSpecifications.byPeriod(null, null)).isNull();
        assertThat(EventSpecifications.byHd(null)).isNull();
        assertThat(EventSpecifications.byDeliveryStatus(null)).isNull();
        assertThat(EventSpecifications.byCustomerName(null)).isNull();
        assertThat(EventSpecifications.byCustomerName("  ")).isNull();
        assertThat(EventSpecifications.byEventCode(null)).isNull();
        assertThat(EventSpecifications.byEventCode("  ")).isNull();
        assertThat(EventSpecifications.byDaytimeWedding(null)).isNull();
        assertThat(EventSpecifications.byOutdoorWedding(null)).isNull();

        assertThat(search(Specification.allOf(
                EventSpecifications.byType(null), EventSpecifications.byCustomerName(null))))
                .contains(wedding, birthday, bare);
    }

    @Test
    void combinedCriteria_matchOnlyWhenAllMatch() {
        Specification<Event> spec = Specification.allOf(
                EventSpecifications.byType(weddingTypeId),
                EventSpecifications.byVenue(hallId),
                EventSpecifications.byCustomerName(tag + "silva"),
                EventSpecifications.byPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)));

        assertThat(search(spec)).contains(wedding).doesNotContain(birthday, bare);
        assertThat(search(spec.and(EventSpecifications.byDeliveryStatus(DeliveryStatus.DELIVERED))))
                .doesNotContain(wedding, birthday, bare);
    }

    private List<UUID> search(Specification<Event> spec) {
        return eventRepository.findAll(spec).stream().map(Event::getId).toList();
    }

    private UUID venue(String name) {
        return eventVenueService.createEventVenue(new CreateEventVenueDto(tag + " " + name, null, null, null, null)).id();
    }

    private UUID hd(String name) {
        return hdService.createHd(new CreateHdDto(tag + " " + name, 1000, null, 0, null, null, null, null)).id();
    }

    private UUID customer(String name) {
        return customerService.createCustomer(new CreateCustomerDto(name, null, null, null)).id();
    }
}
