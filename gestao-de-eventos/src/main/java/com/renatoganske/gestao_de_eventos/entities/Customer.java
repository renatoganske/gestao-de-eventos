package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.CustomerResponseDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity(name = "TB_CUSTOMER")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "contact")
    private String contact;
    @Column(name = "address")
    private String address;
    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "customer")
    private List<Event> events;

    public CustomerResponseDto toResponseDto() {
        return new CustomerResponseDto(
                this.id,
                this.name,
                this.contact,
                this.address,
                this.notes,
                this.events == null ? List.of() : this.events.stream()
                        .map(Event::toSummaryDto)
                        .collect(Collectors.toList()));
    }
}
