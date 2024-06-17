package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.HdResponseDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity(name = "TB_HD")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Hd {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;
    @Column(name = "capacidade")
    private Integer capacidade;
    @Column(name = "data_aquisicao")
    private LocalDate dataAquisicao;
    @Column(name = "status")
    private String status;

    @OneToMany(mappedBy = "hd")
    private List<Evento> eventos;

    public HdResponseDto toResponseDto() {
        return new HdResponseDto(
                this.id,
                this.nome,
                this.capacidade,
                this.dataAquisicao,
                this.status,
                this.eventos
        );
    }
}
