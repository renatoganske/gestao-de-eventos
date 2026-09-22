package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Hd;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for {@link com.renatoganske.gestao_de_eventos.entities.Hd}
 */
public record CreateHdDto(
        String name,
        Integer capacidade,
        LocalDate acquisitionDate,
        String status
) implements Serializable {
    public Hd toEntity() {
        return Hd.builder()
                .name(this.name())
                .capacidade(this.capacidade())
                .acquisitionDate(this.acquisitionDate())
                .status(this.status())
                .build();
    }
}
