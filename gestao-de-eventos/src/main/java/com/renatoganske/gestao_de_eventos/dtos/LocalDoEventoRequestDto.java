package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.LocalDoEvento;

import java.io.Serializable;

/**
 * DTO for {@link LocalDoEvento}
 */
public record LocalDoEventoRequestDto(
        String nome,
        String endereco,
        String cidade,
        String estado,
        String tipo
) implements Serializable {
    public LocalDoEvento toEntity() {
        return LocalDoEvento.builder()
                .nome(this.nome())
                .endereco(this.endereco())
                .cidade(this.cidade())
                .estado(this.estado())
                .tipo(this.tipo())
                .build();
    }
}