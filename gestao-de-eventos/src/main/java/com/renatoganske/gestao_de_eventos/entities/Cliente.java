package com.renatoganske.gestao_de_eventos.entities;

import jakarta.persistence.*;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.List;
import java.util.UUID;

@Entity(name = "TB_CLIENTE")
public class Cliente {
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

    @OneToMany(mappedBy = "cliente")
    private List<Evento> eventos;
}
