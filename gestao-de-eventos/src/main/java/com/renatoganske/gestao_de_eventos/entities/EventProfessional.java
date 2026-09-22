package com.renatoganske.gestao_de_eventos.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity(name = "TB_EVENT_PROFESSIONAL")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EventProfessional {

    @EmbeddedId
    private EventProfessionalId id;

    @ManyToOne
    @MapsId("eventId")
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne
    @MapsId("professionalId")
    @JoinColumn(name = "professional_id")
    private Professional professional;

    @Column(name = "role_in_event")
    private String roleInEvent;
}
