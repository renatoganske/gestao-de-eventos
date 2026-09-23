package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.EventProfessional;

import java.io.Serializable;
import java.util.UUID;

/**
 * Lightweight, relationship-free projection of {@link EventProfessional}, used to embed the
 * event-professional association inside other response DTOs (Event, Professional) without
 * exposing the JPA entity graph (see ADR-0011).
 */
public record EventProfessionalSummaryDto(
        UUID eventId,
        String eventCode,
        String eventName,
        UUID professionalId,
        String professionalName,
        String roleInEvent
) implements Serializable {
}
