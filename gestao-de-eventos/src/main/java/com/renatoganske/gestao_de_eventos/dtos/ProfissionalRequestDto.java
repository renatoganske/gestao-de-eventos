package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Profissional;

import java.io.Serializable;

/**
 * DTO for {@link Profissional}
 */
public record ProfissionalRequestDto(
        String nome,
        String tipo,
        String contato,
        String especialidade,
        String outrasInformacoes
) implements Serializable {
    private Profissional toEntity(ProfissionalRequestDto requestDto) {
        return Profissional.builder()
                .nome(requestDto.nome())
                .tipo(requestDto.tipo())
                .contato(requestDto.contato())
                .especialidade(requestDto.especialidade())
                .outrasInformacoes(requestDto.outrasInformacoes())
                .build();
    }
}