package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.ProfissionalResponseDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity(name = "TB_PROFISSIONAL")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;
    @Column(name = "tipo")
    private String tipo;
    @Column(name = "contato")
    private String contato;
    @Column(name = "especialidade")
    private String especialidade;
    @Column(name = "outras_informacoes")
    private String outrasInformacoes;

    @ManyToMany(mappedBy = "profissionais")
    private List<Evento> eventos;

    public ProfissionalResponseDto toResponseDto(Profissional profissional) {
        return new ProfissionalResponseDto(
                profissional.id,
                profissional.nome,
                profissional.tipo,
                profissional.contato,
                profissional.especialidade,
                profissional.outrasInformacoes,
                profissional.eventos
        );
    }
}
