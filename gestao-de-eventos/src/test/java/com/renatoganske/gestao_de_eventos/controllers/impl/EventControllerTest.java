package com.renatoganske.gestao_de_eventos.controllers.impl;

import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.filters.EventSearchCriteria;
import com.renatoganske.gestao_de_eventos.services.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    @Test
    void search_passesEveryRequestParameterToTheServiceInsideTheCriteria() {
        UUID eventTypeId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID hdId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<EventDto> expected = List.of();
        when(eventService.searchEvents(org.mockito.ArgumentMatchers.any(EventSearchCriteria.class)))
                .thenReturn(expected);

        ResponseEntity<List<EventDto>> response = eventController.search(
                eventTypeId, venueId, professionalId, from, to, hdId, DeliveryStatus.PENDING,
                "maria", "EVT-001", true, false);

        ArgumentCaptor<EventSearchCriteria> captor = ArgumentCaptor.forClass(EventSearchCriteria.class);
        verify(eventService).searchEvents(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new EventSearchCriteria(
                eventTypeId, venueId, professionalId, from, to, hdId, DeliveryStatus.PENDING,
                "maria", "EVT-001", true, false));
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void search_keepsTheWeddingFlagsUnsetWhenTheyAreNotProvided() {
        when(eventService.searchEvents(org.mockito.ArgumentMatchers.any(EventSearchCriteria.class)))
                .thenReturn(List.of());

        eventController.search(null, null, null, null, null, null, null, null, null, null, null);

        ArgumentCaptor<EventSearchCriteria> captor = ArgumentCaptor.forClass(EventSearchCriteria.class);
        verify(eventService).searchEvents(captor.capture());
        assertThat(captor.getValue()).isEqualTo(EventSearchCriteria.none());
    }
}
