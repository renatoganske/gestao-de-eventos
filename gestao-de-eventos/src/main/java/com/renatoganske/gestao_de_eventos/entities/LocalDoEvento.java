package com.renatoganske.gestao_de_eventos.entities;

import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity(name = "TB_LOCAL_DO_EVENTO")
public class LocalDoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;
    @Column(name = "endereco")
    private String endereco;
    @Column(name = "cidade")
    private String cidade;
    @Column(name = "estado")
    private String estado;
    @Column(name = "tipo")
    private String tipo;

    @OneToMany(mappedBy = "localDoEvento")
    private List<Evento> eventos;
}
