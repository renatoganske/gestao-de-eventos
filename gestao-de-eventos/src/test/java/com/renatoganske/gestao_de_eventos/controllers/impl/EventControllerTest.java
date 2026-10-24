package com.renatoganske.gestao_de_eventos.controllers.impl;

import com.renatoganske.gestao_de_eventos.dtos.EventListItemDto;
import com.renatoganske.gestao_de_eventos.dtos.PageDto;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.filters.EventSearchCriteria;
import com.renatoganske.gestao_de_eventos.filters.EventSort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
        UUID customerId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        PageDto<EventListItemDto> expected = new PageDto<>(List.of(), 2, 25, 0, 0);
        when(eventService.searchEvents(any(EventSearchCriteria.class), anyInt(), anyInt(), any(EventSort.class)))
                .thenReturn(expected);

        ResponseEntity<PageDto<EventListItemDto>> response = eventController.search(
                eventTypeId, venueId, professionalId, from, to, hdId, DeliveryStatus.PENDING,
                customerId, "maria", "EVT-001", true, false, "silva", 2, 25, "customer,desc");

        ArgumentCaptor<EventSearchCriteria> captor = ArgumentCaptor.forClass(EventSearchCriteria.class);
        verify(eventService).searchEvents(captor.capture(), eq(2), eq(25),
                eq(new EventSort(EventSort.Key.CUSTOMER, false)));
        assertThat(captor.getValue()).isEqualTo(new EventSearchCriteria(
                eventTypeId, venueId, professionalId, from, to, hdId, DeliveryStatus.PENDING,
                customerId, "maria", "EVT-001", true, false, "silva"));
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void search_keepsTheWeddingFlagsUnsetWhenTheyAreNotProvided() {
        when(eventService.searchEvents(any(EventSearchCriteria.class), anyInt(), anyInt(), any(EventSort.class)))
                .thenReturn(new PageDto<>(List.of(), 0, 50, 0, 0));

        eventController.search(null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 50, null);

        ArgumentCaptor<EventSearchCriteria> captor = ArgumentCaptor.forClass(EventSearchCriteria.class);
        verify(eventService).searchEvents(captor.capture(), eq(0), eq(50), eq(EventSort.DEFAULT));
        assertThat(captor.getValue()).isEqualTo(EventSearchCriteria.none());
    }
}
