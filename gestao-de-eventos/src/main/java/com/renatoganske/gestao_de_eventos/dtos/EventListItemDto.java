package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Row of the paged event search (ADR-0029): only what the event list shows, instead of the whole
 * {@link EventDto}. The team is reduced to its size, which the delete confirmation mentions.
 */
public record EventListItemDto(
        UUID id,
        String eventCode,
        EventTypeDto type,
        String name,
        LocalDate eventDate,
        DeliveryStatus deliveryStatus,
        UUID hdId,
        String hdName,
        // sorted by venue name, as in Event.toDTO()
        List<EventVenueSummaryDto> venues,
        // contracting customer first, as in Event.toDTO()
        List<EventCustomerSummaryDto> customers,
        int professionalCount
) implements Serializable {
}
