package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.EventType;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * DTO for {@link EventType}
 */
public record CreateEventTypeDto(
        @NotBlank String name
) implements Serializable {
    public EventType toEntity() {
        return EventType.builder()
                .name(this.name())
                .build();
    }
}
