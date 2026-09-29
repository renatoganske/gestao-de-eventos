package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalAssignmentDto;
import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalSummaryDto;
import com.renatoganske.gestao_de_eventos.entities.Customer;
import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.entities.EventType;
import com.renatoganske.gestao_de_eventos.entities.EventProfessional;
import com.renatoganske.gestao_de_eventos.entities.EventVenue;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.entities.Professional;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.filters.EventSearchCriteria;
import com.renatoganske.gestao_de_eventos.exceptions.CustomerNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.EventNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.EventTypeNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.EventVenueNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.HdNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.ProfessionalNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.CustomerRepository;
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
                .eventVenue(eventVenue)
                .customer(customer)
                .build();

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
                eventVenue.getId(),
                customer.getId(), null);
    }

    @Test
    void createEvent_resolvesAssociationsByIdSavesAndReturnsResponseDto() {
        when(hdRepository.findById(hd.getId())).thenReturn(Optional.of(hd));
        when(eventVenueRepository.findById(eventVenue.getId())).thenReturn(Optional.of(eventVenue));
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        when(eventRepository.save(captor.capture())).thenReturn(event);

        EventDto result = eventService.createEvent(createEventDto);

        assertThat(captor.getValue().getEventCode()).isEqualTo(createEventDto.eventCode());
        assertThat(captor.getValue().getType()).isEqualTo(weddingType);
        assertThat(captor.getValue().getName()).isEqualTo(createEventDto.name());
        assertThat(captor.getValue().getSizeGb()).isEqualTo(createEventDto.sizeGb());
        assertThat(captor.getValue().getDeliveryStatus()).isEqualTo(createEventDto.deliveryStatus());
        assertThat(captor.getValue().getHd()).isEqualTo(hd);
        assertThat(captor.getValue().getEventVenue()).isEqualTo(eventVenue);
        assertThat(captor.getValue().getCustomer()).isEqualTo(customer);

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
        assertThat(captor.getValue().getEventVenue()).isNull();
        assertThat(captor.getValue().getCustomer()).isNull();
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
                null, null, missingCustomerId, null);
        when(customerRepository.findById(missingCustomerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(dto))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(missingCustomerId.toString());

        verify(eventRepository, never()).save(any());
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
                null, missingEventVenueId, null, null);
        when(eventVenueRepository.findById(missingEventVenueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(dto))
                .isInstanceOf(EventVenueNotFoundException.class)
                .hasMessageContaining(missingEventVenueId.toString());

        verify(eventRepository, never()).save(any());
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
                eventVenue.getId(),
                customer.getId(), null);
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
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
        assertThat(result.customerId()).isEqualTo(customer.getId());
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

        verify(eventRepository, never()).delete(any());
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

    @Test
    void searchEvents_appliesCombinedFiltersOverAllEvents() {
        Event other = Event.builder()
                .id(UUID.randomUUID())
                .name("Aniversario 15 anos")
                .type(birthdayType)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build();
        when(eventRepository.findAll()).thenReturn(List.of(event, other));

        List<EventDto> result = eventService.searchEvents(new EventSearchCriteria(
                weddingType.getId(), null, null, null, null, null, null, null, null, null, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(event.getId());
    }

    @Test
    void searchEvents_noFiltersReturnsAllEvents() {
        Event other = Event.builder().id(UUID.randomUUID()).name("Aniversario 15 anos").build();
        when(eventRepository.findAll()).thenReturn(List.of(event, other));

        List<EventDto> result = eventService.searchEvents(EventSearchCriteria.none());

        assertThat(result).hasSize(2);
    }

    @Test
    void searchEvents_byCustomerNameMatchesPartialCaseInsensitive() {
        Event other = Event.builder()
                .id(UUID.randomUUID())
                .name("Aniversario 15 anos")
                .customer(Customer.builder().name("Ana Souza").build())
                .build();
        when(eventRepository.findAll()).thenReturn(List.of(event, other));

        List<EventDto> result = eventService.searchEvents(new EventSearchCriteria(
                null, null, null, null, null, null, null, "maria", null, null, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(event.getId());
    }

    @Test
    void searchEvents_byEventCodeMatchesPartialCaseInsensitive() {
        Event other = Event.builder()
                .id(UUID.randomUUID())
                .name("Aniversario 15 anos")
                .eventCode("EVT-002")
                .build();
        when(eventRepository.findAll()).thenReturn(List.of(event, other));

        List<EventDto> result = eventService.searchEvents(new EventSearchCriteria(
                null, null, null, null, null, null, null, null, "evt-001", null, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(event.getId());
    }

    @Test
    void searchEvents_byOutdoorWeddingReturnsOnlyExplicitMatches() {
        Event outdoor = Event.builder().id(UUID.randomUUID()).name("Casamento ao ar livre").outdoorWedding(true).build();
        Event indoor = Event.builder().id(UUID.randomUUID()).name("Casamento no salao").outdoorWedding(false).build();
        Event notAWedding = Event.builder().id(UUID.randomUUID()).name("Aniversario 15 anos").build();
        when(eventRepository.findAll()).thenReturn(List.of(outdoor, indoor, notAWedding));

        List<EventDto> onlyOutdoor = eventService.searchEvents(new EventSearchCriteria(
                null, null, null, null, null, null, null, null, null, null, true));
        List<EventDto> onlyIndoor = eventService.searchEvents(new EventSearchCriteria(
                null, null, null, null, null, null, null, null, null, null, false));

        assertThat(onlyOutdoor).extracting(EventDto::id).containsExactly(outdoor.getId());
        assertThat(onlyIndoor).extracting(EventDto::id).containsExactly(indoor.getId());
    }

    @Test
    void searchEvents_byDaytimeWeddingReturnsOnlyExplicitMatches() {
        Event daytime = Event.builder().id(UUID.randomUUID()).name("Casamento de dia").daytimeWedding(true).build();
        Event night = Event.builder().id(UUID.randomUUID()).name("Casamento a noite").daytimeWedding(false).build();
        Event notAWedding = Event.builder().id(UUID.randomUUID()).name("Aniversario 15 anos").build();
        when(eventRepository.findAll()).thenReturn(List.of(daytime, night, notAWedding));

        List<EventDto> onlyDaytime = eventService.searchEvents(new EventSearchCriteria(
                null, null, null, null, null, null, null, null, null, true, null));
        List<EventDto> onlyNight = eventService.searchEvents(new EventSearchCriteria(
                null, null, null, null, null, null, null, null, null, false, null));

        assertThat(onlyDaytime).extracting(EventDto::id).containsExactly(daytime.getId());
        assertThat(onlyNight).extracting(EventDto::id).containsExactly(night.getId());
    }

    @Test
    void searchEvents_weddingFlagsCombineWithOtherFilters() {
        Event outdoorDaytime = Event.builder().id(UUID.randomUUID()).type(weddingType)
                .daytimeWedding(true).outdoorWedding(true).build();
        Event outdoorNight = Event.builder().id(UUID.randomUUID()).type(weddingType)
                .daytimeWedding(false).outdoorWedding(true).build();
        when(eventRepository.findAll()).thenReturn(List.of(outdoorDaytime, outdoorNight));

        List<EventDto> result = eventService.searchEvents(new EventSearchCriteria(
                weddingType.getId(), null, null, null, null, null, null, null, null, true, true));

        assertThat(result).extracting(EventDto::id).containsExactly(outdoorDaytime.getId());
    }
}
