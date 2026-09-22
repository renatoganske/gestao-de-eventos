package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.entities.EventVenue;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for {@link Event}
 */
public record CreateEventDto(
        String eventCode,
        String type,
        String name,
        LocalDate eventDate,
        Boolean daytimeWedding,
        Boolean outdoorWedding,
        Long guestCount,
        String description,
        Double amount,
        Hd hd,
        EventVenue eventVenue,
        CreateCustomerDto customer
) implements Serializable {
    public Event toEntity() {
        return Event.builder()
                .eventCode(this.eventCode())
                .type(this.type())
                .name(this.name())
                .eventDate(this.eventDate())
                .daytimeWedding(this.daytimeWedding())
                .outdoorWedding(this.outdoorWedding())
                .guestCount(this.guestCount())
                .description(this.description())
                .amount(this.amount())
                .hd(this.hd())
                .eventVenue(this.eventVenue())
                .build();
    }
}
