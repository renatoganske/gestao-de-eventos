package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lightweight, relationship-free projection of {@link Event}, used to embed events inside other
 * response DTOs (Customer, EventVenue, Hd) without exposing the JPA entity graph (see ADR-0011).
 */
public record EventSummaryDto(
        UUID id,
        String eventCode,
        EventTypeDto type,
        String name,
        LocalDate eventDate,
        DeliveryStatus deliveryStatus
) implements Serializable {
}
