package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.enums.HdStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Hd}
 */
public record HdDto(
        UUID id, String name,
        Integer capacityGb,
        Integer usedSpaceGb,
        String physicalLocation,
        String serialNumber,
        LocalDate acquisitionDate,
        HdStatus status,
        List<EventSummaryDto> events
) implements Serializable {
}
