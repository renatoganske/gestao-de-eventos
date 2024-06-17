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
    public Evento toEntity(EventoRequestDto requestDto) {
        return Evento.builder()
                .codigoDoEvento(requestDto.codigoDoEvento())
                .tipo(requestDto.tipo())
                .nome(requestDto.nome())
                .dataDoEvento(requestDto.dataDoEvento())
                .casamentoDeDia(requestDto.casamentoDeDia())
                .casamentoExterno(requestDto.casamentoExterno())
                .quantidadeDeConvidados(requestDto.quantidadeDeConvidados())
                .descricao(requestDto.descricao())
                .valor(requestDto.valor())
                .hd(requestDto.hd())
                .localDoEvento(requestDto.localDoEvento())
                .build();
    }
}