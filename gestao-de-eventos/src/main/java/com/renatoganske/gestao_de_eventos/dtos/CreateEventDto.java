package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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
        @Valid List<EventCustomerAssignmentDto> customers,
        @Valid List<EventProfessionalAssignmentDto> professionals
) implements Serializable {
    /**
     * An event has at most one contracting customer (ADR-0025); the database enforces it too, but
     * rejecting it here turns the mistake into a 400 with a message instead of a 409.
     */
    @JsonIgnore
    @AssertTrue(message = "at most one customer can be marked as contracting")
    public boolean isAtMostOneContractingCustomer() {
        return this.customers == null
                || this.customers.stream()
                        .filter(c -> c != null && c.isContracting())
                        .map(EventCustomerAssignmentDto::customerId)
                        .distinct()
                        .count() <= 1;
    }

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
