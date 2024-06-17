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
    public Cliente toEntity(ClienteRequestDto requestDto) {
        return Cliente.builder()
                .nome(requestDto.nome())
                .contato(requestDto.contato())
                .endereco(requestDto.endereco())
                .observacoes(requestDto.observacoes())
                .build();
    }
}