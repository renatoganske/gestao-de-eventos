package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Evento}
 */
public record EventoResponseDto(
        UUID id,
        String codigoDoEvento,
        String tipo, String nome,
        LocalDate dataDoEvento,
        Boolean casamentoDeDia,
        Boolean casamentoExterno,
        Long quantidadeDeConvidados,
        String descricao,
        Double valor, Hd hd,
        LocalDoEvento localDoEvento,
        Cliente cliente,
        List<Profissional> profissionais
) implements Serializable {
}