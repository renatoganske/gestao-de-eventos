package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.EventTypeDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity(name = "TB_EVENT_TYPE")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EventType {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    // ADR-0024: whether events of this type carry the wedding-specific flags (daytime / outdoor).
    @Column(name = "has_wedding_fields", nullable = false)
    private boolean hasWeddingFields;

    public EventTypeDto toResponseDto() {
        return new EventTypeDto(this.id, this.name, this.hasWeddingFields);
    }
}
