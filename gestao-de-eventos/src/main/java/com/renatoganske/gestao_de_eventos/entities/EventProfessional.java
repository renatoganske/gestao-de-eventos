package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.EventProfessionalSummaryDto;
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

    public EventProfessionalSummaryDto toDTO() {
        return new EventProfessionalSummaryDto(
                this.event != null ? this.event.getId() : null,
                this.event != null ? this.event.getEventCode() : null,
                this.event != null ? this.event.getName() : null,
                this.professional != null ? this.professional.getId() : null,
                this.professional != null ? this.professional.getName() : null,
                this.roleInEvent
        );
    }
}
