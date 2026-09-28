package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.SpecialtyTag;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link SpecialtyTag}
 */
public record SpecialtyTagDto(
        UUID id,
        String name
) implements Serializable {
}
