package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.enums.HdStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for {@link com.renatoganske.gestao_de_eventos.entities.Hd}
 */
public record CreateHdDto(
        @NotBlank String name,
        Integer capacityGb,
        @Positive Integer realCapacityGb,
        Integer usedSpaceGb,
        String physicalLocation,
        String serialNumber,
        LocalDate acquisitionDate,
        HdStatus status
) implements Serializable {
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "must not be greater than the nominal capacity")
    public boolean isRealCapacityWithinNominal() {
        return realCapacityGb == null || capacityGb == null || realCapacityGb <= capacityGb;
    }

    public Hd toEntity() {
        return Hd.builder()
                .name(this.name())
                .capacityGb(this.capacityGb())
                .realCapacityGb(this.realCapacityGb())
                .usedSpaceGb(this.usedSpaceGb())
                .physicalLocation(this.physicalLocation())
                .serialNumber(this.serialNumber())
                .acquisitionDate(this.acquisitionDate())
                .status(this.status())
                .build();
    }
}
