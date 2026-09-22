package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.EventVenue;

import java.io.Serializable;

/**
 * DTO for {@link EventVenue}
 */
public record CreateEventVenueDto(
        String name,
        String adress,
        String city,
        String state,
        String type
) implements Serializable {
    public EventVenue toEntity() {
        return EventVenue.builder()
                .name(this.name())
                .adress(this.adress())
                .city(this.city())
                .state(this.state())
                .type(this.type())
                .build();
    }
}