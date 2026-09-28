package com.renatoganske.gestao_de_eventos.dtos;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * Write-side counterpart of {@link EventProfessionalSummaryDto}: carries only what the client
 * decides (who worked on the event, in which role). The event side of the association comes
 * from the aggregate being saved, so it is not part of the payload.
 */
public record EventProfessionalAssignmentDto(
        @NotNull UUID professionalId,
        String roleInEvent
) implements Serializable {
}
