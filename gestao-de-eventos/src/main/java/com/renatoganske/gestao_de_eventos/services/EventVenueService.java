package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueDto;
import com.renatoganske.gestao_de_eventos.entities.EventVenue;
import com.renatoganske.gestao_de_eventos.exceptions.EventVenueNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.EventVenueRepository;
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
public class EventVenueService {

    private final EventVenueRepository eventVenueRepository;

    @Transactional
    public EventVenueDto createEventVenue(CreateEventVenueDto createEventVenueDto) {
        return toResponseDto(eventVenueRepository.save(createEventVenueDto.toEntity()));
    }

    public List<EventVenueDto> getAllEventVenues() {
        return eventVenueRepository.findAll().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public EventVenueDto getEventVenueById(UUID id) {
        return eventVenueRepository.findById(id)
                .map(this::toResponseDto)
                .orElseThrow(() -> new EventVenueNotFoundException(id));
    }

    @Transactional
    public EventVenueDto updateEventVenue(UUID id, CreateEventVenueDto createEventVenueDto) {
        EventVenue eventVenue = eventVenueRepository.findById(id)
                .orElseThrow(() -> new EventVenueNotFoundException(id));

        eventVenue.setName(createEventVenueDto.name());
        eventVenue.setAddress(createEventVenueDto.address());
        eventVenue.setCity(createEventVenueDto.city());
        eventVenue.setState(createEventVenueDto.state());
        eventVenue.setType(createEventVenueDto.type());

        EventVenue updatedEventVenue = eventVenueRepository.save(eventVenue);
        return toResponseDto(updatedEventVenue);
    }

    @Transactional
    public void deleteEventVenue(UUID id) {
        EventVenue eventVenue = eventVenueRepository.findById(id)
                .orElseThrow(() -> new EventVenueNotFoundException(id));
        eventVenueRepository.delete(eventVenue);
    }

    /**
     * Builds the response DTO here rather than via EventVenue#toResponseDto(): that method exists
     * on the entity but is declared {@code private}, so it is inaccessible from this class.
     * Left as-is per task scope (EventVenue.java is off-limits for this ticket) — worth fixing
     * as its own small task so this service can follow the standard entity-owns-conversion convention.
     */
    private EventVenueDto toResponseDto(EventVenue eventVenue) {
        return new EventVenueDto(
                eventVenue.getId(),
                eventVenue.getName(),
                eventVenue.getAddress(),
                eventVenue.getCity(),
                eventVenue.getState(),
                eventVenue.getType(),
                eventVenue.getEvents());
    }
}
