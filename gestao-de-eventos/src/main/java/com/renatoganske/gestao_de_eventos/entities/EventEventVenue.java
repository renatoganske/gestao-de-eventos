package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.EventVenueSummaryDto;
import jakarta.persistence.*;
import lombok.*;

/**
 * Associates a venue with an event (ADR-0026). An event can happen in several places -- the
 * ceremony in a church, the party somewhere else -- and each link may say what the venue is for
 * ({@code role}, free text, optional).
 */
@Entity(name = "TB_EVENT_EVENT_VENUE")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EventEventVenue {

    @EmbeddedId
    private EventEventVenueId id;

    @ManyToOne
    @MapsId("eventId")
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne
    @MapsId("eventVenueId")
    @JoinColumn(name = "event_venue_id")
    private EventVenue eventVenue;

    @Column(name = "role")
    private String role;

    public EventVenueSummaryDto toDTO() {
        return new EventVenueSummaryDto(
                this.eventVenue != null ? this.eventVenue.getId() : null,
                this.eventVenue != null ? this.eventVenue.getName() : null,
                this.role
        );
    }
}
