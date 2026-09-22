package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Event}
 */
public record EventDto(
        UUID id,
        String eventCode,
        String type, String name,
        LocalDate eventDate,
        Boolean daytimeWedding,
        Boolean outdoorWedding,
        Long guestCount,
        String description,
        Double amount, Hd hd,
        EventVenue eventVenue,
        Customer customer,
        List<EventProfessional> eventProfessionals
) implements Serializable {
}
