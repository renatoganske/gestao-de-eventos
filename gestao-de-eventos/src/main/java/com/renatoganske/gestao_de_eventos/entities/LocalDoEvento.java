package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.LocalDoEventoResponseDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity(name = "TB_LOCAL_DO_EVENTO")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
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

    private LocalDoEventoResponseDto toResponseDto(LocalDoEvento localDoEvento) {
        return new LocalDoEventoResponseDto(
                localDoEvento.id,
                localDoEvento.nome,
                localDoEvento.endereco,
                localDoEvento.cidade,
                localDoEvento.estado,
                localDoEvento.tipo,
                localDoEvento.eventos
        );
    }
}
