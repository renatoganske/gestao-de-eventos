package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.enums.EventType;
import com.renatoganske.gestao_de_eventos.exceptions.EventNotFoundException;
import com.renatoganske.gestao_de_eventos.filters.EventFilter;
import com.renatoganske.gestao_de_eventos.repositories.EventRepository;
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

    @Transactional
    public EventDto createEvent(CreateEventDto createEventDto) {
        return eventRepository.save(createEventDto.toEntity()).toDTO();
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

        event.setEventCode(createEventDto.eventCode());
        event.setType(createEventDto.type());
        event.setName(createEventDto.name());
        event.setEventDate(createEventDto.eventDate());
        event.setDaytimeWedding(createEventDto.daytimeWedding());
        event.setOutdoorWedding(createEventDto.outdoorWedding());
        event.setGuestCount(createEventDto.guestCount());
        event.setDescription(createEventDto.description());
        event.setAmount(createEventDto.amount());
        event.setSizeGb(createEventDto.sizeGb());
        event.setDeliveryStatus(createEventDto.deliveryStatus());
        event.setHd(createEventDto.hd());
        event.setEventVenue(createEventDto.eventVenue());
        event.setCustomer(createEventDto.customer() != null ? createEventDto.customer().toEntity() : null);

        Event updatedEvent = eventRepository.save(event);
        return updatedEvent.toDTO();
    }

    @Transactional
    public void deleteEvent(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        eventRepository.delete(event);
    }

    public List<EventDto> searchEvents(EventType type, UUID venueId, UUID professionalId,
                                        LocalDate from, LocalDate to, UUID hdId, DeliveryStatus deliveryStatus) {
        Predicate<Event> filter = EventFilter.byType(type)
                .and(EventFilter.byVenue(venueId))
                .and(EventFilter.byProfessional(professionalId))
                .and(EventFilter.byPeriod(from, to))
                .and(EventFilter.byHd(hdId))
                .and(EventFilter.byDeliveryStatus(deliveryStatus));

        return eventRepository.findAll().stream()
                .filter(filter)
                .map(Event::toDTO)
                .collect(Collectors.toList());
    }
}
