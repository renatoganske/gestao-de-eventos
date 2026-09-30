package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.EventType;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link EventType}
 */
public record EventTypeDto(
        UUID id,
        String name,
        boolean hasWeddingFields
) implements Serializable {
}
