package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Event}
 */
public record EventDto(
        UUID id,
        String codigoDoEvento,
        String tipo, String nome,
        LocalDate dataDoEvento,
        Boolean casamentoDeDia,
        Boolean casamentoExterno,
        Long quantidadeDeConvidados,
        String descricao,
        Double valor, Hd hd,
        EventVenue eventVenue,
        Customer customer,
        List<Professional> profissionais
) implements Serializable {
}