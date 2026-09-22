package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.enums.HdStatus;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for {@link com.renatoganske.gestao_de_eventos.entities.Hd}
 */
public record CreateHdDto(
        String name,
        Integer capacityGb,
        Integer usedSpaceGb,
        String physicalLocation,
        String serialNumber,
        LocalDate acquisitionDate,
        HdStatus status
) implements Serializable {
    public Hd toEntity() {
        return Hd.builder()
                .name(this.name())
                .capacityGb(this.capacityGb())
                .usedSpaceGb(this.usedSpaceGb())
                .physicalLocation(this.physicalLocation())
                .serialNumber(this.serialNumber())
                .acquisitionDate(this.acquisitionDate())
                .status(this.status())
                .build();
    }
}
