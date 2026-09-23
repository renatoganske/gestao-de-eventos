package com.renatoganske.gestao_de_eventos.dtos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.renatoganske.gestao_de_eventos.entities.Customer;
import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.entities.EventProfessional;
import com.renatoganske.gestao_de_eventos.entities.EventProfessionalId;
import com.renatoganske.gestao_de_eventos.entities.EventVenue;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.entities.Professional;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.enums.EventType;
import com.renatoganske.gestao_de_eventos.enums.HdStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Proves ADR-0011 actually closes the cycle: builds the exact bidirectional graph
 * (Event <-> EventProfessional <-> Professional, Event <-> Customer/EventVenue/Hd via
 * their "events" back-reference) that used to StackOverflowError when response DTOs
 * embedded JPA entities directly, and asserts serialization now completes cleanly.
 */
class ResponseDtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void serializingResponseDtosWithBidirectionalRelationshipsDoesNotStackOverflow() {
        Customer customer = Customer.builder().id(UUID.randomUUID()).name("Maria Silva").build();
        EventVenue eventVenue = EventVenue.builder().id(UUID.randomUUID()).name("Buffet Jardim das Rosas").build();
        Hd hd = Hd.builder().id(UUID.randomUUID()).name("HD Externo 1")
                .capacityGb(1000).usedSpaceGb(200).status(HdStatus.ACTIVE).build();
        Professional professional = Professional.builder().id(UUID.randomUUID()).name("Joao Fotografo").build();

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .eventCode("EVT-001")
                .type(EventType.WEDDING)
                .name("Casamento Maria e Joao")
                .eventDate(LocalDate.of(2026, 10, 15))
                .deliveryStatus(DeliveryStatus.PENDING)
                .customer(customer)
                .eventVenue(eventVenue)
                .hd(hd)
                .build();

        EventProfessional eventProfessional = EventProfessional.builder()
                .id(new EventProfessionalId(event.getId(), professional.getId()))
                .event(event)
                .professional(professional)
                .roleInEvent("Fotografo principal")
                .build();

        // fecha os ciclos bidirecionais que estouravam StackOverflowError antes da ADR-0011
        event.setEventProfessionals(List.of(eventProfessional));
        customer.setEvents(List.of(event));
        eventVenue.setEvents(List.of(event));
        hd.setEvents(List.of(event));
        professional.setEventProfessionals(List.of(eventProfessional));

        assertThatCode(() -> {
            objectMapper.writeValueAsString(event.toDTO());
            objectMapper.writeValueAsString(customer.toResponseDto());
            objectMapper.writeValueAsString(eventVenue.toResponseDto());
            objectMapper.writeValueAsString(hd.toResponseDto());
            objectMapper.writeValueAsString(professional.toResponseDto());
        }).doesNotThrowAnyException();
    }

    @Test
    void customerResponseDtoJsonEmbedsEventSummaryNotRawEntity() throws Exception {
        Customer customer = Customer.builder().id(UUID.randomUUID()).name("Maria Silva").build();
        Event event = Event.builder()
                .id(UUID.randomUUID())
                .eventCode("EVT-001")
                .name("Casamento Maria e Joao")
                .customer(customer)
                .build();
        customer.setEvents(List.of(event));

        String json = objectMapper.writeValueAsString(customer.toResponseDto());

        assertThat(json).contains("\"eventCode\":\"EVT-001\"");
        assertThat(json).doesNotContain("\"customer\"");
    }
}
