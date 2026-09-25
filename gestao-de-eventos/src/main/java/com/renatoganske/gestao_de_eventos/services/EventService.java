package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.entities.Customer;
import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.entities.EventVenue;
import com.renatoganske.gestao_de_eventos.entities.EventType;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.exceptions.CustomerNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.EventNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.EventTypeNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.EventVenueNotFoundException;
import com.renatoganske.gestao_de_eventos.exceptions.HdNotFoundException;
import com.renatoganske.gestao_de_eventos.filters.EventFilter;
import com.renatoganske.gestao_de_eventos.repositories.CustomerRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventTypeRepository;
import com.renatoganske.gestao_de_eventos.repositories.EventVenueRepository;
import com.renatoganske.gestao_de_eventos.repositories.HdRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final CustomerRepository customerRepository;
    private final HdRepository hdRepository;
    private final EventVenueRepository eventVenueRepository;
    private final EventTypeRepository eventTypeRepository;

    @Transactional
    public EventDto createEvent(CreateEventDto createEventDto) {
        Event event = createEventDto.toEntity();
        event.setType(resolveEventType(createEventDto.eventTypeId()));
        event.setHd(resolveHd(createEventDto.hdId()));
        event.setEventVenue(resolveEventVenue(createEventDto.eventVenueId()));
        event.setCustomer(resolveCustomer(createEventDto.customerId()));

        Event savedEvent = eventRepository.save(event);
        adjustHdUsedSpace(savedEvent.getHd(), savedEvent.getSizeGb());

        return savedEvent.toDTO();
    }

    public List<EventDto> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(Event::toDTO)
                .collect(Collectors.toList());
    }

    public EventDto getEventById(UUID id) {
        return eventRepository.findById(id)
                .map(Event::toDTO)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    @Transactional
    public EventDto updateEvent(UUID id, CreateEventDto createEventDto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        Hd previousHd = event.getHd();
        Integer previousSizeGb = event.getSizeGb();

        event.setEventCode(createEventDto.eventCode());
        event.setType(resolveEventType(createEventDto.eventTypeId()));
        event.setName(createEventDto.name());
        event.setEventDate(createEventDto.eventDate());
        event.setDaytimeWedding(createEventDto.daytimeWedding());
        event.setOutdoorWedding(createEventDto.outdoorWedding());
        event.setGuestCount(createEventDto.guestCount());
        event.setDescription(createEventDto.description());
        event.setAmount(createEventDto.amount());
        event.setSizeGb(createEventDto.sizeGb());
        event.setDeliveryStatus(createEventDto.deliveryStatus());
        event.setHd(resolveHd(createEventDto.hdId()));
        event.setEventVenue(resolveEventVenue(createEventDto.eventVenueId()));
        event.setCustomer(resolveCustomer(createEventDto.customerId()));

        Event updatedEvent = eventRepository.save(event);

        adjustHdUsedSpace(previousHd, previousSizeGb == null ? null : -previousSizeGb);
        adjustHdUsedSpace(updatedEvent.getHd(), updatedEvent.getSizeGb());

        return updatedEvent.toDTO();
    }

    @Transactional
    public void deleteEvent(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        Hd hd = event.getHd();
        Integer sizeGb = event.getSizeGb();

        eventRepository.delete(event);

        adjustHdUsedSpace(hd, sizeGb == null ? null : -sizeGb);
    }

    public List<EventDto> searchEvents(UUID eventTypeId, UUID venueId, UUID professionalId,
                                        LocalDate from, LocalDate to, UUID hdId, DeliveryStatus deliveryStatus,
                                        String customerName, String eventCode) {
        Predicate<Event> filter = EventFilter.byType(eventTypeId)
                .and(EventFilter.byVenue(venueId))
                .and(EventFilter.byProfessional(professionalId))
                .and(EventFilter.byPeriod(from, to))
                .and(EventFilter.byHd(hdId))
                .and(EventFilter.byDeliveryStatus(deliveryStatus))
                .and(EventFilter.byCustomerName(customerName))
                .and(EventFilter.byEventCode(eventCode));

        return eventRepository.findAll().stream()
                .filter(filter)
                .map(Event::toDTO)
                .collect(Collectors.toList());
    }

    private EventType resolveEventType(UUID eventTypeId) {
        if (eventTypeId == null) {
            return null;
        }
        return eventTypeRepository.findById(eventTypeId)
                .orElseThrow(() -> new EventTypeNotFoundException(eventTypeId));
    }

    private Hd resolveHd(UUID hdId) {
        if (hdId == null) {
            return null;
        }
        return hdRepository.findById(hdId).orElseThrow(() -> new HdNotFoundException(hdId));
    }

    private EventVenue resolveEventVenue(UUID eventVenueId) {
        if (eventVenueId == null) {
            return null;
        }
        return eventVenueRepository.findById(eventVenueId)
                .orElseThrow(() -> new EventVenueNotFoundException(eventVenueId));
    }

    private Customer resolveCustomer(UUID customerId) {
        if (customerId == null) {
            return null;
        }
        return customerRepository.findById(customerId).orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    private void adjustHdUsedSpace(Hd hd, Integer deltaGb) {
        if (hd == null || deltaGb == null || deltaGb == 0) {
            return;
        }
        int currentUsedSpaceGb = hd.getUsedSpaceGb() == null ? 0 : hd.getUsedSpaceGb();
        hd.setUsedSpaceGb(currentUsedSpaceGb + deltaGb);
        hdRepository.save(hd);
    }
}
