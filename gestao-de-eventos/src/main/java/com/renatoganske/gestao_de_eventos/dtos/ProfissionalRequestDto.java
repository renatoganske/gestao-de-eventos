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
    private Profissional toEntity() {
        return Profissional.builder()
                .nome(this.nome())
                .tipo(this.tipo())
                .contato(this.contato())
                .especialidade(this.especialidade())
                .outrasInformacoes(this.outrasInformacoes())
                .build();
    }
}