package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Professional;

import java.io.Serializable;

/**
 * DTO for {@link Professional}
 */
public record CreateProfessionalDto(
        String nome,
        String tipo,
        String contato,
        String especialidade,
        String outrasInformacoes
) implements Serializable {
    private Professional toEntity() {
        return Professional.builder()
                .nome(this.nome())
                .tipo(this.tipo())
                .contato(this.contato())
                .especialidade(this.especialidade())
                .outrasInformacoes(this.outrasInformacoes())
                .build();
    }
}