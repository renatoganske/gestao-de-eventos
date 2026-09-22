package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Customer;
import com.renatoganske.gestao_de_eventos.entities.Event;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Customer}
 */
public record CustomerResponseDto(
        UUID id,
        String name,
        String contact,
        String address,
        String notes,
        List<Event> events
) implements Serializable {
}
