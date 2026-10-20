package com.renatoganske.gestao_de_eventos.filters;

import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateHdDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.EventCustomerAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalSummaryDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueAssignmentDto;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
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
 * Every search goes through {@code EventService.searchEvents}, not through the specifications
 * directly, so it also proves that each search parameter feeds the right filter: with two Booleans
 * and several UUIDs of the same type, the compiler cannot catch a swapped argument.
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
        assertThat(search(criteria().eventTypeId(weddingTypeId))).contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byVenue_matchesAnyOfTheEventsVenues() {
        assertThat(search(criteria().venueId(churchId))).contains(wedding).doesNotContain(birthday, bare);
        // the second venue of the event matches too, not only the first one
        assertThat(search(criteria().venueId(hallId))).contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byProfessional_matchesOnlyEventsWithGivenProfessional() {
        assertThat(search(criteria().professionalId(professionalId)))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byPeriod_matchesWithinRange() {
        assertThat(search(criteria().from(LocalDate.of(2026, 6, 1)).to(LocalDate.of(2026, 6, 30))))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byPeriod_onlyFromBoundsLowerLimit() {
        assertThat(search(criteria().from(LocalDate.of(2026, 3, 1))))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byPeriod_onlyToBoundsUpperLimit() {
        assertThat(search(criteria().to(LocalDate.of(2026, 3, 1))))
                .contains(birthday).doesNotContain(wedding, bare);
    }

    @Test
    void byHd_matchesOnlyGivenHd() {
        assertThat(search(criteria().hdId(hdId))).contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byDeliveryStatus_matchesOnlyGivenStatus() {
        assertThat(search(criteria().deliveryStatus(DeliveryStatus.PENDING)))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byCustomerName_matchesPartialCaseInsensitive() {
        assertThat(search(criteria().customerName(tag.toUpperCase() + "SILVA")))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byCustomerName_matchesAnyOfTheEventsCustomers() {
        assertThat(search(criteria().customerName(tag + "pereira")))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void byCustomerName_returnsAnEventOnceEvenWhenSeveralOfItsCustomersMatch() {
        List<UUID> result = search(criteria().customerName(tag));

        assertThat(result).filteredOn(wedding::equals).hasSize(1);
        assertThat(result).contains(birthday).doesNotContain(bare);
    }

    @Test
    void byEventCode_matchesPartialCaseInsensitive() {
        assertThat(search(criteria().eventCode(tag.toUpperCase() + "-001")))
                .contains(wedding).doesNotContain(birthday, bare);
    }

    @Test
    void textFilters_matchLikeWildcardsLiterally() {
        // unescaped, "_" would match the "-" of the code and "%" any text in between
        assertThat(search(criteria().eventCode(tag + "_001"))).doesNotContain(wedding);
        assertThat(search(criteria().eventCode(tag + "%001"))).doesNotContain(wedding);
        assertThat(search(criteria().customerName("maria%" + tag))).doesNotContain(wedding);
    }

    @Test
    void byOutdoorWedding_matchesOnlyExplicitValue() {
        assertThat(search(criteria().outdoorWedding(true))).contains(bare).doesNotContain(wedding, birthday);
        assertThat(search(criteria().outdoorWedding(false))).contains(wedding).doesNotContain(bare, birthday);
    }

    @Test
    void byDaytimeWedding_matchesOnlyExplicitValue() {
        assertThat(search(criteria().daytimeWedding(true))).contains(wedding).doesNotContain(bare, birthday);
        assertThat(search(criteria().daytimeWedding(false))).contains(bare).doesNotContain(wedding, birthday);
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

        assertThat(search(criteria())).contains(wedding, birthday, bare);
    }

    @Test
    void combinedCriteria_matchOnlyWhenAllMatch() {
        Criteria combined = criteria()
                .eventTypeId(weddingTypeId)
                .venueId(hallId)
                .customerName(tag + "silva")
                .from(LocalDate.of(2026, 6, 1)).to(LocalDate.of(2026, 6, 30));

        assertThat(search(combined)).contains(wedding).doesNotContain(birthday, bare);
        assertThat(search(combined.deliveryStatus(DeliveryStatus.DELIVERED)))
                .doesNotContain(wedding, birthday, bare);
    }

    @Test
    void team_comesOrderedByNameWhateverTheOrderItWasAssigned() {
        UUID assistantId = professionalService.createProfessional(
                new CreateProfessionalDto(tag + " Assistant", null, null, null, null)).id();
        UUID event = eventService.createEvent(new CreateEventDto(
                tag + "-004", null, "Equipe " + tag, null, null, null, null, null, null, null, null, null,
                null, null,
                List.of(new EventProfessionalAssignmentDto(professionalId, "Fotógrafo"),
                        new EventProfessionalAssignmentDto(assistantId, "Assistente")))).id();
        entityManager.flush();
        entityManager.clear();

        assertThat(eventService.getEventById(event).eventProfessionals())
                .extracting(EventProfessionalSummaryDto::professionalName)
                .containsExactly(tag + " Assistant", tag + " Photographer");
    }

    private List<UUID> search(Criteria criteria) {
        return eventService.searchEvents(criteria.build()).stream().map(EventDto::id).toList();
    }

    private static Criteria criteria() {
        return new Criteria();
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

    /** Names each criterion at the call site, which the record's positional constructor does not. */
    private static final class Criteria {
        private UUID eventTypeId;
        private UUID venueId;
        private UUID professionalId;
        private LocalDate from;
        private LocalDate to;
        private UUID hdId;
        private DeliveryStatus deliveryStatus;
        private String customerName;
        private String eventCode;
        private Boolean daytimeWedding;
        private Boolean outdoorWedding;

        Criteria eventTypeId(UUID value) { this.eventTypeId = value; return this; }
        Criteria venueId(UUID value) { this.venueId = value; return this; }
        Criteria professionalId(UUID value) { this.professionalId = value; return this; }
        Criteria from(LocalDate value) { this.from = value; return this; }
        Criteria to(LocalDate value) { this.to = value; return this; }
        Criteria hdId(UUID value) { this.hdId = value; return this; }
        Criteria deliveryStatus(DeliveryStatus value) { this.deliveryStatus = value; return this; }
        Criteria customerName(String value) { this.customerName = value; return this; }
        Criteria eventCode(String value) { this.eventCode = value; return this; }
        Criteria daytimeWedding(Boolean value) { this.daytimeWedding = value; return this; }
        Criteria outdoorWedding(Boolean value) { this.outdoorWedding = value; return this; }

        EventSearchCriteria build() {
            return new EventSearchCriteria(eventTypeId, venueId, professionalId, from, to, hdId,
                    deliveryStatus, customerName, eventCode, daytimeWedding, outdoorWedding);
        }
    }
}
