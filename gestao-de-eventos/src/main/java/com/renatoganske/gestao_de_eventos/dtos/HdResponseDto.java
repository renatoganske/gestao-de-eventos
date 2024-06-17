package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Evento;
import com.renatoganske.gestao_de_eventos.entities.Hd;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Hd}
 */
public record HdResponseDto(
        UUID id, String nome,
        Integer capacidade,
        LocalDate dataAquisicao,
        String status,
        List<Evento> eventos
) implements Serializable {
}