package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Cliente;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link com.renatoganske.gestao_de_eventos.entities.Cliente}
 */
public record ClienteRequestDto(
        String nome,
        String contato,
        String endereco,
        String observacoes
) implements Serializable {
    public Cliente toEntity() {
        return Cliente.builder()
                .nome(this.nome)
                .contato(this.contato)
                .endereco(this.endereco)
                .observacoes(this.observacoes)
                .build();
    }
}