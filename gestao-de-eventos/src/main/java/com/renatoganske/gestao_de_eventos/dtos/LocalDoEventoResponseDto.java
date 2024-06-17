package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Evento;
import com.renatoganske.gestao_de_eventos.entities.LocalDoEvento;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link LocalDoEvento}
 */
public record LocalDoEventoResponseDto(
        UUID id,
        String nome,
        String endereco,
        String cidade,
        String estado,
        String tipo,
        List<Evento> eventos
) implements Serializable {
}