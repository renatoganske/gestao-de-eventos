package com.renatoganske.gestao_de_eventos.dtos;

import com.renatoganske.gestao_de_eventos.entities.EventType;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * DTO for {@link EventType}.
 *
 * <p>{@code hasWeddingFields} is optional: on create, omitting it means {@code false}; on update,
 * omitting it leaves the current value untouched (so renaming a type through a payload that only
 * carries the name never silently un-marks it).
 */
public record CreateEventTypeDto(
        @NotBlank String name,
        Boolean hasWeddingFields
) implements Serializable {
    public EventType toEntity() {
        return EventType.builder()
                .name(this.name())
                .hasWeddingFields(Boolean.TRUE.equals(this.hasWeddingFields()))
                .build();
    }
}
