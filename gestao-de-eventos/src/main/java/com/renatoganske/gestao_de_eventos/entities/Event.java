package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventSummaryDto;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.enums.EventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity(name = "TB_EVENT")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "event_code")
    private String eventCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private EventType type;
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
    @Column(name = "size_gb")
    private Integer sizeGb;
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    private DeliveryStatus deliveryStatus;

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
                this.sizeGb,
                this.deliveryStatus,
                this.hd != null ? this.hd.getId() : null,
                this.eventVenue != null ? this.eventVenue.getId() : null,
                this.customer != null ? this.customer.getId() : null,
                this.eventProfessionals == null ? List.of() : this.eventProfessionals.stream()
                        .map(EventProfessional::toDTO)
                        .collect(Collectors.toList())
        );
    }

    public EventSummaryDto toSummaryDto() {
        return new EventSummaryDto(
                this.id,
                this.eventCode,
                this.type,
                this.name,
                this.eventDate,
                this.deliveryStatus
        );
    }
}
