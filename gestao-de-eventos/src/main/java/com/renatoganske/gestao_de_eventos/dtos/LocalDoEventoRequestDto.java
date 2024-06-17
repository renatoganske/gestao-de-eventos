package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.LocalDoEvento;

import java.io.Serializable;

/**
 * DTO for {@link LocalDoEvento}
 */
public record LocalDoEventoRequestDto(
        String nome,
        String endereco,
        String cidade,
        String estado,
        String tipo
) implements Serializable {
}