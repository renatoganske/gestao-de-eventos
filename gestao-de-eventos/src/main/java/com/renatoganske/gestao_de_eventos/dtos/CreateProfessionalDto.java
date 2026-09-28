package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Professional;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Professional}
 */
public record CreateProfessionalDto(
        @NotBlank String name,
        UUID typeId,
        String contact,
        List<UUID> specialtyTagIds,
        String otherInfo
) implements Serializable {
    public Professional toEntity() {
        return Professional.builder()
                .name(this.name())
                .contact(this.contact())
                .otherInfo(this.otherInfo())
                .build();
    }
}
