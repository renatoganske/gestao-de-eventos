package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.ProfessionalDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity(name = "TB_PROFESSIONAL")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Professional {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "type")
    private String type;
    @Column(name = "contact")
    private String contact;
    @Column(name = "specialty")
    private String specialty;
    @Column(name = "other_info")
    private String otherInfo;

    @ManyToMany(mappedBy = "professionals")
    private List<Event> events;

    public ProfessionalDto toResponseDto() {
        return new ProfessionalDto(
                this.id,
                this.name,
                this.type,
                this.contact,
                this.specialty,
                this.otherInfo,
                this.events
        );
    }
}
