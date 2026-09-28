package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.ProfessionalType;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * DTO for {@link ProfessionalType}
 */
public record CreateProfessionalTypeDto(
        @NotBlank String name
) implements Serializable {
    public ProfessionalType toEntity() {
        return ProfessionalType.builder()
                .name(this.name())
                .build();
    }
}
