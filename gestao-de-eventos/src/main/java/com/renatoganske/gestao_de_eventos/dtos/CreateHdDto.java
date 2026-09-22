package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Hd;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for {@link com.renatoganske.gestao_de_eventos.entities.Hd}
 */
public record CreateHdDto(
        String nome,
        Integer capacidade,
        LocalDate dataAquisicao,
        String status
) implements Serializable {
    public Hd toEntity() {
        return Hd.builder()
                .nome(this.nome())
                .capacidade(this.capacidade())
                .dataAquisicao(this.dataAquisicao())
                .status(this.status())
                .build();
    }
}