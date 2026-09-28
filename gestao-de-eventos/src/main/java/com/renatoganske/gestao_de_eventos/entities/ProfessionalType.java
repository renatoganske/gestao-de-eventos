package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.ProfessionalTypeDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity(name = "TB_PROFESSIONAL_TYPE")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ProfessionalType {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    public ProfessionalTypeDto toResponseDto() {
        return new ProfessionalTypeDto(this.id, this.name);
    }
}
