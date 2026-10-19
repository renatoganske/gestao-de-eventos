package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.EventCustomerSummaryDto;
import jakarta.persistence.*;
import lombok.*;

/**
 * Associates a customer with an event (ADR-0025). An event can have several customers -- both
 * people of a couple, the parents of a birthday child -- and at most one of them is the one who
 * hired the job ({@code contracting}).
 */
@Entity(name = "TB_EVENT_CUSTOMER")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EventCustomer {

    @EmbeddedId
    private EventCustomerId id;

    @ManyToOne
    @MapsId("eventId")
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne
    @MapsId("customerId")
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "contracting", nullable = false)
    private boolean contracting;

    public EventCustomerSummaryDto toDTO() {
        return new EventCustomerSummaryDto(
                this.customer != null ? this.customer.getId() : null,
                this.customer != null ? this.customer.getName() : null,
                this.contracting
        );
    }
}
