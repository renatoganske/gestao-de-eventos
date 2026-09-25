package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Event}
 */
public record EventDto(
        UUID id,
        String eventCode,
        EventTypeDto type, String name,
        LocalDate eventDate,
        Boolean daytimeWedding,
        Boolean outdoorWedding,
        Long guestCount,
        String description,
        Double amount,
        Integer sizeGb,
        DeliveryStatus deliveryStatus,
        UUID hdId,
        UUID eventVenueId,
        UUID customerId,
        List<EventProfessionalSummaryDto> eventProfessionals
) implements Serializable {
}
