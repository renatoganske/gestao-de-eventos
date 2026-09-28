package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.SpecialtyTag;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * DTO for {@link SpecialtyTag}
 */
public record CreateSpecialtyTagDto(
        @NotBlank String name
) implements Serializable {
    public SpecialtyTag toEntity() {
        return SpecialtyTag.builder()
                .name(this.name())
                .build();
    }
}
