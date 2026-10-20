package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateHdDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.CustomerResponseDto;
import com.renatoganske.gestao_de_eventos.dtos.EventCustomerAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventCustomerSummaryDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalSummaryDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueSummaryDto;
import com.renatoganske.gestao_de_eventos.dtos.HdDto;
import com.renatoganske.gestao_de_eventos.dtos.ProfessionalDto;
import com.renatoganske.gestao_de_eventos.exceptions.CustomerNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.EventNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.EventProfessionalRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Runs against the real Postgres instance (not a mocked repository) to prove the fix for the
 * bug found in the develop-wide review: creating an Event with a customer used to throw
 * org.hibernate.TransientPropertyValueException at flush time, because CreateCustomerDto's
 * nested toEntity() always built a transient Customer and Event.customer has no cascade.
 * A mocked-repository test cannot catch this class of bug -- it never flushes to the database.
 */
@SpringBootTest
@Transactional
@Rollback
class EventServiceIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private HdService hdService;

    @Autowired
    private EventVenueService eventVenueService;

    @Autowired
    private ProfessionalService professionalService;

    @Autowired
    private EventProfessionalRepository eventProfessionalRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createEvent_withExistingCustomerHdAndEventVenueIds_persistsSuccessfully() {
        CustomerResponseDto customer = customerService.createCustomer(
                new CreateCustomerDto("Integration Test Customer", null, null, null));
        HdDto hd = hdService.createHd(
                new CreateHdDto("Integration Test HD", 1000, null, 0, null, null, null, null));
        EventVenueDto eventVenue = eventVenueService.createEventVenue(
                new CreateEventVenueDto("Integration Test Venue", null, null, null, null));

        CreateEventDto createEventDto = new CreateEventDto(
                "EVT-INTEGRATION-001", null, "Integration Test Event", null,
                null, null, null, null, null, 50, null,
                hd.id(), List.of(new EventVenueAssignmentDto(eventVenue.id(), "Recepção")),
                List.of(new EventCustomerAssignmentDto(customer.id(), true)), null);

        EventDto result = eventService.createEvent(createEventDto);
        entityManager.flush();

        assertThat(result.id()).isNotNull();
        assertThat(result.customers()).extracting(EventCustomerSummaryDto::customerId).containsExactly(customer.id());
        assertThat(result.hdId()).isEqualTo(hd.id());
        assertThat(result.venues())
                .extracting(EventVenueSummaryDto::venueId, EventVenueSummaryDto::role)
                .containsExactly(tuple(eventVenue.id(), "Recepção"));
        assertThat(hdService.getHdById(hd.id()).usedSpaceGb()).isEqualTo(50);
    }

    @Test
    void createEvent_withNonExistentCustomerId_throwsCustomerNotFoundExceptionAndPersistsNothing() {
        UUID missingCustomerId = UUID.randomUUID();
        CreateEventDto createEventDto = new CreateEventDto(
                "EVT-INTEGRATION-002", null, "Integration Test Event 2", null,
                null, null, null, null, null, null, null,
                null, null, List.of(new EventCustomerAssignmentDto(missingCustomerId, true)), null);

        assertThatThrownBy(() -> {
            eventService.createEvent(createEventDto);
            entityManager.flush();
        }).isInstanceOf(CustomerNotFoundException.class);
    }

    /**
     * Same reason as the customers test below: EventEventVenueId is a record built by hand, so only a
     * flush + re-read proves the composite key, the role and the venue side round-trip.
     */
    @Test
    void createEvent_withSeveralVenues_persistsThemAndBothSidesReadThemBack() {
        EventVenueDto church = eventVenueService.createEventVenue(
                new CreateEventVenueDto("Integration Church", null, null, null, null));
        EventVenueDto hall = eventVenueService.createEventVenue(
                new CreateEventVenueDto("Integration Hall", null, null, null, null));

        EventDto created = eventService.createEvent(new CreateEventDto(
                "EVT-INTEGRATION-020", null, "Integration Two Venues", null,
                null, null, null, null, null, null, null,
                null,
                List.of(new EventVenueAssignmentDto(hall.id(), "Recepção"),
                        new EventVenueAssignmentDto(church.id(), "Cerimônia")),
                null, null));
        entityManager.flush();
        entityManager.clear();

        EventDto reloaded = eventService.getEventById(created.id());
        // sorted by venue name
        assertThat(reloaded.venues())
                .extracting(EventVenueSummaryDto::venueId, EventVenueSummaryDto::role)
                .containsExactly(tuple(church.id(), "Cerimônia"), tuple(hall.id(), "Recepção"));
        assertThat(eventVenueService.getEventVenueById(church.id()).events())
                .extracting(summary -> summary.id())
                .containsExactly(created.id());
    }

    @Test
    void updateEvent_withEmptyVenues_removesTheAssociationRows() {
        EventVenueDto venue = eventVenueService.createEventVenue(
                new CreateEventVenueDto("Integration Venue To Remove", null, null, null, null));
        EventDto created = eventService.createEvent(new CreateEventDto(
                "EVT-INTEGRATION-021", null, "Integration Event 21", null,
                null, null, null, null, null, null, null,
                null, List.of(new EventVenueAssignmentDto(venue.id(), null)), null, null));
        entityManager.flush();

        eventService.updateEvent(created.id(), new CreateEventDto(
                "EVT-INTEGRATION-021", null, "Integration Event 21", null,
                null, null, null, null, null, null, null,
                null, List.of(), null, null));
        entityManager.flush();
        entityManager.clear();

        assertThat(eventService.getEventById(created.id()).venues()).isEmpty();
        assertThat(eventVenueService.getEventVenueById(venue.id()).events()).isEmpty();
    }

    @Test
    void deleteEvent_withVenues_removesTheAssociationRowsAndTheEvent() {
        EventVenueDto venue = eventVenueService.createEventVenue(
                new CreateEventVenueDto("Integration Venue Of Deleted Event", null, null, null, null));
        EventDto created = eventService.createEvent(new CreateEventDto(
                "EVT-INTEGRATION-022", null, "Integration Event 22", null,
                null, null, null, null, null, null, null,
                null, List.of(new EventVenueAssignmentDto(venue.id(), "Cerimônia")), null, null));
        entityManager.flush();

        eventService.deleteEvent(created.id());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> eventService.getEventById(created.id())).isInstanceOf(EventNotFoundException.class);
        assertThat(eventVenueService.getEventVenueById(venue.id()).events()).isEmpty();
    }

    /**
     * Same reason as the professionals test below: EventCustomerId is a record built by hand, so only
     * a flush + re-read proves the composite key, the contracting flag and the customer side round-trip.
     */
    @Test
    void createEvent_withSeveralCustomers_persistsThemAndBothSidesReadThemBack() {
        CustomerResponseDto bride = customerService.createCustomer(
                new CreateCustomerDto("Integration Bride", null, null, null));
        CustomerResponseDto groom = customerService.createCustomer(
                new CreateCustomerDto("Integration Groom", null, null, null));

        EventDto created = eventService.createEvent(new CreateEventDto(
                "EVT-INTEGRATION-010", null, "Integration Wedding", null,
                null, null, null, null, null, null, null,
                null, null,
                List.of(new EventCustomerAssignmentDto(groom.id(), false),
                        new EventCustomerAssignmentDto(bride.id(), true)),
                null));
        entityManager.flush();
        entityManager.clear();

        EventDto reloaded = eventService.getEventById(created.id());
        assertThat(reloaded.customers())
                .extracting(EventCustomerSummaryDto::customerId, EventCustomerSummaryDto::contracting)
                .containsExactly(tuple(bride.id(), true), tuple(groom.id(), false));
        assertThat(customerService.getCustomerById(groom.id()).events())
                .extracting(summary -> summary.id())
                .containsExactly(created.id());
    }

    @Test
    void updateEvent_withEmptyCustomers_removesTheAssociationRows() {
        CustomerResponseDto customer = customerService.createCustomer(
                new CreateCustomerDto("Integration Customer To Remove", null, null, null));
        EventDto created = eventService.createEvent(new CreateEventDto(
                "EVT-INTEGRATION-011", null, "Integration Event 11", null,
                null, null, null, null, null, null, null,
                null, null, List.of(new EventCustomerAssignmentDto(customer.id(), true)), null));
        entityManager.flush();

        eventService.updateEvent(created.id(), new CreateEventDto(
                "EVT-INTEGRATION-011", null, "Integration Event 11", null,
                null, null, null, null, null, null, null,
                null, null, List.of(), null));
        entityManager.flush();
        entityManager.clear();

        assertThat(eventService.getEventById(created.id()).customers()).isEmpty();
    }

    @Test
    void deleteEvent_withCustomers_removesTheAssociationRowsAndTheEvent() {
        CustomerResponseDto customer = customerService.createCustomer(
                new CreateCustomerDto("Integration Customer Of Deleted Event", null, null, null));
        EventDto created = eventService.createEvent(new CreateEventDto(
                "EVT-INTEGRATION-012", null, "Integration Event 12", null,
                null, null, null, null, null, null, null,
                null, null, List.of(new EventCustomerAssignmentDto(customer.id(), true)), null));
        entityManager.flush();

        eventService.deleteEvent(created.id());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> eventService.getEventById(created.id())).isInstanceOf(EventNotFoundException.class);
        assertThat(customerService.getCustomerById(customer.id()).events()).isEmpty();
    }

    /**
     * EventProfessionalId is a record, so @MapsId cannot derive the composite key -- Hibernate would
     * have to write into immutable fields. EventService builds the key by hand instead, and only a
     * test that actually flushes and re-reads proves that round-trips.
     */
    @Test
    void createEvent_withProfessionals_persistsTheCompositeKeyAndReadsItBack() {
        ProfessionalDto photographer = professionalService.createProfessional(
                new CreateProfessionalDto("Integration Photographer", null, null, null, null));
        ProfessionalDto assistant = professionalService.createProfessional(
                new CreateProfessionalDto("Integration Assistant", null, null, null, null));

        CreateEventDto createEventDto = new CreateEventDto(
                "EVT-INTEGRATION-003", null, "Integration Test Event 3", null,
                null, null, null, null, null, null, null,
                null, null, null,
                List.of(new EventProfessionalAssignmentDto(photographer.id(), "Fotografo principal"),
                        new EventProfessionalAssignmentDto(assistant.id(), "Segundo fotografo")));

        EventDto created = eventService.createEvent(createEventDto);
        entityManager.flush();
        entityManager.clear();

        EventDto reloaded = eventService.getEventById(created.id());
        assertThat(reloaded.eventProfessionals())
                .extracting(EventProfessionalSummaryDto::professionalId, EventProfessionalSummaryDto::roleInEvent)
                .containsExactlyInAnyOrder(
                        tuple(photographer.id(), "Fotografo principal"),
                        tuple(assistant.id(), "Segundo fotografo"));
    }

    @Test
    void updateEvent_withADifferentTeam_replacesTheAssociationRows() {
        ProfessionalDto photographer = professionalService.createProfessional(
                new CreateProfessionalDto("Integration Photographer 2", null, null, null, null));
        ProfessionalDto assistant = professionalService.createProfessional(
                new CreateProfessionalDto("Integration Assistant 2", null, null, null, null));

        EventDto created = eventService.createEvent(new CreateEventDto(
                "EVT-INTEGRATION-004", null, "Integration Test Event 4", null,
                null, null, null, null, null, null, null,
                null, null, null,
                List.of(new EventProfessionalAssignmentDto(photographer.id(), "Fotografo principal"))));
        entityManager.flush();

        EventDto updated = eventService.updateEvent(created.id(), new CreateEventDto(
                "EVT-INTEGRATION-004", null, "Integration Test Event 4", null,
                null, null, null, null, null, null, null,
                null, null, null,
                List.of(new EventProfessionalAssignmentDto(assistant.id(), "Fotografo principal"))));
        entityManager.flush();
        entityManager.clear();

        assertThat(eventService.getEventById(updated.id()).eventProfessionals())
                .extracting(EventProfessionalSummaryDto::professionalId)
                .containsExactly(assistant.id());
    }

    /**
     * TB_EVENT_PROFESSIONAL references TB_EVENT without ON DELETE CASCADE, so deleting an event that
     * has a team used to be a foreign key violation waiting to happen -- unreachable only because
     * nothing could write the association rows before this change.
     */
    @Test
    void deleteEvent_withATeam_removesTheAssociationRowsInsteadOfViolatingTheForeignKey() {
        ProfessionalDto photographer = professionalService.createProfessional(
                new CreateProfessionalDto("Integration Photographer 3", null, null, null, null));

        EventDto created = eventService.createEvent(new CreateEventDto(
                "EVT-INTEGRATION-005", null, "Integration Test Event 5", null,
                null, null, null, null, null, null, null,
                null, null, null,
                List.of(new EventProfessionalAssignmentDto(photographer.id(), "Fotografo principal"))));
        entityManager.flush();

        eventService.deleteEvent(created.id());
        entityManager.flush();

        assertThat(eventProfessionalRepository.findByEvent_Id(created.id())).isEmpty();
        assertThatThrownBy(() -> eventService.getEventById(created.id()))
                .isInstanceOf(EventNotFoundException.class);
    }
}
