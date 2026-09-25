package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.EventVenueDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity(name = "TB_EVENT_VENUE")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EventVenue {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "address")
    private String address;
    @Column(name = "city")
    private String city;
    @Column(name = "state")
    private String state;
    @Column(name = "type")
    private String type;

    @OneToMany(mappedBy = "eventVenue")
    private List<Event> events;

    public EventVenueDto toResponseDto() {
        return new EventVenueDto(
                this.id,
                this.name,
                this.address,
                this.city,
                this.state,
                this.type,
                this.events == null ? List.of() : this.events.stream()
                        .map(Event::toSummaryDto)
                        .collect(Collectors.toList())
        );
    }
}
