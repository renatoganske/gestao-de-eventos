package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Customer;

import java.io.Serializable;
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
        int eventCount
) implements Serializable {
}
