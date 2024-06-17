package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Hd;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for {@link com.renatoganske.gestao_de_eventos.entities.Hd}
 */
public record HdRequestDto(
        String nome,
        Integer capacidade,
        LocalDate dataAquisicao,
        String status
) implements Serializable {
    public Hd toEntity(HdRequestDto requestDto) {
        return Hd.builder()
                .nome(requestDto.nome())
                .capacidade(requestDto.capacidade())
                .dataAquisicao(requestDto.dataAquisicao())
                .status(requestDto.status())
                .build();
    }
}