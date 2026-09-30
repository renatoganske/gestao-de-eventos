package com.renatoganske.gestao_de_eventos.controllers.impl;

import com.renatoganske.gestao_de_eventos.controllers.IEventController;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventDto;
import com.renatoganske.gestao_de_eventos.dtos.EventDto;
import com.renatoganske.gestao_de_eventos.enums.DeliveryStatus;
import com.renatoganske.gestao_de_eventos.filters.EventSearchCriteria;
import com.renatoganske.gestao_de_eventos.services.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EventController implements IEventController {

    private final EventService eventService;

    @Override
    public ResponseEntity<List<EventDto>> findAll() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @Override
    public ResponseEntity<EventDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @Override
    public ResponseEntity<List<EventDto>> search(UUID eventTypeId, UUID venueId, UUID professionalId,
                                                  LocalDate from, LocalDate to, UUID hdId, DeliveryStatus deliveryStatus,
                                                  String customerName, String eventCode,
                                                  Boolean daytimeWedding, Boolean outdoorWedding) {
        EventSearchCriteria criteria = new EventSearchCriteria(
                eventTypeId, venueId, professionalId, from, to, hdId, deliveryStatus,
                customerName, eventCode, daytimeWedding, outdoorWedding);
        return ResponseEntity.ok(eventService.searchEvents(criteria));
    }

    @Override
    public ResponseEntity<EventDto> create(@RequestBody @Valid CreateEventDto requestDto) {
        EventDto responseDto = eventService.createEvent(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Override
    public ResponseEntity<EventDto> update(@PathVariable UUID id, @RequestBody @Valid CreateEventDto requestDto) {
        return ResponseEntity.ok(eventService.updateEvent(id, requestDto));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        eventService.deleteEvent(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
