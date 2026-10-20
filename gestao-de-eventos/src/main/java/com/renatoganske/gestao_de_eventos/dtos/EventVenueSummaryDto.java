package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.EventEventVenue;

import java.io.Serializable;
import java.util.UUID;

/**
 * Relationship-free projection of {@link EventEventVenue}, embedded in {@link EventDto} without
 * exposing the JPA entity graph (see ADR-0011).
 */
public record EventVenueSummaryDto(
        UUID venueId,
        String venueName,
        String role
) implements Serializable {
}
