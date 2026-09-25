package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueDto;
import com.renatoganske.gestao_de_eventos.entities.EventVenue;
import com.renatoganske.gestao_de_eventos.exceptions.EventVenueNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.EventVenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class EventVenueServiceTest {

    @Mock
    private EventVenueRepository eventVenueRepository;

    @InjectMocks
    private EventVenueService eventVenueService;

    private EventVenue eventVenue;
    private CreateEventVenueDto createEventVenueDto;

    @BeforeEach
    void setUp() {
        eventVenue = EventVenue.builder()
                .id(UUID.randomUUID())
                .name("Buffet Jardim das Rosas")
                .address("Av. Central, 1000")
                .city("Curitiba")
                .state("PR")
                .type("Buffet")
                .build();

        createEventVenueDto = new CreateEventVenueDto(
                eventVenue.getName(),
                eventVenue.getAddress(),
                eventVenue.getCity(),
                eventVenue.getState(),
                eventVenue.getType());
    }

    @Test
    void createEventVenue_savesAndReturnsResponseDto() {
        ArgumentCaptor<EventVenue> captor = ArgumentCaptor.forClass(EventVenue.class);
        when(eventVenueRepository.save(captor.capture())).thenReturn(eventVenue);

        EventVenueDto result = eventVenueService.createEventVenue(createEventVenueDto);

        assertThat(captor.getValue().getName()).isEqualTo(createEventVenueDto.name());
        assertThat(captor.getValue().getAddress()).isEqualTo(createEventVenueDto.address());
        assertThat(captor.getValue().getCity()).isEqualTo(createEventVenueDto.city());
        assertThat(captor.getValue().getState()).isEqualTo(createEventVenueDto.state());
        assertThat(captor.getValue().getType()).isEqualTo(createEventVenueDto.type());

        assertThat(result.id()).isEqualTo(eventVenue.getId());
        assertThat(result.name()).isEqualTo(eventVenue.getName());
        assertThat(result.address()).isEqualTo(eventVenue.getAddress());
        assertThat(result.city()).isEqualTo(eventVenue.getCity());
        assertThat(result.state()).isEqualTo(eventVenue.getState());
        assertThat(result.type()).isEqualTo(eventVenue.getType());
    }

    @Test
    void getAllEventVenues_returnsAllMappedEventVenues() {
        EventVenue other = EventVenue.builder()
                .id(UUID.randomUUID())
                .name("Sitio Recanto Verde")
                .build();
        when(eventVenueRepository.findAll()).thenReturn(List.of(eventVenue, other));

        List<EventVenueDto> result = eventVenueService.getAllEventVenues();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EventVenueDto::name)
                .containsExactly(eventVenue.getName(), other.getName());
    }

    @Test
    void getAllEventVenues_returnsEmptyListWhenNoEventVenues() {
        when(eventVenueRepository.findAll()).thenReturn(List.of());

        List<EventVenueDto> result = eventVenueService.getAllEventVenues();

        assertThat(result).isEmpty();
    }

    @Test
    void getEventVenueById_returnsEventVenueWhenFound() {
        when(eventVenueRepository.findById(eventVenue.getId())).thenReturn(Optional.of(eventVenue));

        EventVenueDto result = eventVenueService.getEventVenueById(eventVenue.getId());

        assertThat(result.id()).isEqualTo(eventVenue.getId());
        assertThat(result.name()).isEqualTo(eventVenue.getName());
    }

    @Test
    void getEventVenueById_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(eventVenueRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventVenueService.getEventVenueById(id))
                .isInstanceOf(EventVenueNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateEventVenue_updatesAndReturnsEventVenueWhenFound() {
        UUID id = eventVenue.getId();
        CreateEventVenueDto updateDto = new CreateEventVenueDto(
                "Salao Villa Bella", "Rua Nova, 456", "Sao Jose dos Pinhais", "PR", "Salao");
        when(eventVenueRepository.findById(id)).thenReturn(Optional.of(eventVenue));
        when(eventVenueRepository.save(any(EventVenue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventVenueDto result = eventVenueService.updateEventVenue(id, updateDto);

        assertThat(result.name()).isEqualTo("Salao Villa Bella");
        assertThat(result.address()).isEqualTo("Rua Nova, 456");
        assertThat(result.city()).isEqualTo("Sao Jose dos Pinhais");
        assertThat(result.state()).isEqualTo("PR");
        assertThat(result.type()).isEqualTo("Salao");
    }

    @Test
    void updateEventVenue_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(eventVenueRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventVenueService.updateEventVenue(id, createEventVenueDto))
                .isInstanceOf(EventVenueNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(eventVenueRepository, never()).save(any());
    }

    @Test
    void deleteEventVenue_deletesWhenFound() {
        UUID id = eventVenue.getId();
        when(eventVenueRepository.findById(id)).thenReturn(Optional.of(eventVenue));

        eventVenueService.deleteEventVenue(id);

        verify(eventVenueRepository, times(1)).delete(eventVenue);
    }

    @Test
    void deleteEventVenue_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(eventVenueRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventVenueService.deleteEventVenue(id))
                .isInstanceOf(EventVenueNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(eventVenueRepository, never()).delete(any());
    }
}
