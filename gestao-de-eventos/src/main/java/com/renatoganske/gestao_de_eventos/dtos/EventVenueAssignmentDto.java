package com.renatoganske.gestao_de_eventos.dtos;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * Write-side counterpart of {@link EventVenueSummaryDto}: which venue is linked to the event and,
 * optionally, what it is for ("Cerimônia", "Recepção", "Making-of"...).
 */
public record EventVenueAssignmentDto(
        @NotNull UUID venueId,
        String role
) implements Serializable {
}
