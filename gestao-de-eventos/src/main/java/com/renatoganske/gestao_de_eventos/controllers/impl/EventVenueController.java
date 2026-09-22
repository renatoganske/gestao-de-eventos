package com.renatoganske.gestao_de_eventos.controllers.impl;

import com.renatoganske.gestao_de_eventos.controllers.IEventVenueController;
import com.renatoganske.gestao_de_eventos.dtos.CreateEventVenueDto;
import com.renatoganske.gestao_de_eventos.dtos.EventVenueDto;
import com.renatoganske.gestao_de_eventos.services.EventVenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EventVenueController implements IEventVenueController {

    private final EventVenueService eventVenueService;

    @Override
    public ResponseEntity<List<EventVenueDto>> findAll() {
        return ResponseEntity.ok(eventVenueService.getAllEventVenues());
    }

    @Override
    public ResponseEntity<EventVenueDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventVenueService.getEventVenueById(id));
    }

    @Override
    public ResponseEntity<EventVenueDto> create(@RequestBody @Valid CreateEventVenueDto requestDto) {
        EventVenueDto responseDto = eventVenueService.createEventVenue(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Override
    public ResponseEntity<EventVenueDto> update(@PathVariable UUID id, @RequestBody @Valid CreateEventVenueDto requestDto) {
        return ResponseEntity.ok(eventVenueService.updateEventVenue(id, requestDto));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        eventVenueService.deleteEventVenue(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
