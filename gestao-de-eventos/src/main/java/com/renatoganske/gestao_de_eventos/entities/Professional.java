package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.ProfessionalDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @ManyToOne
    @JoinColumn(name = "professional_type_id")
    private ProfessionalType type;
    @Column(name = "contact")
    private String contact;
    @ManyToMany
    @JoinTable(
            name = "tb_professional_specialty",
            joinColumns = @JoinColumn(name = "professional_id"),
            inverseJoinColumns = @JoinColumn(name = "specialty_tag_id")
    )
    @Builder.Default
    private Set<SpecialtyTag> specialtyTags = new java.util.HashSet<>();
    @Column(name = "other_info")
    private String otherInfo;

    @OneToMany(mappedBy = "professional")
    private List<EventProfessional> eventProfessionals;

    public ProfessionalDto toResponseDto() {
        return new ProfessionalDto(
                this.id,
                this.name,
                this.type != null ? this.type.toResponseDto() : null,
                this.contact,
                this.specialtyTags == null ? List.of() : this.specialtyTags.stream()
                        .map(SpecialtyTag::toResponseDto)
                        .collect(Collectors.toList()),
                this.otherInfo,
                this.eventProfessionals == null ? List.of() : this.eventProfessionals.stream()
                        .map(EventProfessional::toDTO)
                        .collect(Collectors.toList())
        );
    }
}
