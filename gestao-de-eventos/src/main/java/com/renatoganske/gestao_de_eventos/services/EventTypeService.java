package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateEventTypeDto;
import com.renatoganske.gestao_de_eventos.dtos.EventTypeDto;
import com.renatoganske.gestao_de_eventos.entities.EventType;
import com.renatoganske.gestao_de_eventos.exceptions.EventTypeNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.EventTypeRepository;
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
public class EventTypeService {

    private final EventTypeRepository eventTypeRepository;

    @Transactional
    public EventTypeDto createEventType(CreateEventTypeDto createEventTypeDto) {
        return eventTypeRepository.save(createEventTypeDto.toEntity()).toResponseDto();
    }

    public List<EventTypeDto> getAllEventTypes() {
        return eventTypeRepository.findAll().stream()
                .map(EventType::toResponseDto)
                .collect(Collectors.toList());
    }

    public EventTypeDto getEventTypeById(UUID id) {
        return eventTypeRepository.findById(id)
                .map(EventType::toResponseDto)
                .orElseThrow(() -> new EventTypeNotFoundException(id));
    }

    @Transactional
    public EventTypeDto updateEventType(UUID id, CreateEventTypeDto createEventTypeDto) {
        EventType eventType = eventTypeRepository.findById(id)
                .orElseThrow(() -> new EventTypeNotFoundException(id));

        eventType.setName(createEventTypeDto.name());

        EventType updatedEventType = eventTypeRepository.save(eventType);
        return updatedEventType.toResponseDto();
    }

    @Transactional
    public void deleteEventType(UUID id) {
        EventType eventType = eventTypeRepository.findById(id)
                .orElseThrow(() -> new EventTypeNotFoundException(id));
        eventTypeRepository.delete(eventType);
    }
}
