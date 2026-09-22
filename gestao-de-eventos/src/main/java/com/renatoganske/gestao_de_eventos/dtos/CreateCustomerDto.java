package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Customer;

import java.io.Serializable;

/**
 * DTO for {@link Customer}
 */
public record CreateCustomerDto(
        String nome,
        String contato,
        String endereco,
        String observacoes
) implements Serializable {
    public Customer toEntity() {
        return Customer.builder()
                .nome(this.nome)
                .contato(this.contato)
                .endereco(this.endereco)
                .observacoes(this.observacoes)
                .build();
    }
}