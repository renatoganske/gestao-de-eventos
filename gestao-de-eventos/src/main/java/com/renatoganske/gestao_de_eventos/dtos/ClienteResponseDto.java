package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Cliente;
import com.renatoganske.gestao_de_eventos.entities.Evento;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Cliente}
 */
public record ClienteResponseDto(
        UUID id,
        String nome,
        String contato,
        String endereco,
        String observacoes,
        List<Evento> eventos
) implements Serializable {
}