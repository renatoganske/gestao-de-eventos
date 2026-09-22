package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.ClienteResponseDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity(name = "TB_CLIENTE")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;
    @Column(name = "contato")
    private String contato;
    @Column(name = "endereco")
    private String endereco;
    @Column(name = "observacoes")
    private String observacoes;

    @OneToMany(mappedBy = "customer")
    private List<Event> events;

    public ClienteResponseDto toResponseDto() {
        return new ClienteResponseDto(
                this.id,
                this.nome,
                this.contato,
                this.endereco,
                this.observacoes,
                this.events);
    }
}
