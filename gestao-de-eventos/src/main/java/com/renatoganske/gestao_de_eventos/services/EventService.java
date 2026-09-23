package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.entities.Event;
import com.renatoganske.gestao_de_eventos.exceptions.EventNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.EventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
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
}
