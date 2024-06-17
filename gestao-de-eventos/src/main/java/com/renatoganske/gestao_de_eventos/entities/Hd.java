package com.renatoganske.gestao_de_eventos.entities;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity(name = "TB_HD")
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
    private Enum status;

    @OneToMany(mappedBy = "hd")
    private List<Evento> eventos;
}
