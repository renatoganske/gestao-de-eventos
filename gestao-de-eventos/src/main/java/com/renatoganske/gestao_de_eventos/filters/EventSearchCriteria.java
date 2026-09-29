package com.renatoganske.gestao_de_eventos.filters;

import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * All optional criteria accepted by the event search. A {@code null} field means "do not filter by it".
 * Grouping them in a record avoids a long positional parameter list, where same-typed arguments
 * (several UUIDs, two Booleans) could be swapped without the compiler noticing.
 */
public record EventSearchCriteria(
        UUID eventTypeId,
        UUID venueId,
        UUID professionalId,
        LocalDate from,
        LocalDate to,
        UUID hdId,
        DeliveryStatus deliveryStatus,
        String customerName,
        String eventCode,
        Boolean daytimeWedding,
        Boolean outdoorWedding) {

    public static EventSearchCriteria none() {
        return new EventSearchCriteria(null, null, null, null, null, null, null, null, null, null, null);
    }
}
