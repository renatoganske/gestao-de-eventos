package com.renatoganske.gestao_de_eventos.filters;

import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Predicate;

public interface EventFilter extends Predicate<Event> {

    static EventFilter byType(UUID eventTypeId) {
        return e -> eventTypeId == null
                || (e.getType() != null && eventTypeId.equals(e.getType().getId()));
    }

    static EventFilter byVenue(UUID venueId) {
        return e -> venueId == null
                || (e.getEventVenue() != null && venueId.equals(e.getEventVenue().getId()));
    }

    static EventFilter byProfessional(UUID professionalId) {
        return e -> professionalId == null
                || (e.getEventProfessionals() != null && e.getEventProfessionals().stream()
                        .anyMatch(ep -> ep.getProfessional() != null
                                && professionalId.equals(ep.getProfessional().getId())));
    }

    static EventFilter byPeriod(LocalDate from, LocalDate to) {
        return e -> {
            if (from == null && to == null) {
                return true;
            }
            if (e.getEventDate() == null) {
                return false;
            }
            if (from != null && e.getEventDate().isBefore(from)) {
                return false;
            }
            return to == null || !e.getEventDate().isAfter(to);
        };
    }

    static EventFilter byHd(UUID hdId) {
        return e -> hdId == null
                || (e.getHd() != null && hdId.equals(e.getHd().getId()));
    }

    static EventFilter byDeliveryStatus(DeliveryStatus deliveryStatus) {
        return e -> deliveryStatus == null || e.getDeliveryStatus() == deliveryStatus;
    }

    static EventFilter byCustomerName(String customerName) {
        return e -> customerName == null || customerName.isBlank()
                || (e.getCustomer() != null && e.getCustomer().getName() != null
                        && e.getCustomer().getName().toLowerCase().contains(customerName.toLowerCase()));
    }

    static EventFilter byEventCode(String eventCode) {
        return e -> eventCode == null || eventCode.isBlank()
                || (e.getEventCode() != null
                        && e.getEventCode().toLowerCase().contains(eventCode.toLowerCase()));
    }
}
