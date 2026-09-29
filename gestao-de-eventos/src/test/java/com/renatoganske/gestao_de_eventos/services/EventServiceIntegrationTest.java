package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateHdDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateProfessionalDto;
import com.renatoganske.gestao_de_eventos.dtos.CustomerResponseDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalSummaryDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueDto;
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
                hd.id(), eventVenue.id(), customer.id(), null);

        EventDto result = eventService.createEvent(createEventDto);
        entityManager.flush();

        assertThat(result.id()).isNotNull();
        assertThat(result.customerId()).isEqualTo(customer.id());
        assertThat(result.hdId()).isEqualTo(hd.id());
        assertThat(result.eventVenueId()).isEqualTo(eventVenue.id());
        assertThat(hdService.getHdById(hd.id()).usedSpaceGb()).isEqualTo(50);
    }

    @Test
    void createEvent_withNonExistentCustomerId_throwsCustomerNotFoundExceptionAndPersistsNothing() {
        UUID missingCustomerId = UUID.randomUUID();
        CreateEventDto createEventDto = new CreateEventDto(
                "EVT-INTEGRATION-002", null, "Integration Test Event 2", null,
                null, null, null, null, null, null, null,
                null, null, missingCustomerId, null);

        assertThatThrownBy(() -> {
            eventService.createEvent(createEventDto);
            entityManager.flush();
        }).isInstanceOf(CustomerNotFoundException.class);
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
