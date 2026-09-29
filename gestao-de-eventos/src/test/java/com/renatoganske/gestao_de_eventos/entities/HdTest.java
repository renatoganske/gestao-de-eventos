package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.HdDto;
import com.renatoganske.gestao_de_eventos.enums.HdStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HdTest {

    @Test
    void toResponseDto_mapsAllFieldsIncludingStatusEnum() {
        UUID id = UUID.randomUUID();
        List<Event> events = Collections.emptyList();

        Hd hd = Hd.builder()
                .id(id)
                .name("HD Externo 1TB")
                .capacityGb(1000)
                .realCapacityGb(931)
                .usedSpaceGb(350)
                .physicalLocation("Gaveta do escritório")
                .serialNumber("SN-123456")
                .acquisitionDate(LocalDate.of(2024, 1, 15))
                .status(HdStatus.ACTIVE)
                .events(events)
                .build();

        HdDto dto = hd.toResponseDto();

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.name()).isEqualTo("HD Externo 1TB");
        assertThat(dto.capacityGb()).isEqualTo(1000);
        assertThat(dto.realCapacityGb()).isEqualTo(931);
        assertThat(dto.usedSpaceGb()).isEqualTo(350);
        assertThat(dto.physicalLocation()).isEqualTo("Gaveta do escritório");
        assertThat(dto.serialNumber()).isEqualTo("SN-123456");
        assertThat(dto.acquisitionDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(dto.status()).isEqualTo(HdStatus.ACTIVE);
        assertThat(dto.events()).isEqualTo(events);
    }
}
