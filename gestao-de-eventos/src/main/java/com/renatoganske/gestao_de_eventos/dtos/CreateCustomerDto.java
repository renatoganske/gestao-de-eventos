package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Customer;

import java.io.Serializable;

/**
 * DTO for {@link Customer}
 */
public record CreateCustomerDto(
        String name,
        String contact,
        String address,
        String notes
) implements Serializable {
    public Customer toEntity() {
        return Customer.builder()
                .name(this.name)
                .contact(this.contact)
                .address(this.address)
                .notes(this.notes)
                .build();
    }
}
