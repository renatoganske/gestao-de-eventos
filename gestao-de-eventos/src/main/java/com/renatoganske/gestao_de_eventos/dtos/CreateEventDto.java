package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.entities.EventVenue;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for {@link Event}
 */
public record CreateEventDto(
        String codigoDoEvento,
        String tipo,
        String nome,
        LocalDate dataDoEvento,
        Boolean casamentoDeDia,
        Boolean casamentoExterno,
        Long quantidadeDeConvidados,
        String descricao,
        Double valor,
        Hd hd,
        EventVenue eventVenue,
        CreateCustomerDto cliente
) implements Serializable {
    public Event toEntity() {
        return Event.builder()
                .codigoDoEvento(this.codigoDoEvento())
                .tipo(this.tipo())
                .nome(this.nome())
                .dataDoEvento(this.dataDoEvento())
                .casamentoDeDia(this.casamentoDeDia())
                .casamentoExterno(this.casamentoExterno())
                .quantidadeDeConvidados(this.quantidadeDeConvidados())
                .descricao(this.descricao())
                .valor(this.valor())
                .hd(this.hd())
                .eventVenue(this.eventVenue())
                .build();
    }
}