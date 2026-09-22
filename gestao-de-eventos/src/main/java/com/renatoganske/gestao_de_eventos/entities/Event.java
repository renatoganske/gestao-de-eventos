package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity(name = "TB_EVENT")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "event_code")
    private String eventCode;
    @Column(name = "type")
    private String type;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "event_date")
    private LocalDate eventDate;
    @Column(name = "daytime_wedding")
    private Boolean daytimeWedding;
    @Column(name = "outdoor_wedding")
    private Boolean outdoorWedding;
    @Column(name = "guest_count")
    private Long guestCount;
    @Column(name = "description")
    private String description;
    @Column(name = "amount")
    private Double amount;

    @ManyToOne
    @JoinColumn(name = "hd_id")
    private Hd hd;

    @ManyToOne
    @JoinColumn(name = "event_venue_id")
    private EventVenue eventVenue;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @OneToMany(mappedBy = "event")
    private List<EventProfessional> eventProfessionals;

    public EventDto toDTO() {
        return new EventDto(
                this.id,
                this.eventCode,
                this.type,
                this.name,
                this.eventDate,
                this.daytimeWedding,
                this.outdoorWedding,
                this.guestCount,
                this.description,
                this.amount,
                this.hd,
                this.eventVenue,
                this.customer,
                this.eventProfessionals
        );
    }
}
