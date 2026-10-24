package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.EventVenue;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link EventVenue}
 */
public record EventVenueDto(
        UUID id,
        String name,
        String address,
        String city,
        String state,
        String type,
        int eventCount
) implements Serializable {
}
