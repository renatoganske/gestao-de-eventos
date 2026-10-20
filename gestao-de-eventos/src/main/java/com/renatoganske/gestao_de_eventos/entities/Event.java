package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventSummaryDto;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Comparator;
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
    @ManyToOne
    @JoinColumn(name = "event_type_id")
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

    @OneToMany(mappedBy = "event")
    private List<EventEventVenue> eventVenues;

    @OneToMany(mappedBy = "event")
    private List<EventCustomer> eventCustomers;

    @OneToMany(mappedBy = "event")
    private List<EventProfessional> eventProfessionals;

    public EventDto toDTO() {
        return new EventDto(
                this.id,
                this.eventCode,
                this.type != null ? this.type.toResponseDto() : null,
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
                this.eventVenues == null ? List.of() : this.eventVenues.stream()
                        // by name, so the order does not depend on how the database returns the rows
                        .sorted(Comparator.comparing((EventEventVenue ev) -> ev.getEventVenue().getName(),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .map(EventEventVenue::toDTO)
                        .collect(Collectors.toList()),
                this.eventCustomers == null ? List.of() : this.eventCustomers.stream()
                        // contracting customer first, so lists and labels can just take the head
                        .sorted(Comparator.comparing((EventCustomer ec) -> !ec.isContracting()))
                        .map(EventCustomer::toDTO)
                        .collect(Collectors.toList()),
                this.eventProfessionals == null ? List.of() : this.eventProfessionals.stream()
                        // by name too: batch fetching returns the rows in batch order, not in a stable one
                        .sorted(Comparator.comparing((EventProfessional ep) -> ep.getProfessional().getName(),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .map(EventProfessional::toDTO)
                        .collect(Collectors.toList())
        );
    }

    public EventSummaryDto toSummaryDto() {
        return new EventSummaryDto(
                this.id,
                this.eventCode,
                this.type != null ? this.type.toResponseDto() : null,
                this.name,
                this.eventDate,
                this.deliveryStatus
        );
    }
}
