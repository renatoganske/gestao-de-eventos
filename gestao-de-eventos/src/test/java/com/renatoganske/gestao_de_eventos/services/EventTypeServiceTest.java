package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.EventTypeDto;
import com.renatoganske.gestao_de_eventos.entities.EventType;
import com.renatoganske.gestao_de_eventos.exceptions.EventTypeNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.ResourceInUseException;
import com.renatoganske.gestao_de_eventos.repositories.EventRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventTypeRepository;
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
class EventTypeServiceTest {

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventTypeService eventTypeService;

    private EventType eventType;
    private CreateEventTypeDto createEventTypeDto;

    @BeforeEach
    void setUp() {
        eventType = EventType.builder()
                .id(UUID.randomUUID())
                .name("WEDDING")
                .build();

        createEventTypeDto = new CreateEventTypeDto(eventType.getName(), null);
    }

    @Test
    void createEventType_savesAndReturnsResponseDto() {
        ArgumentCaptor<EventType> captor = ArgumentCaptor.forClass(EventType.class);
        when(eventTypeRepository.save(captor.capture())).thenReturn(eventType);

        EventTypeDto result = eventTypeService.createEventType(createEventTypeDto);

        assertThat(captor.getValue().getName()).isEqualTo(createEventTypeDto.name());

        assertThat(result.id()).isEqualTo(eventType.getId());
        assertThat(result.name()).isEqualTo(eventType.getName());
    }

    @Test
    void getAllEventTypes_returnsAllMappedEventTypes() {
        EventType other = EventType.builder().id(UUID.randomUUID()).name("BATIZADO").build();
        when(eventTypeRepository.findAll()).thenReturn(List.of(eventType, other));

        List<EventTypeDto> result = eventTypeService.getAllEventTypes();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EventTypeDto::name)
                .containsExactly(eventType.getName(), other.getName());
    }

    @Test
    void getAllEventTypes_returnsEmptyListWhenNoEventTypes() {
        when(eventTypeRepository.findAll()).thenReturn(List.of());

        List<EventTypeDto> result = eventTypeService.getAllEventTypes();

        assertThat(result).isEmpty();
    }

    @Test
    void getEventTypeById_returnsEventTypeWhenFound() {
        when(eventTypeRepository.findById(eventType.getId())).thenReturn(Optional.of(eventType));

        EventTypeDto result = eventTypeService.getEventTypeById(eventType.getId());

        assertThat(result.id()).isEqualTo(eventType.getId());
        assertThat(result.name()).isEqualTo(eventType.getName());
    }

    @Test
    void getEventTypeById_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(eventTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventTypeService.getEventTypeById(id))
                .isInstanceOf(EventTypeNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateEventType_updatesAndReturnsEventTypeWhenFound() {
        UUID id = eventType.getId();
        CreateEventTypeDto updateDto = new CreateEventTypeDto("BATIZADO", null);
        when(eventTypeRepository.findById(id)).thenReturn(Optional.of(eventType));
        when(eventTypeRepository.save(any(EventType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventTypeDto result = eventTypeService.updateEventType(id, updateDto);

        assertThat(result.name()).isEqualTo("BATIZADO");
    }

    @Test
    void createEventType_defaultsHasWeddingFieldsToFalseWhenOmitted() {
        ArgumentCaptor<EventType> captor = ArgumentCaptor.forClass(EventType.class);
        when(eventTypeRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        EventTypeDto result = eventTypeService.createEventType(new CreateEventTypeDto("Ensaio", null));

        assertThat(captor.getValue().isHasWeddingFields()).isFalse();
        assertThat(result.hasWeddingFields()).isFalse();
    }

    @Test
    void createEventType_savesHasWeddingFieldsWhenTrue() {
        ArgumentCaptor<EventType> captor = ArgumentCaptor.forClass(EventType.class);
        when(eventTypeRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        EventTypeDto result = eventTypeService.createEventType(new CreateEventTypeDto("Mini Wedding", true));

        assertThat(captor.getValue().isHasWeddingFields()).isTrue();
        assertThat(result.hasWeddingFields()).isTrue();
    }

    @Test
    void updateEventType_setsHasWeddingFieldsWhenProvided() {
        UUID id = eventType.getId();
        when(eventTypeRepository.findById(id)).thenReturn(Optional.of(eventType));
        when(eventTypeRepository.save(any(EventType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventTypeDto marked = eventTypeService.updateEventType(id, new CreateEventTypeDto("WEDDING", true));
        assertThat(marked.hasWeddingFields()).isTrue();

        EventTypeDto unmarked = eventTypeService.updateEventType(id, new CreateEventTypeDto("WEDDING", false));
        assertThat(unmarked.hasWeddingFields()).isFalse();
    }

    @Test
    void updateEventType_keepsCurrentHasWeddingFieldsWhenOmitted() {
        UUID id = eventType.getId();
        eventType.setHasWeddingFields(true);
        when(eventTypeRepository.findById(id)).thenReturn(Optional.of(eventType));
        when(eventTypeRepository.save(any(EventType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventTypeDto renamed = eventTypeService.updateEventType(id, new CreateEventTypeDto("Casamento", null));

        assertThat(renamed.name()).isEqualTo("Casamento");
        assertThat(renamed.hasWeddingFields()).isTrue();
    }

    @Test
    void updateEventType_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(eventTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventTypeService.updateEventType(id, createEventTypeDto))
                .isInstanceOf(EventTypeNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(eventTypeRepository, never()).save(any());
    }

    @Test
    void deleteEventType_deletesWhenFoundAndUnused() {
        UUID id = eventType.getId();
        when(eventTypeRepository.findById(id)).thenReturn(Optional.of(eventType));
        when(eventRepository.countByType_Id(id)).thenReturn(0L);

        eventTypeService.deleteEventType(id);

        verify(eventTypeRepository, times(1)).delete(eventType);
    }

    @Test
    void deleteEventType_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(eventTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventTypeService.deleteEventType(id))
                .isInstanceOf(EventTypeNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(eventTypeRepository, never()).delete(any());
    }

    @Test
    void deleteEventType_throwsResourceInUseExceptionWhenUsedByEvents() {
        UUID id = eventType.getId();
        when(eventTypeRepository.findById(id)).thenReturn(Optional.of(eventType));
        when(eventRepository.countByType_Id(id)).thenReturn(3L);

        assertThatThrownBy(() -> eventTypeService.deleteEventType(id))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("3");

        verify(eventTypeRepository, never()).delete(any());
    }
}
