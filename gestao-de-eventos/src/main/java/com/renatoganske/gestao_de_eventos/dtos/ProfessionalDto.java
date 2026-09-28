package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Professional;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Professional}
 */
public record ProfessionalDto(
        UUID id,
        String name,
        ProfessionalTypeDto type,
        String contact,
        List<SpecialtyTagDto> specialtyTags,
        String otherInfo,
        List<EventProfessionalSummaryDto> eventProfessionals
) implements Serializable {
}
