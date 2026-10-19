package com.renatoganske.gestao_de_eventos.dtos;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * Write-side counterpart of {@link EventCustomerSummaryDto}: which customer is linked to the event
 * and whether they are the one who hired it. {@code contracting} omitted means {@code false}.
 */
public record EventCustomerAssignmentDto(
        @NotNull UUID customerId,
        Boolean contracting
) implements Serializable {

    public boolean isContracting() {
        return Boolean.TRUE.equals(this.contracting);
    }
}
