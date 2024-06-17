package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Evento;
import com.renatoganske.gestao_de_eventos.entities.Profissional;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Profissional}
 */
public record ProfissionalResponseDto(
        UUID id,
        String nome,
        String tipo,
        String contato,
        String especialidade,
        String outrasInformacoes,
        List<Evento> eventos
) implements Serializable {
}