package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.EventCustomer;

import java.io.Serializable;
import java.util.UUID;

/**
 * Relationship-free projection of {@link EventCustomer}, embedded in {@link EventDto} without
 * exposing the JPA entity graph (see ADR-0011).
 */
public record EventCustomerSummaryDto(
        UUID customerId,
        String customerName,
        boolean contracting
) implements Serializable {
}
