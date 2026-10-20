package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueDto;
import com.renatoganske.gestao_de_eventos.entities.EventVenue;
import com.renatoganske.gestao_de_eventos.exceptions.EventVenueNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.EventCountProjection;
import com.renatoganske.gestao_de_eventos.repositories.EventVenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventVenueService {

    private final EventVenueRepository eventVenueRepository;

    @Transactional
    public EventVenueDto createEventVenue(CreateEventVenueDto createEventVenueDto) {
        // A just-created venue has no events yet.
        return eventVenueRepository.save(createEventVenueDto.toEntity()).toResponseDto(0);
    }

    @Transactional(readOnly = true)
    public List<EventVenueDto> getAllEventVenues() {
        Map<UUID, Integer> eventCounts = EventCountProjection.toMap(eventVenueRepository.countEventsGroupedByEventVenue());
        return eventVenueRepository.findAll().stream()
                .map(venue -> venue.toResponseDto(EventCountProjection.countOf(eventCounts, venue.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventVenueDto getEventVenueById(UUID id) {
        return eventVenueRepository.findById(id)
                .map(venue -> venue.toResponseDto(countEvents(venue)))
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
        return updatedEventVenue.toResponseDto(countEvents(updatedEventVenue));
    }

    @Transactional
    public void deleteEventVenue(UUID id) {
        EventVenue eventVenue = eventVenueRepository.findById(id)
                .orElseThrow(() -> new EventVenueNotFoundException(id));
        eventVenueRepository.delete(eventVenue);
    }

    private int countEvents(EventVenue eventVenue) {
        return Math.toIntExact(eventVenueRepository.countEventsByEventVenueId(eventVenue.getId()));
    }
}
