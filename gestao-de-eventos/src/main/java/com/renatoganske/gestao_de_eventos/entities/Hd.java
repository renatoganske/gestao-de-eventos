package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.HdDto;
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

    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "capacidade")
    private Integer capacidade;
    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;
    @Column(name = "status")
    private String status;

    @OneToMany(mappedBy = "hd")
    private List<Event> events;

    public HdDto toResponseDto() {
        return new HdDto(
                this.id,
                this.name,
                this.capacidade,
                this.acquisitionDate,
                this.status,
                this.events
        );
    }
}
