package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Evento;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.entities.LocalDoEvento;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for {@link Evento}
 */
public record EventoRequestDto(
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
        LocalDoEvento localDoEvento,
        ClienteRequestDto cliente
) implements Serializable {
    public Evento toEntity() {
        return Evento.builder()
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
                .localDoEvento(this.localDoEvento())
                .build();
    }
}