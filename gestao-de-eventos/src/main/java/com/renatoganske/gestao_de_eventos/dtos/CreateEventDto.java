package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Event}
 */
public record CreateEventDto(
        String eventCode,
        UUID eventTypeId,
        @NotBlank String name,
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
        @Valid List<EventProfessionalAssignmentDto> professionals
) implements Serializable {
    public Event toEntity() {
        return Event.builder()
                .eventCode(this.eventCode())
                .name(this.name())
                .eventDate(this.eventDate())
                .daytimeWedding(this.daytimeWedding())
                .outdoorWedding(this.outdoorWedding())
                .guestCount(this.guestCount())
                .description(this.description())
                .amount(this.amount())
                .sizeGb(this.sizeGb())
                .deliveryStatus(this.deliveryStatus())
                .build();
    }
}
