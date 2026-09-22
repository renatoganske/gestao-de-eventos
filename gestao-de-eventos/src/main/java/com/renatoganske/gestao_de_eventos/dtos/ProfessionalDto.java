package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.entities.Professional;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Professional}
 */
public record ProfessionalDto(
        UUID id,
        String nome,
        String tipo,
        String contato,
        String especialidade,
        String outrasInformacoes,
        List<Event> events
) implements Serializable {
}