package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateCustomerDto;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.entities.Customer;
import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.entities.EventVenue;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.enums.EventType;
import com.renatoganske.gestao_de_eventos.exceptions.EventNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private Event event;
    private CreateEventDto createEventDto;

    @BeforeEach
    void setUp() {
        Hd hd = Hd.builder().id(UUID.randomUUID()).name("HD Externo 1").build();
        EventVenue eventVenue = EventVenue.builder().id(UUID.randomUUID()).name("Buffet Jardim das Rosas").build();
        Customer customer = Customer.builder().id(UUID.randomUUID()).name("Maria Silva").build();

        event = Event.builder()
                .id(UUID.randomUUID())
                .eventCode("EVT-001")
                .type(EventType.WEDDING)
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
                event.getType(),
                event.getName(),
                event.getEventDate(),
                event.getDaytimeWedding(),
                event.getOutdoorWedding(),
                event.getGuestCount(),
                event.getDescription(),
                event.getAmount(),
                event.getSizeGb(),
                event.getDeliveryStatus(),
                hd,
                eventVenue,
                new CreateCustomerDto(customer.getName(), null, null, null));
    }

    @Test
    void createEvent_savesAndReturnsResponseDto() {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        when(eventRepository.save(captor.capture())).thenReturn(event);

        EventDto result = eventService.createEvent(createEventDto);

        assertThat(captor.getValue().getEventCode()).isEqualTo(createEventDto.eventCode());
        assertThat(captor.getValue().getType()).isEqualTo(createEventDto.type());
        assertThat(captor.getValue().getName()).isEqualTo(createEventDto.name());
        assertThat(captor.getValue().getSizeGb()).isEqualTo(createEventDto.sizeGb());
        assertThat(captor.getValue().getDeliveryStatus()).isEqualTo(createEventDto.deliveryStatus());
        assertThat(captor.getValue().getHd()).isEqualTo(createEventDto.hd());
        assertThat(captor.getValue().getEventVenue()).isEqualTo(createEventDto.eventVenue());
        assertThat(captor.getValue().getCustomer().getName()).isEqualTo(createEventDto.customer().name());

        assertThat(result.id()).isEqualTo(event.getId());
        assertThat(result.name()).isEqualTo(event.getName());
        assertThat(result.deliveryStatus()).isEqualTo(event.getDeliveryStatus());
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
                EventType.BIRTHDAY,
                "Aniversario Ana",
                LocalDate.of(2026, 12, 1),
                false,
                true,
                80L,
                "Festa ao ar livre",
                3000.0,
                20,
                DeliveryStatus.DELIVERED,
                createEventDto.hd(),
                createEventDto.eventVenue(),
                new CreateCustomerDto("Ana Souza", null, null, null));
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventDto result = eventService.updateEvent(id, updateDto);

        assertThat(result.eventCode()).isEqualTo("EVT-002");
        assertThat(result.type()).isEqualTo(EventType.BIRTHDAY);
        assertThat(result.name()).isEqualTo("Aniversario Ana");
        assertThat(result.sizeGb()).isEqualTo(20);
        assertThat(result.deliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        // CreateCustomerDto.toEntity() nao seta id em Customer novo, entao customerId fica null aqui
        assertThat(result.customerId()).isNull();
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
    void deleteEvent_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.deleteEvent(id))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(eventRepository, never()).delete(any());
    }

    @Test
    void searchEvents_appliesCombinedFiltersOverAllEvents() {
        Event other = Event.builder()
                .id(UUID.randomUUID())
                .name("Aniversario 15 anos")
                .type(EventType.BIRTHDAY)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build();
        when(eventRepository.findAll()).thenReturn(List.of(event, other));

        List<EventDto> result = eventService.searchEvents(
                EventType.WEDDING, null, null, null, null, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(event.getId());
    }

    @Test
    void searchEvents_noFiltersReturnsAllEvents() {
        Event other = Event.builder().id(UUID.randomUUID()).name("Aniversario 15 anos").build();
        when(eventRepository.findAll()).thenReturn(List.of(event, other));

        List<EventDto> result = eventService.searchEvents(null, null, null, null, null, null, null, null, null);

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

        List<EventDto> result = eventService.searchEvents(
                null, null, null, null, null, null, null, "maria", null);

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

        List<EventDto> result = eventService.searchEvents(
                null, null, null, null, null, null, null, null, "evt-001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(event.getId());
    }
}
