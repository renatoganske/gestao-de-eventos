package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.ProfessionalType;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link ProfessionalType}
 */
public record ProfessionalTypeDto(
        UUID id,
        String name
) implements Serializable {
}
