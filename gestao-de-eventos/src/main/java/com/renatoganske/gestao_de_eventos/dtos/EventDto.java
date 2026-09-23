package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.*;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.enums.EventType;

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
        EventType type, String name,
        LocalDate eventDate,
        Boolean daytimeWedding,
        Boolean outdoorWedding,
        Long guestCount,
        String description,
        Double amount,
        Integer sizeGb,
        DeliveryStatus deliveryStatus,
        Hd hd,
        EventVenue eventVenue,
        Customer customer,
        List<EventProfessional> eventProfessionals
) implements Serializable {
}
