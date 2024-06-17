package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.EventoResponseDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity(name = "TB_EVENTO")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "codigo_do_evento")
    private String codigoDoEvento;
    @Column(name = "tipo")
    private String tipo;
    @Column(name = "nome", nullable = false)
    private String nome;
    @Column(name = "data_do_evento")
    private LocalDate dataDoEvento;
    @Column(name = "casamento_de_dia")
    private Boolean casamentoDeDia;
    @Column(name = "casamento_externo")
    private Boolean casamentoExterno;
    @Column(name = "quantidade_de_convidados")
    private Long quantidadeDeConvidados;
    @Column(name = "descricao")
    private String descricao;
    @Column(name = "valor")
    private Double valor;

    @ManyToOne
    @JoinColumn(name = "hd_id")
    private Hd hd;

    @ManyToOne
    @JoinColumn(name = "local_do_evento_id")
    private LocalDoEvento localDoEvento;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToMany
    @JoinTable(
            name = "evento_profissional",
            joinColumns = @JoinColumn(name = "evento_id"),
            inverseJoinColumns = @JoinColumn(name = "profissional_id")
    )
    private List<Profissional> profissionais;

    public EventoResponseDto toDTO() {
        return new EventoResponseDto(
                this.id,
                this.codigoDoEvento,
                this.tipo,
                this.nome,
                this.dataDoEvento,
                this.casamentoDeDia,
                this.casamentoExterno,
                this.quantidadeDeConvidados,
                this.descricao,
                this.valor,
                this.hd,
                this.localDoEvento,
                this.cliente,
                this.profissionais
        );
    }
}
