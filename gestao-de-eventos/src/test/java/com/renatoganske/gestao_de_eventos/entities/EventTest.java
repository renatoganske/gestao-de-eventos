package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.enums.EventType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventTest {

    @Test
    void toDTO_mapsAllFieldsIncludingTypeAndDeliveryStatusEnums() {
        UUID id = UUID.randomUUID();
        List<EventProfessional> eventProfessionals = Collections.emptyList();
        Hd hd = Hd.builder().id(UUID.randomUUID()).name("HD Externo 1TB").build();
        EventVenue eventVenue = EventVenue.builder().id(UUID.randomUUID()).name("Salao de Festas").build();
        Customer customer = Customer.builder().id(UUID.randomUUID()).name("Ana e Bruno").build();

        Event event = Event.builder()
                .id(id)
                .eventCode("EVT-001")
                .type(EventType.WEDDING)
                .name("Casamento Ana e Bruno")
                .eventDate(LocalDate.of(2026, 10, 10))
                .daytimeWedding(false)
                .outdoorWedding(true)
                .guestCount(150L)
                .description("Casamento ao ar livre")
                .amount(8500.0)
                .sizeGb(120)
                .deliveryStatus(DeliveryStatus.PENDING)
                .hd(hd)
                .eventVenue(eventVenue)
                .customer(customer)
                .eventProfessionals(eventProfessionals)
                .build();

        EventDto dto = event.toDTO();

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.eventCode()).isEqualTo("EVT-001");
        assertThat(dto.type()).isEqualTo(EventType.WEDDING);
        assertThat(dto.name()).isEqualTo("Casamento Ana e Bruno");
        assertThat(dto.eventDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(dto.daytimeWedding()).isFalse();
        assertThat(dto.outdoorWedding()).isTrue();
        assertThat(dto.guestCount()).isEqualTo(150L);
        assertThat(dto.description()).isEqualTo("Casamento ao ar livre");
        assertThat(dto.amount()).isEqualTo(8500.0);
        assertThat(dto.sizeGb()).isEqualTo(120);
        assertThat(dto.deliveryStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(dto.hd()).isEqualTo(hd);
        assertThat(dto.eventVenue()).isEqualTo(eventVenue);
        assertThat(dto.customer()).isEqualTo(customer);
        assertThat(dto.eventProfessionals()).isEqualTo(eventProfessionals);
    }
}
