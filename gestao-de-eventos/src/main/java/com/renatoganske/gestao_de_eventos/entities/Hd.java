package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.HdDto;
import com.renatoganske.gestao_de_eventos.enums.HdStatus;
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
    @Column(name = "capacity_gb")
    private Integer capacityGb;
    @Column(name = "used_space_gb")
    private Integer usedSpaceGb;
    @Column(name = "physical_location")
    private String physicalLocation;
    @Column(name = "serial_number")
    private String serialNumber;
    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private HdStatus status;

    @OneToMany(mappedBy = "hd")
    private List<Event> events;

    public HdDto toResponseDto() {
        return new HdDto(
                this.id,
                this.name,
                this.capacityGb,
                this.usedSpaceGb,
                this.physicalLocation,
                this.serialNumber,
                this.acquisitionDate,
                this.status,
                this.events
        );
    }
}
