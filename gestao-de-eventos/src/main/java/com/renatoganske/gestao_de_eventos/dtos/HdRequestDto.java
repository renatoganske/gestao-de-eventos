package com.renatoganske.gestao_de_eventos.dtos;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for {@link com.renatoganske.gestao_de_eventos.entities.Hd}
 */
public record HdRequestDto(
        String nome,
        Integer capacidade,
        LocalDate dataAquisicao,
        String status
) implements Serializable {
}