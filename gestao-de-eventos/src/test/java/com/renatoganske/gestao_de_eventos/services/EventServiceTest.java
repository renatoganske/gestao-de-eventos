package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventCustomerAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueSummaryDto;
import com.renatoganske.gestao_de_eventos.dtos.EventCustomerSummaryDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventListItemDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalSummaryDto;
import com.renatoganske.gestao_de_eventos.dtos.PageDto;
import com.renatoganske.gestao_de_eventos.entities.Customer;
import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.entities.EventCustomer;
import com.renatoganske.gestao_de_eventos.entities.EventCustomerId;
import com.renatoganske.gestao_de_eventos.entities.EventType;
import com.renatoganske.gestao_de_eventos.entities.EventProfessional;
import com.renatoganske.gestao_de_eventos.entities.EventEventVenue;
import com.renatoganske.gestao_de_eventos.entities.EventEventVenueId;
import com.renatoganske.gestao_de_eventos.entities.EventVenue;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.entities.Professional;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.filters.EventSearchCriteria;
import com.renatoganske.gestao_de_eventos.filters.EventSort;
import com.renatoganske.gestao_de_eventos.exceptions.CustomerNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.EventNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.EventTypeNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.EventVenueNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.HdNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.InvalidRequestParameterException;
import com.renatoganske.gestao_de_eventos.exceptions.ProfessionalNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.CustomerRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventCustomerRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventEventVenueRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventProfessionalRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventTypeRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventVenueRepository;
import com.renatoganske.gestao_de_eventos.repositories.HdRepository;
import com.renatoganske.gestao_de_eventos.repositories.ProfessionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private HdRepository hdRepository;

    @Mock
    private EventVenueRepository eventVenueRepository;

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private EventProfessionalRepository eventProfessionalRepository;

    @Mock
    private EventCustomerRepository eventCustomerRepository;

    @Mock
    private EventEventVenueRepository eventEventVenueRepository;

    @InjectMocks
    private EventService eventService;

    private Hd hd;
    private EventVenue eventVenue;
    private Customer customer;
    private EventType weddingType;
    private EventType birthdayType;
    private Professional photographer;
    private Professional assistant;
    private Event event;
    private CreateEventDto createEventDto;

    @BeforeEach
    void setUp() {
        hd = Hd.builder().id(UUID.randomUUID()).name("HD Externo 1").usedSpaceGb(500).build();
        eventVenue = EventVenue.builder().id(UUID.randomUUID()).name("Buffet Jardim das Rosas").build();
        customer = Customer.builder().id(UUID.randomUUID()).name("Maria Silva").build();
        weddingType = EventType.builder().id(UUID.randomUUID()).name("WEDDING").build();
        birthdayType = EventType.builder().id(UUID.randomUUID()).name("BIRTHDAY").build();
        lenient().when(eventTypeRepository.findById(weddingType.getId())).thenReturn(Optional.of(weddingType));
        lenient().when(eventTypeRepository.findById(birthdayType.getId())).thenReturn(Optional.of(birthdayType));

        photographer = Professional.builder().id(UUID.randomUUID()).name("Renato").build();
        assistant = Professional.builder().id(UUID.randomUUID()).name("Fernanda").build();

        event = Event.builder()
                .id(UUID.randomUUID())
                .eventCode("EVT-001")
                .type(weddingType)
                .name("Casamento Maria e Joao")
                .eventDate(LocalDate.of(2026, 10, 15))
                .daytimeWedding(true)
                .outdoorWedding(false)
                .guestCount(150L)
                .description("Casamento no salao principal")
                .amount(8000.0)
                .sizeGb(50)
                .deliveryStatus(DeliveryStatus.PENDING)
                .hd(hd)
                .build();
        event.setEventCustomers(List.of(link(event, customer, true)));
        event.setEventVenues(List.of(venueLink(event, eventVenue, "Recepção")));

        createEventDto = new CreateEventDto(
                event.getEventCode(),
                weddingType.getId(),
                event.getName(),
                event.getEventDate(),
                event.getDaytimeWedding(),
                event.getOutdoorWedding(),
                event.getGuestCount(),
                event.getDescription(),
                event.getAmount(),
                event.getSizeGb(),
                event.getDeliveryStatus(),
                hd.getId(),
                List.of(new EventVenueAssignmentDto(eventVenue.getId(), "Recepção")),
                List.of(new EventCustomerAssignmentDto(customer.getId(), true)), null);
    }

    @Test
    void createEvent_resolvesAssociationsByIdSavesAndReturnsResponseDto() {
        when(hdRepository.findById(hd.getId())).thenReturn(Optional.of(hd));
        when(eventVenueRepository.findById(eventVenue.getId())).thenReturn(Optional.of(eventVenue));
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        when(eventRepository.save(captor.capture())).thenReturn(event);
        ArgumentCaptor<List<EventCustomer>> customersCaptor = ArgumentCaptor.forClass(List.class);
        when(eventCustomerRepository.saveAll(customersCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<List<EventEventVenue>> venuesCaptor = ArgumentCaptor.forClass(List.class);
        when(eventEventVenueRepository.saveAll(venuesCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        EventDto result = eventService.createEvent(createEventDto);

        assertThat(captor.getValue().getEventCode()).isEqualTo(createEventDto.eventCode());
        assertThat(captor.getValue().getType()).isEqualTo(weddingType);
        assertThat(captor.getValue().getName()).isEqualTo(createEventDto.name());
        assertThat(captor.getValue().getSizeGb()).isEqualTo(createEventDto.sizeGb());
        assertThat(captor.getValue().getDeliveryStatus()).isEqualTo(createEventDto.deliveryStatus());
        assertThat(captor.getValue().getHd()).isEqualTo(hd);
        assertThat(venuesCaptor.getValue())
                .extracting(EventEventVenue::getEventVenue, EventEventVenue::getRole)
                .containsExactly(tuple(eventVenue, "Recepção"));
        assertThat(customersCaptor.getValue())
                .extracting(EventCustomer::getCustomer, EventCustomer::isContracting)
                .containsExactly(tuple(customer, true));

        assertThat(result.id()).isEqualTo(event.getId());
        assertThat(result.name()).isEqualTo(event.getName());
        assertThat(result.deliveryStatus()).isEqualTo(event.getDeliveryStatus());
    }

    @Test
    void createEvent_withoutAssociations_leavesThemNull() {
        CreateEventDto dtoWithoutAssociations = new CreateEventDto(
                "EVT-003", null, "Ensaio solo", null,
                null, null, null, null, null, null, null,
                null, null, null, null);
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        when(eventRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        eventService.createEvent(dtoWithoutAssociations);

        assertThat(captor.getValue().getHd()).isNull();
        verify(eventEventVenueRepository, never()).saveAll(any());
        verify(eventCustomerRepository, never()).saveAll(any());
        verify(hdRepository, never()).save(any());
    }

    @Test
    void createEvent_withHdButNoSizeGb_doesNotAdjustUsedSpace() {
        CreateEventDto dtoWithHdButNoSizeGb = new CreateEventDto(
                "EVT-008", null, "Evento sem tamanho definido", null,
                null, null, null, null, null, null, null,
                hd.getId(), null, null, null);
        when(hdRepository.findById(hd.getId())).thenReturn(Optional.of(hd));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        eventService.createEvent(dtoWithHdButNoSizeGb);

        assertThat(hd.getUsedSpaceGb()).isEqualTo(500);
        verify(hdRepository, never()).save(any());
    }

    @Test
    void createEvent_incrementsHdUsedSpaceBySizeGb() {
        when(hdRepository.findById(hd.getId())).thenReturn(Optional.of(hd));
        when(eventVenueRepository.findById(eventVenue.getId())).thenReturn(Optional.of(eventVenue));
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        eventService.createEvent(createEventDto);

        assertThat(hd.getUsedSpaceGb()).isEqualTo(550);
        verify(hdRepository, times(1)).save(hd);
    }

    @Test
    void createEvent_throwsCustomerNotFoundException_whenCustomerIdDoesNotExist() {
        UUID missingCustomerId = UUID.randomUUID();
        CreateEventDto dto = new CreateEventDto(
                "EVT-004", null, "Evento sem cliente valido", null,
                null, null, null, null, null, null, null,
                null, null, List.of(new EventCustomerAssignmentDto(missingCustomerId, true)), null);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(customerRepository.findById(missingCustomerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(dto))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(missingCustomerId.toString());

        verify(eventCustomerRepository, never()).saveAll(any());
    }

    @Test
    void createEvent_throwsHdNotFoundException_whenHdIdDoesNotExist() {
        UUID missingHdId = UUID.randomUUID();
        CreateEventDto dto = new CreateEventDto(
                "EVT-005", null, "Evento sem HD valido", null,
                null, null, null, null, null, null, null,
                missingHdId, null, null, null);
        when(hdRepository.findById(missingHdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(dto))
                .isInstanceOf(HdNotFoundException.class)
                .hasMessageContaining(missingHdId.toString());

        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_throwsEventVenueNotFoundException_whenEventVenueIdDoesNotExist() {
        UUID missingEventVenueId = UUID.randomUUID();
        CreateEventDto dto = new CreateEventDto(
                "EVT-007", null, "Evento sem local valido", null,
                null, null, null, null, null, null, null,
                null, List.of(new EventVenueAssignmentDto(missingEventVenueId, null)), null, null);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventVenueRepository.findById(missingEventVenueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(dto))
                .isInstanceOf(EventVenueNotFoundException.class)
                .hasMessageContaining(missingEventVenueId.toString());

        verify(eventEventVenueRepository, never()).saveAll(any());
    }

    @Test
    void createEvent_throwsEventTypeNotFoundException_whenEventTypeIdDoesNotExist() {
        UUID missingEventTypeId = UUID.randomUUID();
        CreateEventDto dto = new CreateEventDto(
                "EVT-006", missingEventTypeId, "Evento sem tipo valido", null,
                null, null, null, null, null, null, null,
                null, null, null, null);
        when(eventTypeRepository.findById(missingEventTypeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(dto))
                .isInstanceOf(EventTypeNotFoundException.class)
                .hasMessageContaining(missingEventTypeId.toString());

        verify(eventRepository, never()).save(any());
    }

    @Test
    void getAllEvents_returnsAllMappedEvents() {
        Event other = Event.builder().id(UUID.randomUUID()).name("Aniversario 15 anos").build();
        when(eventRepository.findAll()).thenReturn(List.of(event, other));

        List<EventDto> result = eventService.getAllEvents();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EventDto::name)
                .containsExactly(event.getName(), other.getName());
    }

    @Test
    void getAllEvents_returnsEmptyListWhenNoEvents() {
        when(eventRepository.findAll()).thenReturn(List.of());

        List<EventDto> result = eventService.getAllEvents();

        assertThat(result).isEmpty();
    }

    @Test
    void getEventById_returnsEventWhenFound() {
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        EventDto result = eventService.getEventById(event.getId());

        assertThat(result.id()).isEqualTo(event.getId());
        assertThat(result.name()).isEqualTo(event.getName());
    }

    @Test
    void getEventById_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventById(id))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateEvent_updatesAndReturnsEventWhenFound() {
        UUID id = event.getId();
        CreateEventDto updateDto = new CreateEventDto(
                "EVT-002",
                birthdayType.getId(),
                "Aniversario Ana",
                LocalDate.of(2026, 12, 1),
                false,
                true,
                80L,
                "Festa ao ar livre",
                3000.0,
                20,
                DeliveryStatus.DELIVERED,
                hd.getId(),
                List.of(new EventVenueAssignmentDto(eventVenue.getId(), "Cerimônia")),
                List.of(new EventCustomerAssignmentDto(customer.getId(), true)), null);
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventCustomerRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventEventVenueRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hdRepository.findById(hd.getId())).thenReturn(Optional.of(hd));
        when(eventVenueRepository.findById(eventVenue.getId())).thenReturn(Optional.of(eventVenue));
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        EventDto result = eventService.updateEvent(id, updateDto);

        assertThat(result.eventCode()).isEqualTo("EVT-002");
        assertThat(result.type().id()).isEqualTo(birthdayType.getId());
        assertThat(result.type().name()).isEqualTo("BIRTHDAY");
        assertThat(result.name()).isEqualTo("Aniversario Ana");
        assertThat(result.sizeGb()).isEqualTo(20);
        assertThat(result.deliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(result.customers())
                .extracting(EventCustomerSummaryDto::customerId, EventCustomerSummaryDto::contracting)
                .containsExactly(tuple(customer.getId(), true));
        assertThat(result.venues())
                .extracting(EventVenueSummaryDto::venueId, EventVenueSummaryDto::role)
                .containsExactly(tuple(eventVenue.getId(), "Cerimônia"));
    }

    @Test
    void updateEvent_adjustsSameHdUsedSpaceByDeltaBetweenOldAndNewSizeGb() {
        UUID id = event.getId();
        CreateEventDto updateDto = new CreateEventDto(
                "EVT-002", null, "Aniversario Ana", null,
                null, null, null, null, null, 20, DeliveryStatus.DELIVERED,
                hd.getId(), null, null, null);
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hdRepository.findById(hd.getId())).thenReturn(Optional.of(hd));

        // event original tinha sizeGb=50 no mesmo hd (usedSpaceGb inicial = 500);
        // update troca para sizeGb=20 -> delta = -50 + 20 = -30
        eventService.updateEvent(id, updateDto);

        assertThat(hd.getUsedSpaceGb()).isEqualTo(470);
    }

    @Test
    void updateEvent_movesUsedSpaceFromOldHdToNewHd_whenHdChanges() {
        UUID id = event.getId();
        Hd newHd = Hd.builder().id(UUID.randomUUID()).name("HD Externo 2").usedSpaceGb(100).build();
        CreateEventDto updateDto = new CreateEventDto(
                "EVT-002", null, "Aniversario Ana", null,
                null, null, null, null, null, 50, DeliveryStatus.DELIVERED,
                newHd.getId(), null, null, null);
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hdRepository.findById(newHd.getId())).thenReturn(Optional.of(newHd));

        eventService.updateEvent(id, updateDto);

        assertThat(hd.getUsedSpaceGb()).isEqualTo(450);
        assertThat(newHd.getUsedSpaceGb()).isEqualTo(150);
    }

    @Test
    void updateEvent_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateEvent(id, createEventDto))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(eventRepository, never()).save(any());
    }

    @Test
    void deleteEvent_deletesWhenFound() {
        UUID id = event.getId();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));

        eventService.deleteEvent(id);

        verify(eventRepository, times(1)).delete(event);
    }

    @Test
    void deleteEvent_decrementsHdUsedSpaceBySizeGb() {
        UUID id = event.getId();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));

        eventService.deleteEvent(id);

        assertThat(hd.getUsedSpaceGb()).isEqualTo(450);
        verify(hdRepository, times(1)).save(hd);
    }

    @Test
    void deleteEvent_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.deleteEvent(id))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void createEvent_withProfessionals_persistsAssociationsKeyedByEventAndProfessional() {
        CreateEventDto dto = dtoWithProfessionals(List.of(
                new EventProfessionalAssignmentDto(photographer.getId(), "Fotografo principal"),
                new EventProfessionalAssignmentDto(assistant.getId(), "Segundo fotografo")));
        stubProfessionals(photographer, assistant);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        ArgumentCaptor<List<EventProfessional>> captor = ArgumentCaptor.forClass(List.class);
        when(eventProfessionalRepository.saveAll(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        eventService.createEvent(dto);

        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).allSatisfy(association -> {
            assertThat(association.getId().eventId()).isEqualTo(event.getId());
            assertThat(association.getEvent()).isSameAs(event);
        });
        assertThat(captor.getValue())
                .extracting(association -> association.getId().professionalId(), EventProfessional::getRoleInEvent)
                .containsExactly(
                        tuple(photographer.getId(), "Fotografo principal"),
                        tuple(assistant.getId(), "Segundo fotografo"));
    }

    @Test
    void createEvent_withProfessionals_exposesThemInTheResponseDto() {
        CreateEventDto dto = dtoWithProfessionals(List.of(
                new EventProfessionalAssignmentDto(photographer.getId(), "Fotografo principal")));
        stubProfessionals(photographer);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventProfessionalRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EventDto result = eventService.createEvent(dto);

        assertThat(result.eventProfessionals())
                .extracting(EventProfessionalSummaryDto::professionalId, EventProfessionalSummaryDto::roleInEvent)
                .containsExactly(tuple(photographer.getId(), "Fotografo principal"));
    }

    @Test
    void createEvent_withNullProfessionals_leavesTheAssociationTableUntouched() {
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        eventService.createEvent(dtoWithProfessionals(null));

        verify(eventProfessionalRepository, never()).deleteByEvent_Id(any());
        verify(eventProfessionalRepository, never()).saveAll(any());
    }

    @Test
    void createEvent_withRepeatedProfessional_keepsOnlyTheLastRole() {
        CreateEventDto dto = dtoWithProfessionals(List.of(
                new EventProfessionalAssignmentDto(photographer.getId(), "Fotografo principal"),
                new EventProfessionalAssignmentDto(photographer.getId(), "Cinegrafista")));
        stubProfessionals(photographer);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        ArgumentCaptor<List<EventProfessional>> captor = ArgumentCaptor.forClass(List.class);
        when(eventProfessionalRepository.saveAll(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        eventService.createEvent(dto);

        assertThat(captor.getValue())
                .extracting(EventProfessional::getRoleInEvent)
                .containsExactly("Cinegrafista");
    }

    @Test
    void createEvent_withUnknownProfessional_throwsProfessionalNotFoundException() {
        UUID missingProfessionalId = UUID.randomUUID();
        CreateEventDto dto = dtoWithProfessionals(List.of(
                new EventProfessionalAssignmentDto(missingProfessionalId, "Fotografo principal")));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(professionalRepository.findById(missingProfessionalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(dto))
                .isInstanceOf(ProfessionalNotFoundException.class)
                .hasMessageContaining(missingProfessionalId.toString());

        verify(eventProfessionalRepository, never()).saveAll(any());
    }

    @Test
    void updateEvent_withProfessionals_replacesTheWholeTeam() {
        UUID id = event.getId();
        CreateEventDto dto = dtoWithProfessionals(List.of(
                new EventProfessionalAssignmentDto(assistant.getId(), "Fotografo principal")));
        stubProfessionals(assistant);
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventProfessionalRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        eventService.updateEvent(id, dto);

        verify(eventProfessionalRepository, times(1)).deleteByEvent_Id(id);
        verify(eventProfessionalRepository, times(1)).flush();
        verify(eventProfessionalRepository, times(1)).saveAll(any());
    }

    @Test
    void updateEvent_withEmptyProfessionals_clearsTheTeam() {
        UUID id = event.getId();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<List<EventProfessional>> captor = ArgumentCaptor.forClass(List.class);
        when(eventProfessionalRepository.saveAll(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        eventService.updateEvent(id, dtoWithProfessionals(List.of()));

        verify(eventProfessionalRepository, times(1)).deleteByEvent_Id(id);
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void updateEvent_withNullProfessionals_keepsTheCurrentTeam() {
        UUID id = event.getId();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        eventService.updateEvent(id, dtoWithProfessionals(null));

        verify(eventProfessionalRepository, never()).deleteByEvent_Id(any());
        verify(eventProfessionalRepository, never()).saveAll(any());
    }

    @Test
    void deleteEvent_removesAssociationsBeforeDeletingTheEvent() {
        UUID id = event.getId();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        InOrder inOrder = inOrder(eventProfessionalRepository, eventRepository);

        eventService.deleteEvent(id);

        inOrder.verify(eventProfessionalRepository).deleteByEvent_Id(id);
        inOrder.verify(eventProfessionalRepository).flush();
        inOrder.verify(eventRepository).delete(event);
    }

    @Test
    void createEvent_withSeveralCustomers_persistsOneAssociationEachWithTheContractingFlag() {
        Customer partner = Customer.builder().id(UUID.randomUUID()).name("Joao Silva").build();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.findById(partner.getId())).thenReturn(Optional.of(partner));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        ArgumentCaptor<List<EventCustomer>> captor = ArgumentCaptor.forClass(List.class);
        when(eventCustomerRepository.saveAll(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        eventService.createEvent(dtoWithCustomers(List.of(
                new EventCustomerAssignmentDto(customer.getId(), true),
                new EventCustomerAssignmentDto(partner.getId(), null))));

        assertThat(captor.getValue()).allSatisfy(association -> {
            assertThat(association.getId().eventId()).isEqualTo(event.getId());
            assertThat(association.getEvent()).isSameAs(event);
        });
        assertThat(captor.getValue())
                .extracting(association -> association.getId().customerId(), EventCustomer::isContracting)
                .containsExactly(tuple(customer.getId(), true), tuple(partner.getId(), false));
    }

    @Test
    void createEvent_withSeveralCustomers_listsTheContractingOneFirstInTheResponseDto() {
        Customer partner = Customer.builder().id(UUID.randomUUID()).name("Joao Silva").build();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.findById(partner.getId())).thenReturn(Optional.of(partner));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventCustomerRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EventDto result = eventService.createEvent(dtoWithCustomers(List.of(
                new EventCustomerAssignmentDto(partner.getId(), false),
                new EventCustomerAssignmentDto(customer.getId(), true))));

        assertThat(result.customers())
                .extracting(EventCustomerSummaryDto::customerName, EventCustomerSummaryDto::contracting)
                .containsExactly(tuple("Maria Silva", true), tuple("Joao Silva", false));
    }

    @Test
    void createEvent_withRepeatedCustomer_keepsASingleAssociationMarkedContractingIfAnyIs() {
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        ArgumentCaptor<List<EventCustomer>> captor = ArgumentCaptor.forClass(List.class);
        when(eventCustomerRepository.saveAll(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        eventService.createEvent(dtoWithCustomers(List.of(
                new EventCustomerAssignmentDto(customer.getId(), true),
                new EventCustomerAssignmentDto(customer.getId(), false))));

        assertThat(captor.getValue())
                .extracting(EventCustomer::getCustomer, EventCustomer::isContracting)
                .containsExactly(tuple(customer, true));
    }

    @Test
    void updateEvent_withEmptyCustomers_removesThemAll() {
        UUID id = event.getId();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<List<EventCustomer>> captor = ArgumentCaptor.forClass(List.class);
        when(eventCustomerRepository.saveAll(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        EventDto result = eventService.updateEvent(id, dtoWithCustomers(List.of()));

        verify(eventCustomerRepository, times(1)).deleteByEvent_Id(id);
        verify(eventCustomerRepository, times(1)).flush();
        assertThat(captor.getValue()).isEmpty();
        assertThat(result.customers()).isEmpty();
    }

    @Test
    void updateEvent_withNullCustomers_keepsTheCurrentOnes() {
        UUID id = event.getId();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventDto result = eventService.updateEvent(id, dtoWithCustomers(null));

        verify(eventCustomerRepository, never()).deleteByEvent_Id(any());
        verify(eventCustomerRepository, never()).saveAll(any());
        assertThat(result.customers()).extracting(EventCustomerSummaryDto::customerId).containsExactly(customer.getId());
    }

    @Test
    void deleteEvent_removesCustomerAssociationsBeforeDeletingTheEvent() {
        UUID id = event.getId();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        InOrder inOrder = inOrder(eventCustomerRepository, eventRepository);

        eventService.deleteEvent(id);

        inOrder.verify(eventCustomerRepository).deleteByEvent_Id(id);
        inOrder.verify(eventCustomerRepository).flush();
        inOrder.verify(eventRepository).delete(event);
    }

    @Test
    void createEvent_withSeveralVenues_persistsOneAssociationEachWithItsRole() {
        EventVenue church = EventVenue.builder().id(UUID.randomUUID()).name("Igreja Matriz").build();
        stubVenues(church, eventVenue);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        ArgumentCaptor<List<EventEventVenue>> captor = ArgumentCaptor.forClass(List.class);
        when(eventEventVenueRepository.saveAll(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        eventService.createEvent(dtoWithVenues(List.of(
                new EventVenueAssignmentDto(church.getId(), "Cerimônia"),
                new EventVenueAssignmentDto(eventVenue.getId(), null))));

        assertThat(captor.getValue()).allSatisfy(association -> {
            assertThat(association.getId().eventId()).isEqualTo(event.getId());
            assertThat(association.getEvent()).isSameAs(event);
        });
        assertThat(captor.getValue())
                .extracting(association -> association.getId().eventVenueId(), EventEventVenue::getRole)
                .containsExactly(tuple(church.getId(), "Cerimônia"), tuple(eventVenue.getId(), null));
    }

    @Test
    void createEvent_withSeveralVenues_listsThemByNameInTheResponseDto() {
        EventVenue church = EventVenue.builder().id(UUID.randomUUID()).name("Igreja Matriz").build();
        stubVenues(church, eventVenue);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventEventVenueRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EventDto result = eventService.createEvent(dtoWithVenues(List.of(
                new EventVenueAssignmentDto(eventVenue.getId(), "Recepção"),
                new EventVenueAssignmentDto(church.getId(), "Cerimônia"))));

        assertThat(result.venues())
                .extracting(EventVenueSummaryDto::venueName, EventVenueSummaryDto::role)
                .containsExactly(tuple("Buffet Jardim das Rosas", "Recepção"), tuple("Igreja Matriz", "Cerimônia"));
    }

    @Test
    void createEvent_withRepeatedVenue_keepsASingleAssociationWithTheLastRole() {
        stubVenues(eventVenue);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        ArgumentCaptor<List<EventEventVenue>> captor = ArgumentCaptor.forClass(List.class);
        when(eventEventVenueRepository.saveAll(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        eventService.createEvent(dtoWithVenues(List.of(
                new EventVenueAssignmentDto(eventVenue.getId(), "Cerimônia"),
                new EventVenueAssignmentDto(eventVenue.getId(), "Cerimônia e recepção"))));

        assertThat(captor.getValue())
                .extracting(EventEventVenue::getEventVenue, EventEventVenue::getRole)
                .containsExactly(tuple(eventVenue, "Cerimônia e recepção"));
    }

    @Test
    void updateEvent_withEmptyVenues_removesThemAll() {
        UUID id = event.getId();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<List<EventEventVenue>> captor = ArgumentCaptor.forClass(List.class);
        when(eventEventVenueRepository.saveAll(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        EventDto result = eventService.updateEvent(id, dtoWithVenues(List.of()));

        verify(eventEventVenueRepository, times(1)).deleteByEvent_Id(id);
        verify(eventEventVenueRepository, times(1)).flush();
        assertThat(captor.getValue()).isEmpty();
        assertThat(result.venues()).isEmpty();
    }

    @Test
    void updateEvent_withNullVenues_keepsTheCurrentOnes() {
        UUID id = event.getId();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventDto result = eventService.updateEvent(id, dtoWithVenues(null));

        verify(eventEventVenueRepository, never()).deleteByEvent_Id(any());
        verify(eventEventVenueRepository, never()).saveAll(any());
        assertThat(result.venues()).extracting(EventVenueSummaryDto::venueId).containsExactly(eventVenue.getId());
    }

    @Test
    void deleteEvent_removesVenueAssociationsBeforeDeletingTheEvent() {
        UUID id = event.getId();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        InOrder inOrder = inOrder(eventEventVenueRepository, eventRepository);

        eventService.deleteEvent(id);

        inOrder.verify(eventEventVenueRepository).deleteByEvent_Id(id);
        inOrder.verify(eventEventVenueRepository).flush();
        inOrder.verify(eventRepository).delete(event);
    }

    private CreateEventDto dtoWithVenues(List<EventVenueAssignmentDto> venues) {
        return new CreateEventDto(
                "EVT-300", null, "Evento com locais", null,
                null, null, null, null, null, null, null,
                null, venues, null, null);
    }

    private void stubVenues(EventVenue... venues) {
        for (EventVenue venue : venues) {
            when(eventVenueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        }
    }

    private static EventEventVenue venueLink(Event event, EventVenue venue, String role) {
        return EventEventVenue.builder()
                .id(new EventEventVenueId(event.getId(), venue.getId()))
                .event(event)
                .eventVenue(venue)
                .role(role)
                .build();
    }

    private CreateEventDto dtoWithCustomers(List<EventCustomerAssignmentDto> customers) {
        return new CreateEventDto(
                "EVT-200", null, "Evento com clientes", null,
                null, null, null, null, null, null, null,
                null, null, customers, null);
    }

    private static EventCustomer link(Event event, Customer customer, boolean contracting) {
        return EventCustomer.builder()
                .id(new EventCustomerId(event.getId(), customer.getId()))
                .event(event)
                .customer(customer)
                .contracting(contracting)
                .build();
    }

    private CreateEventDto dtoWithProfessionals(List<EventProfessionalAssignmentDto> professionals) {
        return new CreateEventDto(
                "EVT-100", null, "Evento com equipe", null,
                null, null, null, null, null, null, null,
                null, null, null, professionals);
    }

    private void stubProfessionals(Professional... professionals) {
        for (Professional professional : professionals) {
            when(professionalRepository.findById(professional.getId())).thenReturn(Optional.of(professional));
        }
    }

    // The filter rules themselves run in the database and are covered by EventSpecificationsIntegrationTest;
    // here only the delegation to the repository and the mapping of what it returns are checked.
    @Test
    void searchEvents_queriesTheRepositoryWithASpecificationAndTheRequestedPageAndMapsTheResult() {
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(event), invocation.getArgument(1), 41));

        PageDto<EventListItemDto> result = eventService.searchEvents(new EventSearchCriteria(
                weddingType.getId(), null, null, null, null, null, null, null, "maria", null, null, null, null),
                2, 20, EventSort.DEFAULT);

        assertThat(result.content()).extracting(EventListItemDto::id).containsExactly(event.getId());
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(41);
        assertThat(result.totalPages()).isEqualTo(3);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        // the order is set by the specification (some columns are subqueries), never by the Pageable
        assertThat(pageable.getValue().getSort().isUnsorted()).isTrue();
        verify(eventRepository, never()).findAll();
    }

    @Test
    void searchEvents_withNoCriteria_stillQueriesThroughTheSpecification() {
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        assertThat(eventService.searchEvents(EventSearchCriteria.none(), 0, 50, EventSort.DEFAULT).content()).isEmpty();
        verify(eventRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void searchEvents_rejectsANegativePage() {
        assertThatThrownBy(() -> eventService.searchEvents(EventSearchCriteria.none(), -1, 50, EventSort.DEFAULT))
                .isInstanceOf(InvalidRequestParameterException.class)
                .hasMessageContaining("page -1");
        verifyNoInteractions(eventRepository);
    }

    @Test
    void searchEvents_acceptsSizesFrom1ToTheMaximumAndRejectsTheRest() {
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        eventService.searchEvents(EventSearchCriteria.none(), 0, 1, EventSort.DEFAULT);
        eventService.searchEvents(EventSearchCriteria.none(), 0, EventService.MAX_PAGE_SIZE, EventSort.DEFAULT);

        assertThatThrownBy(() -> eventService.searchEvents(EventSearchCriteria.none(), 0, 0, EventSort.DEFAULT))
                .isInstanceOf(InvalidRequestParameterException.class);
        assertThatThrownBy(() -> eventService.searchEvents(
                EventSearchCriteria.none(), 0, EventService.MAX_PAGE_SIZE + 1, EventSort.DEFAULT))
                .isInstanceOf(InvalidRequestParameterException.class)
                .hasMessageContaining("1 to " + EventService.MAX_PAGE_SIZE);
    }

    @Test
    void getUpcomingEvents_rejectsALimitOutOfRangeWithoutQuerying() {
        assertThatThrownBy(() -> eventService.getUpcomingEvents(0))
                .isInstanceOf(InvalidRequestParameterException.class);
        assertThatThrownBy(() -> eventService.getUpcomingEvents(EventService.MAX_UPCOMING_LIMIT + 1))
                .isInstanceOf(InvalidRequestParameterException.class)
                .hasMessageContaining("1 to " + EventService.MAX_UPCOMING_LIMIT);
        verifyNoInteractions(eventRepository);
    }
}
