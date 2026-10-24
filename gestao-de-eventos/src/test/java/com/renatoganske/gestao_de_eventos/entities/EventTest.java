package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.EventCustomerSummaryDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventListItemDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueSummaryDto;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class EventTest {

    @Test
    void toDTO_mapsAllFieldsIncludingTypeAndDeliveryStatusEnums() {
        UUID id = UUID.randomUUID();
        List<EventProfessional> eventProfessionals = Collections.emptyList();
        Hd hd = Hd.builder().id(UUID.randomUUID()).name("HD Externo 1TB").build();
        EventVenue eventVenue = EventVenue.builder().id(UUID.randomUUID()).name("Salao de Festas").build();
        Customer customer = Customer.builder().id(UUID.randomUUID()).name("Ana e Bruno").build();
        EventType type = EventType.builder().id(UUID.randomUUID()).name("WEDDING").build();

        Event event = Event.builder()
                .id(id)
                .eventCode("EVT-001")
                .type(type)
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
                .eventProfessionals(eventProfessionals)
                .build();
        Customer partner = Customer.builder().id(UUID.randomUUID()).name("Bruno").build();
        event.setEventCustomers(List.of(
                EventCustomer.builder().id(new EventCustomerId(id, partner.getId()))
                        .event(event).customer(partner).contracting(false).build(),
                EventCustomer.builder().id(new EventCustomerId(id, customer.getId()))
                        .event(event).customer(customer).contracting(true).build()));

        EventVenue church = EventVenue.builder().id(UUID.randomUUID()).name("Igreja Matriz").build();
        event.setEventVenues(List.of(
                EventEventVenue.builder().id(new EventEventVenueId(id, church.getId()))
                        .event(event).eventVenue(church).role("Cerimônia").build(),
                EventEventVenue.builder().id(new EventEventVenueId(id, eventVenue.getId()))
                        .event(event).eventVenue(eventVenue).build()));

        EventDto dto = event.toDTO();

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.eventCode()).isEqualTo("EVT-001");
        assertThat(dto.type().id()).isEqualTo(type.getId());
        assertThat(dto.type().name()).isEqualTo("WEDDING");
        assertThat(dto.name()).isEqualTo("Casamento Ana e Bruno");
        assertThat(dto.eventDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(dto.daytimeWedding()).isFalse();
        assertThat(dto.outdoorWedding()).isTrue();
        assertThat(dto.guestCount()).isEqualTo(150L);
        assertThat(dto.description()).isEqualTo("Casamento ao ar livre");
        assertThat(dto.amount()).isEqualTo(8500.0);
        assertThat(dto.sizeGb()).isEqualTo(120);
        assertThat(dto.deliveryStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(dto.hdId()).isEqualTo(hd.getId());
        // venues come sorted by name, each with its optional role
        assertThat(dto.venues())
                .extracting(EventVenueSummaryDto::venueId, EventVenueSummaryDto::venueName, EventVenueSummaryDto::role)
                .containsExactly(
                        tuple(church.getId(), "Igreja Matriz", "Cerimônia"),
                        tuple(eventVenue.getId(), "Salao de Festas", null));
        // contracting customer comes first regardless of the association order
        assertThat(dto.customers())
                .extracting(EventCustomerSummaryDto::customerId, EventCustomerSummaryDto::customerName,
                        EventCustomerSummaryDto::contracting)
                .containsExactly(
                        tuple(customer.getId(), "Ana e Bruno", true),
                        tuple(partner.getId(), "Bruno", false));
        assertThat(dto.eventProfessionals()).isEmpty();
    }

    @Test
    void toListItemDto_keepsOnlyWhatTheListShowsWithTheSameOrderingAsTheFullDto() {
        UUID id = UUID.randomUUID();
        Hd hd = Hd.builder().id(UUID.randomUUID()).name("HD Externo 1TB").build();
        EventType type = EventType.builder().id(UUID.randomUUID()).name("WEDDING").build();
        Event event = Event.builder()
                .id(id).eventCode("EVT-001").type(type).name("Casamento Ana e Bruno")
                .eventDate(LocalDate.of(2026, 10, 10)).deliveryStatus(DeliveryStatus.DELIVERED).hd(hd)
                .amount(8500.0).description("nao vai para a lista")
                .build();
        Customer contracting = Customer.builder().id(UUID.randomUUID()).name("Ana").build();
        Customer partner = Customer.builder().id(UUID.randomUUID()).name("Bruno").build();
        event.setEventCustomers(List.of(
                EventCustomer.builder().id(new EventCustomerId(id, partner.getId()))
                        .event(event).customer(partner).contracting(false).build(),
                EventCustomer.builder().id(new EventCustomerId(id, contracting.getId()))
                        .event(event).customer(contracting).contracting(true).build()));
        EventVenue hall = EventVenue.builder().id(UUID.randomUUID()).name("Salao").build();
        EventVenue church = EventVenue.builder().id(UUID.randomUUID()).name("Igreja").build();
        event.setEventVenues(List.of(
                EventEventVenue.builder().id(new EventEventVenueId(id, hall.getId())).event(event).eventVenue(hall).build(),
                EventEventVenue.builder().id(new EventEventVenueId(id, church.getId())).event(event).eventVenue(church)
                        .role("Cerimônia").build()));
        Professional photographer = Professional.builder().id(UUID.randomUUID()).name("Foto").build();
        Professional assistant = Professional.builder().id(UUID.randomUUID()).name("Assistente").build();
        event.setEventProfessionals(List.of(
                EventProfessional.builder().id(new EventProfessionalId(id, photographer.getId()))
                        .event(event).professional(photographer).build(),
                EventProfessional.builder().id(new EventProfessionalId(id, assistant.getId()))
                        .event(event).professional(assistant).build()));

        EventListItemDto dto = event.toListItemDto();

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.eventCode()).isEqualTo("EVT-001");
        assertThat(dto.type().name()).isEqualTo("WEDDING");
        assertThat(dto.name()).isEqualTo("Casamento Ana e Bruno");
        assertThat(dto.eventDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(dto.deliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(dto.hdId()).isEqualTo(hd.getId());
        assertThat(dto.hdName()).isEqualTo("HD Externo 1TB");
        assertThat(dto.venues()).extracting(EventVenueSummaryDto::venueName).containsExactly("Igreja", "Salao");
        assertThat(dto.customers()).extracting(EventCustomerSummaryDto::customerName).containsExactly("Ana", "Bruno");
        assertThat(dto.professionalCount()).isEqualTo(2);
    }

    @Test
    void toListItemDto_toleratesAnEventWithoutTypeHdOrAssociations() {
        EventListItemDto dto = Event.builder().id(UUID.randomUUID()).eventCode("EVT-002").name("Vazio").build()
                .toListItemDto();

        assertThat(dto.type()).isNull();
        assertThat(dto.hdId()).isNull();
        assertThat(dto.hdName()).isNull();
        assertThat(dto.venues()).isEmpty();
        assertThat(dto.customers()).isEmpty();
        assertThat(dto.professionalCount()).isZero();
    }
}
