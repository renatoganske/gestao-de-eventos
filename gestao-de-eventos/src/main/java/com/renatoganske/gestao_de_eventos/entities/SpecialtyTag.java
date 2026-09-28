package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.SpecialtyTagDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity(name = "TB_SPECIALTY_TAG")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SpecialtyTag {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    public SpecialtyTagDto toResponseDto() {
        return new SpecialtyTagDto(this.id, this.name);
    }
}
