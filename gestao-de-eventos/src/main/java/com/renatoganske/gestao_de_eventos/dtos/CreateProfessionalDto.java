package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Professional;

import java.io.Serializable;

/**
 * DTO for {@link Professional}
 */
public record CreateProfessionalDto(
        String name,
        String type,
        String contact,
        String specialty,
        String otherInfo
) implements Serializable {
    public Professional toEntity() {
        return Professional.builder()
                .name(this.name())
                .type(this.type())
                .contact(this.contact())
                .specialty(this.specialty())
                .otherInfo(this.otherInfo())
                .build();
    }
}
